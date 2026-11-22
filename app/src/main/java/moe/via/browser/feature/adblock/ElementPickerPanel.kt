package moe.via.browser.feature.adblock

import moe.via.browser.ui.theme.fonts.NativeTypography

import moe.via.browser.ui.message.BrowserMessages

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.ui.theme.window.ChromeLayoutPolicy
import moe.via.browser.ui.theme.window.ChromeMode
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** Transparent overlay lets page touches through outside the native control panel. */
class ElementPickerPanel(private val activity: Activity, private val parent: () -> FrameLayout, private val actions: Actions) {
    interface Actions { fun parent(); fun child(); fun save(); fun cancel() }
    private val legacy = ContextThemeWrapper(activity, if (activity.palette.night) android.R.style.Theme_Black else android.R.style.Theme_Light)
    private var layer: FrameLayout? = null
    private var body: View? = null
    private var selector: TextView? = null
    private var top = false
    fun show() {
        close()
        top = ChromeLayoutPolicy.resolve(activity).topAnchored
        val content = LayoutInflater.from(legacy).inflate(R.layout.element_picker_panel, null).also { NativeTypography.tree(it) }
        body = content
        selector = content.findViewById<TextView>(R.id.element_picker_selector).apply {
            setTextColor(activity.palette.secondary); setHintTextColor(activity.palette.secondary)
            setOnLongClickListener {
                if (text.isNotEmpty()) runCatching { activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("内容", text)) }
                    .onSuccess { BrowserMessages.show(activity, "已复制内容") }
                true
            }
        }
        listOf(R.id.element_picker_parent, R.id.element_picker_child, R.id.element_picker_save, R.id.element_picker_cancel).forEach { id -> content.findViewById<TextView>(id).setTextColor(activity.palette.text) }
        content.findViewById<View>(R.id.element_picker_parent).setOnClickListener { actions.parent() }
        content.findViewById<View>(R.id.element_picker_child).setOnClickListener { actions.child() }
        content.findViewById<View>(R.id.element_picker_save).setOnClickListener { actions.save() }
        content.findViewById<View>(R.id.element_picker_cancel).setOnClickListener { actions.cancel() }
        content.findViewById<ImageView>(R.id.element_picker_move).apply { imageTintList = android.content.res.ColorStateList.valueOf(activity.palette.icon); setOnClickListener { move() } }
        layer = FrameLayout(activity).also { parent().addView(it, FrameLayout.LayoutParams(-1, -1)) }
        content.accessibilityPaneTitle = "标记广告"
        bind()
        content.alpha = 0f; content.translationY = if (top) -120f else 120f
        content.animate().alpha(1f).translationY(0f).setDuration(160).setInterpolator(AccelerateInterpolator()).start()
    }
    fun update(value: String) { selector?.text = value }
    private fun bind(updateRotation: Boolean = true) {
        val content = body ?: return
        val container = layer ?: return
        val metrics = activity.resources.displayMetrics
        val short = minOf(parent().width.takeIf { it > 0 } ?: metrics.widthPixels, parent().height.takeIf { it > 0 } ?: metrics.heightPixels)
        val spec = ChromeLayoutPolicy.resolve(activity)
        val width = if (spec.tablet && spec.mode != ChromeMode.TRADITIONAL) minOf(short - activity.dp(96), activity.dp(384)) else short
        (content.parent as? android.view.ViewGroup)?.removeView(content)
        content.background = GradientDrawable().apply {
            setColor(if (activity.palette.night) 0xCC000000.toInt() else 0xEBFFFFFF.toInt())
            val radius = activity.dp(18).toFloat()
            cornerRadii = if (top) floatArrayOf(0f, 0f, 0f, 0f, radius, radius, radius, radius) else floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
        }
        content.clipToOutline = true
        container.addView(content, FrameLayout.LayoutParams(width.coerceAtLeast(1), -2, Gravity.END or if (top) Gravity.TOP else Gravity.BOTTOM))
        if (updateRotation) content.findViewById<View>(R.id.element_picker_move).rotation = if (top) 180f else 0f
    }
    private fun move() {
        val content = body ?: return
        top = !top
        content.findViewById<View>(R.id.element_picker_move).animate().rotation(if (top) 180f else 0f).setDuration(240).start()
        content.animate().alpha(0f).setDuration(120).withEndAction { if (body === content) { bind(false); content.animate().alpha(1f).setDuration(120).start() } }.start()
    }
    fun rebind() {
        val overlay = layer ?: return
        (overlay.parent as? android.view.ViewGroup)?.removeView(overlay); parent().addView(overlay, FrameLayout.LayoutParams(-1, -1)); bind()
    }
    fun close() { body?.animate()?.cancel(); layer?.let { (it.parent as? android.view.ViewGroup)?.removeView(it) }; layer = null; body = null; selector = null }
}
