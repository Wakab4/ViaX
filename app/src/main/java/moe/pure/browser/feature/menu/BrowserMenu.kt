package moe.pure.browser.feature.menu

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.Dialog
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import moe.pure.browser.R
import moe.pure.browser.feature.settings.context.SettingsWindowEffects
import moe.pure.browser.ui.chrome.ChromeLayoutPolicy
import moe.pure.browser.ui.chrome.ChromeMode
import moe.pure.browser.ui.components.SkinIconBinder
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.touchFeedback
import moe.pure.browser.feature.menu.ui.MenuWindowPolicy
import moe.pure.browser.ui.theme.palette
import java.lang.ref.WeakReference

/** i8.k window rendering; state and command routing are owned by MenuState/Controller. */
class BrowserMenu(private val activity: Activity) {
    private var current: Dialog? = null
    private var currentPager: MenuPager? = null
    private var windowToken: Any? = null
    val isShowing get() = current?.isShowing == true
    fun close(): Boolean = current?.takeIf { it.isShowing }?.let { it.dismiss(); true } ?: false
    fun readerStateCallback(): (Int) -> Unit {
        val owner = WeakReference(this)
        val pager = WeakReference(currentPager)
        val token = windowToken
        return { mode ->
            val window = owner.get()
            val target = pager.get()
            if (token != null && window != null && target != null && window.windowToken === token && window.currentPager === target && window.isShowing)
                target.readerState(mode)
        }
    }
    fun nextPage() {
        val pager = currentPager ?: return
        if (pager.width > 0) pager.nextPage()
        else pager.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(view: View, left: Int, top: Int, right: Int, bottom: Int,
                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                if (right > left) { pager.removeOnLayoutChangeListener(this); if (currentPager === pager && isShowing) pager.nextPage() }
            }
        })
    }

    fun show(entries: List<MenuEntry?>, context: MenuContext, onAction: (MenuEntry, Boolean) -> Unit, onExit: () -> Unit) {
        close()
        val icons = SkinIconBinder(activity)
        val spec = ChromeLayoutPolicy.resolve(activity)
        val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val dialog = Dialog(activity, R.style.Theme_PureBrowser_Dialog)
        current = dialog
        windowToken = Any()
        val pageSize = if (entries.size > 5) 10 else 5
        val indicator = PageIndicator(activity, (entries.size + pageSize - 1) / pageSize)
        var bounce: ObjectAnimator? = null
        lateinit var pager: MenuPager
        pager = MenuPager(activity, entries, icons, { entry, longPress ->
            if (!longPress && entry.action == MenuAction.TOOLS) pager.nextPage()
            else { dialog.dismiss(); onAction(entry, longPress) }
        }, { indicator.page = it }, {
            dialog.window?.decorView?.let { decor ->
                bounce?.cancel()
                bounce = ObjectAnimator.ofFloat(decor, View.TRANSLATION_Y, 0f, activity.dp(10).toFloat() * if (spec.topAnchored) -1 else 1, 0f)
                    .apply { duration = 280L; start() }
            }
        })
        currentPager = pager
        val header = LinearLayout(activity)
        header.addView(Space(activity), LinearLayout.LayoutParams(0, -2, 3f))
        header.addView(headerButton(R.drawable.ref_ic_exit, activity.getString(R.string.ref_menu_exit), icons) { dialog.dismiss(); onExit() }, LinearLayout.LayoutParams(0, activity.dp(48), 1f))
        header.addView(headerButton(R.drawable.ref_ic_next, activity.getString(android.R.string.cancel), icons) { dialog.dismiss() }.apply {
            rotation = if (spec.topAnchored) 180f else 0f
        }, LinearLayout.LayoutParams(0, activity.dp(48), 1f))
        val dots = LinearLayout.LayoutParams(-2, -2).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = activity.dp(if (spec.topAnchored) 4 else 2); bottomMargin = activity.dp(if (spec.topAnchored) 2 else 4)
        }
        val grid = LinearLayout.LayoutParams(-1, -2).apply { topMargin = activity.dp(12) }
        val headerSize = LinearLayout.LayoutParams(-1, activity.dp(48))
        if (spec.topAnchored) { body.addView(header, headerSize); body.addView(indicator, dots); body.addView(pager, grid) }
        else { body.addView(pager, grid); body.addView(indicator, dots); body.addView(header, headerSize) }
        dialog.setContentView(body, ViewGroup.LayoutParams(-1, -2))
        dialog.setCanceledOnTouchOutside(true)
        var effects: AutoCloseable? = null
        dialog.setOnDismissListener {
            bounce?.cancel(); bounce = null; pager.close(); icons.close(); effects?.close(); effects = null
            if (current === dialog) { current = null; currentPager = null; windowToken = null }
        }
        val measured = minOf(context.parentWidth, context.parentHeight)
        val shortSide = if (measured > 0) measured else minOf(activity.resources.displayMetrics.widthPixels, activity.resources.displayMetrics.heightPixels)
        val width = if (spec.tablet && spec.mode != ChromeMode.TRADITIONAL) minOf(shortSide - activity.dp(96), activity.dp(384)) else shortSide
        dialog.window?.apply {
            attributes = attributes.apply {
                this.width = width; height = -2; gravity = spec.popupGravity; dimAmount = .2f
                windowAnimations = if (spec.topAnchored) R.style.Animation_PureBrowser_MenuTop else R.style.Animation_PureBrowser_MenuBottom
            }
        }
        dialog.show()
        MenuWindowPolicy.apply(activity, dialog)
        dialog.window?.setLayout(width, -2)
        effects = SettingsWindowEffects.attachDialog(dialog)
    }

    private fun headerButton(icon: Int, label: String, icons: SkinIconBinder, action: () -> Unit) = ImageView(activity).apply {
        icons.image(this, icon); imageTintList = ColorStateList.valueOf(activity.palette.icon); contentDescription = label
        setPadding(dp(13), dp(13), dp(13), dp(13)); background = activity.touchFeedback(24); setOnClickListener { action() }
    }
}
