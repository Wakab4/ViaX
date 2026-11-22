package moe.via.browser.browser.ui.tabs.switcher

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.ui.components.recycler.NativeRecyclerEffects
import moe.via.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.via.browser.ui.theme.window.ChromeLayoutPolicy
import moe.via.browser.ui.components.SkinIconBinder
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** f8.c/l0: transparent overlay, rounded list, ItemTouchHelper and independently animated list height. */
internal class TabSwitcherPopup(private val activity: Activity, private val parent: FrameLayout, private val actions: Actions) {
    interface Actions {
        fun dismiss()
        fun select(id: String)
        fun close(id: String)
        fun context(anchor: View, id: String)
        fun move(from: String, to: String): Boolean
        fun create()
        fun closedTabs()
    }
    private var stopped = false
    private var dragging = false
    private var heightAnimation: ValueAnimator? = null
    private var removed = false
    private var finished: (() -> Unit)? = null
    private val spec = ChromeLayoutPolicy.resolve(activity)
    private val bottom = !spec.topAnchored
    private val skin = SkinIconBinder(activity)
    val view = FrameLayout(activity).apply {
        isClickable = true
        setOnClickListener { if (!stopped) actions.dismiss() }
    }
    private val adapter: TabSwitcherAdapter = TabSwitcherAdapter(activity, object : TabSwitcherAdapter.Actions {
        override fun select(id: String) { if (!stopped) actions.select(id) }
        override fun close(id: String) { if (!stopped) actions.close(id) }
        override fun context(anchor: View, id: String) { if (!stopped && !dragging) actions.context(anchor, id) }
        override fun drag(holder: RecyclerView.ViewHolder) {
            if (!stopped && adapter.itemCount > 1) { dragging = true; touch.startDrag(holder) }
        }
    })
    private val rows = RecyclerView(activity).apply {
        NativeRecyclerEffects.install(this)
        layoutManager = LinearLayoutManager(activity).apply { stackFromEnd = this@TabSwitcherPopup.bottom }
        setHasFixedSize(true)
        itemAnimator = DefaultItemAnimator().apply { addDuration = 100; removeDuration = 100; changeDuration = 100 }
        this.adapter = this@TabSwitcherPopup.adapter
    }
    private val touch: ItemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(3, 48) {
        override fun isLongPressDragEnabled() = false
        override fun onMove(list: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
            if (stopped) return false
            val source = adapter.items.getOrNull(from.bindingAdapterPosition) ?: return false
            val target = adapter.items.getOrNull(to.bindingAdapterPosition) ?: return false
            return actions.move(source.id, target.id)
        }
        override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) {
            if (!stopped) adapter.items.getOrNull(holder.bindingAdapterPosition)?.let { actions.close(it.id) }
        }
        override fun onSelectedChanged(holder: RecyclerView.ViewHolder?, state: Int) {
            super.onSelectedChanged(holder, state)
            if (state == ItemTouchHelper.ACTION_STATE_DRAG && holder != null) holder.itemView.setBackgroundColor(0x30808080)
            else if (state == ItemTouchHelper.ACTION_STATE_IDLE) dragging = false
        }
        override fun clearView(list: RecyclerView, holder: RecyclerView.ViewHolder) {
            super.clearView(list, holder)
            holder.itemView.setBackgroundResource(R.drawable.ref_tab_switcher_row_feedback)
        }
    })
    private val add = ImageView(activity).apply {
        setPadding(dp(13), dp(13), dp(13), dp(13))
        imageTintList = android.content.res.ColorStateList.valueOf(activity.palette.icon)
        contentDescription = activity.getString(R.string.ref_tab_switcher_new_tab)
        setBackgroundResource(R.drawable.ref_tab_switcher_row_feedback)
        setOnClickListener { if (!stopped) actions.create() }
        setOnLongClickListener { if (!stopped) actions.closedTabs(); true }
    }
    private val body = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(8)); isClickable = true
        addView(rows, LinearLayout.LayoutParams(-1, 0))
        addView(add, LinearLayout.LayoutParams(-1, dp(48)))
    }
    private var home = false
    private val geometry = View.OnLayoutChangeListener { _, l, t, r, b, oldL, oldT, oldR, oldB ->
        if (!stopped && (r - l != oldR - oldL || b - t != oldB - oldT)) updateGeometry(false)
    }
    private var focus: RecyclerView.OnChildAttachStateChangeListener? = null

    init {
        touch.attachToRecyclerView(rows)
        skin.image(add, R.drawable.ref_ic_add)
        ViewCompat.setAccessibilityPaneTitle(body, activity.getString(R.string.ref_tab_switcher_title))
        view.addView(body, FrameLayout.LayoutParams(width(), -2, spec.popupGravity))
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = Unit
            override fun onViewDetachedFromWindow(view: View) { if (stopped) finishClose() }
        })
    }

    fun show(items: List<TabSwitcherItem>) {
        render(items, false)
        parent.addView(view, FrameLayout.LayoutParams(-1, -1))
        parent.addOnLayoutChangeListener(geometry)
        NativeTypography.tree(view)
        view.alpha = 0f; view.translationY = if (bottom) 120f else -120f
        view.animate().alpha(1f).translationY(0f).setDuration(160).setInterpolator(AccelerateInterpolator()).start()
        val selected = items.indexOfFirst { it.selected }
        if (selected >= 0) {
            rows.scrollToPosition(selected)
            focusSelected(items[selected].id)
        }
    }

    fun render(items: List<TabSwitcherItem>, animateHeight: Boolean = true) {
        if (stopped) return
        val structural = adapter.submit(items)
        val nextHome = items.firstOrNull { it.selected }?.home == true
        if (structural || home != nextHome || !animateHeight) {
            home = nextHome; updateBackground()
        }
        if (structural || !animateHeight) updateGeometry(animateHeight)
    }

    private fun width(): Int {
        val metrics = activity.resources.displayMetrics
        val edge = if (parent.width > 0 && parent.height > 0) minOf(parent.width, parent.height) else minOf(metrics.widthPixels, metrics.heightPixels)
        return if (spec.tablet && spec.mode != moe.via.browser.ui.theme.window.ChromeMode.TRADITIONAL)
            minOf(edge - activity.dp(96), activity.dp(384)).coerceAtLeast(1) else edge.coerceAtLeast(1)
    }
    private fun listHeight(): Int {
        val maximum = (parent.height.takeIf { it > 0 } ?: activity.resources.displayMetrics.heightPixels) - activity.dp(16)
        return minOf((maximum - activity.dp(48) - activity.dp(16)).coerceAtLeast(0), activity.dp(48) * adapter.itemCount)
    }
    private fun updateGeometry(animate: Boolean) {
        (body.layoutParams as FrameLayout.LayoutParams).let { it.width = width(); body.layoutParams = it }
        val target = listHeight()
        val params = rows.layoutParams
        val previous = params.height.coerceAtLeast(0)
        heightAnimation?.cancel(); heightAnimation = null
        if (!animate || !view.isAttachedToWindow || previous == target) {
            params.height = target; rows.layoutParams = params
        } else {
            heightAnimation = ValueAnimator.ofInt(previous, previous, target).apply {
                duration = 350; interpolator = PathInterpolator(.2f, .2f, .8f, .8f)
                addUpdateListener { if (!stopped) { params.height = it.animatedValue as Int; rows.layoutParams = params } }
                start()
            }
        }
    }
    private fun updateBackground() {
        val radius = activity.dp(18).toFloat()
        val top = if (home || bottom) radius else 0f
        val lower = if (home || !bottom) radius else 0f
        body.background = GradientDrawable().apply {
            setColor(if (activity.palette.night) 0xCC000000.toInt() else 0xEBFFFFFF.toInt())
            cornerRadii = floatArrayOf(top, top, top, top, lower, lower, lower, lower)
        }
    }
    private fun focusSelected(id: String) {
        fun focus(view: View) { view.requestFocus(); view.performAccessibilityAction(64, null) }
        val listener = object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(child: View) {
                val holder = rows.getChildViewHolder(child)
                if (adapter.items.getOrNull(holder.bindingAdapterPosition)?.id == id) {
                    rows.removeOnChildAttachStateChangeListener(this); this@TabSwitcherPopup.focus = null
                    child.post { if (!stopped && child.isAttachedToWindow) focus(child) }
                }
            }
            override fun onChildViewDetachedFromWindow(child: View) = Unit
        }
        this.focus = listener; rows.addOnChildAttachStateChangeListener(listener)
        rows.post {
            if (!stopped) {
                val index = adapter.items.indexOfFirst { it.id == id }
                rows.findViewHolderForAdapterPosition(index)?.itemView?.let {
                    rows.removeOnChildAttachStateChangeListener(listener); this.focus = null; focus(it)
                }
            }
        }
    }

    fun close(animate: Boolean, finished: () -> Unit) {
        if (stopped) { if (!animate) finishClose(); return }
        this.finished = finished
        stopped = true
        parent.removeOnLayoutChangeListener(geometry)
        focus?.let(rows::removeOnChildAttachStateChangeListener); focus = null
        heightAnimation?.cancel(); heightAnimation = null
        view.animate().cancel(); rows.stopScroll(); rows.itemAnimator?.endAnimations()
        touch.attachToRecyclerView(null)
        (rows.edgeEffectFactory as? NativeRecyclerEdgeEffect)?.close()
        adapter.stop(); skin.close(); add.setOnClickListener(null); add.setOnLongClickListener(null)
        view.isClickable = false; view.setOnClickListener(null)
        if (animate && view.isAttachedToWindow && !activity.isDestroyed && !activity.isFinishing)
            view.animate().alpha(0f).translationY(if (bottom) 120f else -120f).setDuration(160)
                .setInterpolator(AccelerateInterpolator()).withEndAction(::finishClose).start()
        else finishClose()
    }

    fun dispose() { if (stopped) finishClose() else close(false) {} }

    private fun finishClose() {
        if (removed) return
        removed = true
        view.animate().withEndAction(null).cancel()
        parent.removeView(view); rows.adapter = null; adapter.close()
        val callback = finished; finished = null; callback?.invoke()
    }
}
