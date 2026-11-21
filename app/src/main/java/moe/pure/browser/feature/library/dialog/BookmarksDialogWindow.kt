package moe.pure.browser.feature.library.dialog

import android.app.Activity
import android.app.Dialog
import android.view.Gravity
import android.view.Window
import moe.pure.browser.R
import moe.pure.browser.feature.settings.context.SettingsWindowEffects
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.dialog.DialogLayoutOwner

internal class BookmarksDialogWindow(private val activity: Activity, view: BookmarksDialogView) : Dialog(activity, R.style.Theme_PureBrowser_Dialog), DialogLayoutOwner {
    private var effects: AutoCloseable? = null
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(view.root); setCanceledOnTouchOutside(true)
        window?.apply { setGravity(Gravity.CENTER); setDimAmount(.4f) }
    }
    override fun show() {
        super.show(); updateGeometry()
        @Suppress("DEPRECATION")
        window?.decorView?.systemUiVisibility = activity.window.decorView.systemUiVisibility
        effects?.close(); effects = SettingsWindowEffects.attachDialog(this)
    }
    override fun updateGeometry() {
        val metrics = activity.resources.displayMetrics
        val edge = minOf(metrics.widthPixels, metrics.heightPixels) * 4 / 5
        window?.setLayout(minOf(edge, activity.dp(450)), minOf(edge, activity.dp(600)))
    }
    override fun dismiss() { effects?.close(); effects = null; super.dismiss() }
}
