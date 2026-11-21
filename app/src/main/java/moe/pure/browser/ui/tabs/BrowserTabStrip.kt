package moe.pure.browser.ui.tabs

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.model.BrowserTab
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.palette
import kotlin.math.abs
import kotlin.math.roundToInt

/** Native's optional 36 dp horizontal tab row, independent of tab/session ownership. */
class BrowserTabStrip(private val activity: Activity, private val actions: Actions) {
    interface Actions {
        fun select(index: Int)
        fun close(index: Int)
        fun newTab()
        fun move(from: Int, to: Int)
        fun closeIndices(indices: List<Int>)
        fun duplicate(index: Int)
        fun copyUrl(url: String)
    }

    private data class Row(val root: LinearLayout, val icon: ImageView, val title: TextView, val close: ImageView)
    private data class Drag(val id: String, val downX: Float, val downY: Float, val grabOffset: Float,
        var x: Float, var active: Boolean = false)

    private var disposed = false
    private var tabs: List<TabStripPresentation> = emptyList()
    private var selectedId: String? = null
    private var iconColor = activity.palette.icon
    private var textColor = activity.palette.text
    private var tabWidth = activity.dp(180)
    private var slots = 7
    private var drag: Drag? = null
    private var menu: BrowserDialog? = null
    private val rows = linkedMapOf<String, Row>()
    private var visibleIds: Set<String> = emptySet()
    private val icons = TabStripIcons(activity)
    private val touchSlop = ViewConfiguration.get(activity).scaledTouchSlop
    private val root: LinearLayout = object : LinearLayout(activity) {
        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            parentWidth(w)
        }
        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            super.onLayout(changed, l, t, r, b)
            positionDraggedRow()
            refreshVisibleIcons()
        }
        override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
            val candidate = drag
            if (candidate != null) {
                if (event.actionMasked == MotionEvent.ACTION_MOVE && !candidate.active) {
                    val dx = event.x - candidate.downX
                    val dy = event.y - candidate.downY
                    if (abs(dx) > touchSlop && abs(dx) >= abs(dy)) beginDrag(candidate)
                    else if (abs(dy) > touchSlop) endDrag()
                }
                if (candidate.active) return true
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) endDrag()
            }
            return super.onInterceptTouchEvent(event)
        }
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val current = drag ?: return super.onTouchEvent(event)
            if (!current.active) return super.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    current.x = event.x
                    reorderDraggedRow()
                    positionDraggedRow()
                    removeCallbacks(autoScroll)
                    postOnAnimation(autoScroll)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> endDrag()
            }
            return true
        }
    }.apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = ViewGroup.LayoutParams(-1, activity.dp(36))
        weightSum = slots.toFloat()
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val strip = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = ViewGroup.LayoutParams(-2, -1)
        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        dividerDrawable = GradientDrawable().apply { setColor(DIVIDER); setSize(activity.dp(1), activity.dp(1)) }
    }
    private val scroll = HorizontalScrollView(activity).apply {
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        isFillViewport = false
        addView(strip, ViewGroup.LayoutParams(-2, -1))
        setOnScrollChangeListener { _, _, _, _, _ -> refreshVisibleIcons() }
        accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.collectionInfo = AccessibilityNodeInfo.CollectionInfo.obtain(1, tabs.size, false, AccessibilityNodeInfo.CollectionInfo.SELECTION_MODE_SINGLE)
            }
        }
    }
    private val newButton = ImageView(activity).apply {
        icons.control(this, R.drawable.ref_ic_add, iconColor)
        setPadding(0, activity.dp(7), 0, activity.dp(7))
        scaleType = ImageView.ScaleType.FIT_CENTER
        contentDescription = "新建标签"
        background = feedback(round = true)
        setOnClickListener { if (!disposed) actions.newTab() }
    }
    private val autoScroll: Runnable = object : Runnable {
        override fun run() {
            val current = drag?.takeIf { it.active } ?: return
            val edge = activity.dp(32)
            val localX = current.x - scroll.left
            val delta = when {
                localX < edge -> -((edge - localX) / edge * activity.dp(12)).roundToInt().coerceAtLeast(1)
                localX > scroll.width - edge -> ((localX - (scroll.width - edge)) / edge * activity.dp(12)).roundToInt().coerceAtLeast(1)
                else -> 0
            }
            if (delta != 0) {
                scroll.scrollBy(delta, 0)
                reorderDraggedRow()
                positionDraggedRow()
                root.postOnAnimation(this)
            }
        }
    }

    val view: View get() = root

    init {
        root.addView(scroll, LinearLayout.LayoutParams(0, -1, (slots - 1).toFloat()))
        root.addView(newButton, LinearLayout.LayoutParams(0, -1, 1f))
    }

    /** Captures display metadata without instantiating dormant WebViews. */
    fun render(tabs: List<BrowserTab>, selectedIndex: Int) {
        if (disposed) return
        val oldSelection = selectedId
        this.tabs = tabs.map(TabStripPresentation::capture)
        selectedId = this.tabs.getOrNull(selectedIndex)?.id
        drag?.id?.let { id -> if (this.tabs.none { it.id == id }) endDrag() }
        val ids = this.tabs.map(TabStripPresentation::id).toSet()
        rows.keys.filter { it !in ids }.toList().forEach { id -> rows.remove(id)?.let { strip.removeView(it.root) } }
        this.tabs.forEachIndexed { index, tab ->
            val row = rows.getOrPut(tab.id) { createRow(tab.id) }
            if (strip.getChildAt(index) !== row.root) {
                (row.root.parent as? ViewGroup)?.removeView(row.root)
                strip.addView(row.root, index, LinearLayout.LayoutParams(tabWidth, -1))
            }
            bind(row, tab)
        }
        icons.control(newButton, R.drawable.ref_ic_add, iconColor)
        if (oldSelection != selectedId && drag?.active != true) revealSelected()
    }

    fun recolor(icon: Int, text: Int) {
        if (disposed || iconColor == icon && textColor == text) return
        iconColor = icon; textColor = text
        refreshVisuals()
    }

    private fun createRow(id: String): Row {
        val icon = ImageView(activity).apply {
            setPadding(activity.dp(13), 0, activity.dp(13), 0)
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> if (tabs.size >= 2) {
                        val row = rows[id]?.root ?: return@setOnTouchListener false
                        val rootLocation = IntArray(2); root.getLocationOnScreen(rootLocation)
                        val rowLocation = IntArray(2); row.getLocationOnScreen(rowLocation)
                        val rootX = event.rawX - rootLocation[0]
                        drag = Drag(id, rootX, event.rawY - rootLocation[1], event.rawX - rowLocation[0], rootX)
                        root.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP -> if (drag?.active != true) endDrag()
                    MotionEvent.ACTION_CANCEL -> if (drag?.active != true) endDrag()
                }
                false
            }
        }
        val title = NativeTypography.text(activity).apply {
            textSize = 14f
            setSingleLine()
            gravity = Gravity.CENTER_VERTICAL
            ellipsize = TextUtils.TruncateAt.END
            isHorizontalFadingEdgeEnabled = false
            textDirection = View.TEXT_DIRECTION_LOCALE
        }
        val close = ImageView(activity).apply {
            icons.control(this, R.drawable.ref_ic_close, iconColor)
            setPadding(activity.dp(14), 0, activity.dp(14), 0)
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = feedback(round = true)
            contentDescription = "关闭标签"
            setOnClickListener { index(id).takeIf { it >= 0 }?.let(actions::close) }
        }
        val item = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            background = feedback()
            addView(icon, LinearLayout.LayoutParams(activity.dp(48), -1))
            addView(title, LinearLayout.LayoutParams(0, -1, 1f))
            addView(close, LinearLayout.LayoutParams(activity.dp(48), -1))
            setOnClickListener { index(id).takeIf { it >= 0 }?.let(actions::select) }
            setOnLongClickListener {
                if (drag != null) true else { contextMenu(id, this); true }
            }
            accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    val position = index(id)
                    info.collectionItemInfo = AccessibilityNodeInfo.CollectionItemInfo.obtain(0, 1, position.coerceAtLeast(0), 1, false, selectedId == id)
                    if (position > 0) info.addAction(AccessibilityNodeInfo.AccessibilityAction(R.id.tab_strip_move_before, "将标签向前移动"))
                    if (position >= 0 && position < tabs.lastIndex) info.addAction(AccessibilityNodeInfo.AccessibilityAction(R.id.tab_strip_move_after, "将标签向后移动"))
                }
                override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
                    val from = index(id)
                    val to = when (action) { R.id.tab_strip_move_before -> from - 1; R.id.tab_strip_move_after -> from + 1; else -> return super.performAccessibilityAction(host, action, args) }
                    if (from !in tabs.indices || to !in tabs.indices) return false
                    actions.move(from, to)
                    return true
                }
            }
        }
        return Row(item, icon, title, close)
    }

    private fun bind(row: Row, tab: TabStripPresentation) {
        val selected = tab.id == selectedId
        val alpha = if (selected) 1f else .45f
        row.root.isSelected = selected
        row.root.contentDescription = tab.title
        row.title.text = tab.title
        row.title.setTextColor(textColor)
        row.title.alpha = alpha; row.icon.alpha = alpha; row.close.alpha = alpha
        bindIcons(row, tab, visible(row))
        if (row.root.layoutParams?.width != tabWidth) row.root.layoutParams = LinearLayout.LayoutParams(tabWidth, -1)
    }

    private fun refreshVisuals() {
        if (disposed) return
        icons.control(newButton, R.drawable.ref_ic_add, iconColor)
        tabs.forEach { tab -> rows[tab.id]?.let { bind(it, tab) } }
    }

    private fun visible(row: Row) = row.root.width > 0 && row.root.right > scroll.scrollX &&
        row.root.left < scroll.scrollX + scroll.width

    private fun bindIcons(row: Row, tab: TabStripPresentation, readFile: Boolean) {
        icons.control(row.close, R.drawable.ref_ic_close, iconColor)
        icons.site(row.icon, tab.url, tab.icon, iconColor, readFile)
    }

    private fun refreshVisibleIcons() {
        if (disposed) return
        icons.control(newButton, R.drawable.ref_ic_add, iconColor)
        val visibleNow = linkedSetOf<String>()
        tabs.forEach { tab -> rows[tab.id]?.let { row ->
            if (visible(row)) { visibleNow += tab.id; bindIcons(row, tab, readFile = true) }
            else if (tab.id in visibleIds) bindIcons(row, tab, readFile = false)
        } }
        visibleIds = visibleNow
    }

    private fun parentWidth(width: Int) {
        if (disposed || width <= 0) return
        val widthDp = (width / activity.resources.displayMetrics.density + .5f).toInt()
        val unit = when { widthDp < 420 -> (widthDp / 7).coerceAtLeast(1); widthDp > 900 -> 72; else -> 60 }
        slots = (widthDp / unit).coerceAtLeast(2)
        root.weightSum = slots.toFloat()
        scroll.layoutParams = LinearLayout.LayoutParams(0, -1, (slots - 1).toFloat())
        tabWidth = minOf(activity.dp(180), maxOf(((width / slots) * (slots - 1) - activity.dp(1) * 2) / 2, activity.dp(100)))
        rows.values.forEach { it.root.layoutParams = LinearLayout.LayoutParams(tabWidth, -1) }
        revealSelected()
    }

    private fun revealSelected() {
        root.post {
            if (disposed || drag?.active == true) return@post
            val row = selectedId?.let { rows[it]?.root } ?: return@post
            val left = row.left; val right = row.right
            when {
                left < scroll.scrollX -> scroll.smoothScrollTo(left, 0)
                right > scroll.scrollX + scroll.width -> scroll.smoothScrollTo(right - scroll.width, 0)
            }
        }
    }

    private fun contextMenu(id: String, anchor: View) {
        val position = index(id)
        val tab = tabs.getOrNull(position) ?: return
        val commands = mutableListOf<Pair<String, () -> Unit>>()
        if (tabs.size > 1) {
            commands += "关闭所有标签页" to { actions.closeIndices(tabs.indices.toList()) }
            commands += "关闭其它标签页" to { actions.closeIndices(tabs.indices.filter { it != index(id) }) }
        }
        if (position > 0 && position < tabs.lastIndex) {
            val rtl = root.layoutDirection == View.LAYOUT_DIRECTION_RTL
            commands += "关闭左侧标签页" to { closeSide(id, before = !rtl) }
            commands += "关闭右侧标签页" to { closeSide(id, before = rtl) }
        }
        if (!tab.home && (tab.url.startsWith("http://", true) || tab.url.startsWith("https://", true)))
            commands += "复制标签页" to { index(id).takeIf { it >= 0 }?.let(actions::duplicate) }
        if (!tab.home && !tab.url.startsWith("file://", true)) commands += "复制链接" to { actions.copyUrl(tab.url) }
        if (commands.isEmpty()) return
        menu?.dismiss()
        menu = BrowserDialog(activity).items(commands.map { it.first }) { commands[it].second() }.anchoredTo(anchor).apply {
            setOnDismissListener { menu = null }
            show()
        }
    }

    private fun closeSide(id: String, before: Boolean) {
        val position = index(id)
        if (position < 0) return
        actions.closeIndices(tabs.indices.filter { if (before) it < position else it > position })
    }

    private fun index(id: String) = tabs.indexOfFirst { it.id == id }

    private fun beginDrag(value: Drag) {
        value.active = true
        menu?.dismiss()
        rows[value.id]?.root?.apply { setBackgroundColor(DIVIDER); translationZ = 1f; isPressed = false }
    }

    private fun reorderDraggedRow() {
        val value = drag?.takeIf { it.active } ?: return
        val from = index(value.id)
        if (from < 0 || strip.childCount == 0) return
        val center = value.x - scroll.left + scroll.scrollX - value.grabOffset + tabWidth / 2f
        val to = tabs.indices.minByOrNull { position ->
            val child = strip.getChildAt(position)
            if (child == null) Float.MAX_VALUE else abs(center - (child.left + child.width / 2f))
        } ?: return
        if (from != to) actions.move(from, to)
    }

    private fun positionDraggedRow() {
        val value = drag?.takeIf { it.active } ?: return
        val row = rows[value.id]?.root ?: return
        row.translationX = value.x - scroll.left + scroll.scrollX - value.grabOffset - row.left
    }

    private fun endDrag() {
        val old = drag ?: return
        drag = null
        root.removeCallbacks(autoScroll)
        rows[old.id]?.root?.apply {
            translationX = 0f; translationZ = 0f; background = feedback(); isPressed = false
        }
        root.parent?.requestDisallowInterceptTouchEvent(false)
    }

    private fun feedback(round: Boolean = false) = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed), GradientDrawable().apply {
            setColor(DIVIDER)
            if (round) cornerRadius = activity.dp(18).toFloat()
        })
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        endDrag()
        menu?.dismiss(); menu = null
        rows.clear(); tabs = emptyList(); visibleIds = emptySet(); strip.removeAllViews()
        newButton.setOnClickListener(null)
    }

    companion object { private const val DIVIDER = 0x30808080 }
}
