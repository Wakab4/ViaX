package moe.via.browser.ui.components.toolbar

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.theme.skin.NativeSkinIcons
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** The page toolbar owns z's title, spacer, navigation and action geometry. */
class NativePageToolbar(context: Context, title: String, back: () -> Unit) : LinearLayout(context) {
    private val icons = NativeSkinIcons(context)
    private val actions = ArrayList<NativeToolbarAction>()
    private val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x30808080 }
    val titleView = NativeTypography.text(context).apply {
        text = title
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.page_info_title_size).toFloat())
        setTextColor(context.palette.text)
        setSingleLine(); maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        gravity = Gravity.CENTER_VERTICAL; textDirection = View.TEXT_DIRECTION_LOCALE
        NativeTypography.style(this, Typeface.BOLD)
    }

    init {
        orientation = HORIZONTAL
        setWillNotDraw(false)
        addView(icon(R.drawable.ref_ic_back, context.getString(R.string.ref_page_navigation_up), back))
        addView(titleView, LayoutParams(0, -2, 24f).apply { gravity = Gravity.CENTER_VERTICAL })
        addView(Space(context), LayoutParams(0, dp(54), 1f))
    }

    private fun actionLayout(width: Int) = LayoutParams(width, -1).apply {
        val margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics).toInt()
        setMargins(margin, margin, margin, margin)
    }

    private fun icon(resource: Int, label: String, clicked: () -> Unit, skinKey: String? = null) = ImageView(context).apply {
        setImageDrawable(icons.resolve(resource, skinKey) ?: context.getDrawable(resource))
        imageTintList = ColorStateList.valueOf(context.palette.icon)
        contentDescription = label
        setPadding(dp(13), 0, dp(13), 0)
        setBackgroundResource(R.drawable.dialog_button_feedback)
        layoutParams = actionLayout(dp(48))
        setOnClickListener { clicked() }
    }

    fun addIconAction(resource: Int, label: String, clicked: () -> Unit): ImageView =
        addAction(NativeToolbarAction(View.generateViewId(), 0, resource, label), clicked) as ImageView

    fun addTextAction(label: String, clicked: () -> Unit): TextView =
        addAction(NativeToolbarAction(View.generateViewId(), 0, null, label), clicked) as TextView

    private fun textAction(label: String, clicked: () -> Unit) = NativeTypography.text(context).apply {
        text = label
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.page_info_action_size).toFloat())
        setTextColor(context.palette.text)
        setSingleLine(); maxLines = 1; setLines(1); ellipsize = TextUtils.TruncateAt.END
        gravity = Gravity.CENTER; setAllCaps(true)
        setPadding(dp(13), 0, dp(13), 0)
        setBackgroundResource(R.drawable.dialog_button_feedback)
        layoutParams = actionLayout(-2)
        setOnClickListener { clicked() }
    }

    fun addAction(item: NativeToolbarAction, clicked: () -> Unit): View {
        require(findViewById<View>(item.id) == null) { "The menu item id must be unique" }
        actions.add(item)
        return (if (item.icon == null) textAction(item.label, clicked)
            else icon(item.icon, item.label, clicked, item.skinKey)).also {
            it.id = item.id
            it.visibility = if (item.visible) View.VISIBLE else View.GONE
            addView(it)
        }
    }

    fun setGroupVisible(group: Int, visible: Boolean) {
        actions.filter { it.group == group }.forEach { setItemVisible(it.id, visible) }
    }

    fun setContentColor(color: Int) {
        for (index in 0 until childCount) {
            when (val child = getChildAt(index)) {
                is TextView -> child.setTextColor(color)
                is ImageView -> child.imageTintList = ColorStateList.valueOf(color)
            }
        }
    }

    fun setItemVisible(id: Int, visible: Boolean) {
        val view = findViewById<View>(id) ?: return
        if ((view.visibility == View.VISIBLE) == visible) return
        view.visibility = if (visible) View.VISIBLE else View.GONE
        actions.firstOrNull { it.id == id }?.visible = visible
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, measuredHeight - 2f, measuredWidth.toFloat(), measuredHeight.toFloat(), divider)
    }
}
