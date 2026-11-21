package moe.pure.browser.ui.components

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import android.view.WindowManager
import moe.pure.browser.ui.chrome.ChromeLayoutPolicy

object BrowserPopup {
    fun create(activity: Activity, body: View): Dialog = object : Dialog(activity) {
        private var effects: AutoCloseable? = null
        override fun show() {
            super.show()
            effects?.close()
            effects = moe.pure.browser.feature.settings.context.SettingsWindowEffects.attachDialog(this)
        }
        override fun dismiss() { effects?.close(); effects = null; super.dismiss() }
    }.apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(body)
        setCanceledOnTouchOutside(true)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val spec = ChromeLayoutPolicy.resolve(activity)
            setGravity(spec.popupGravity)
            setLayout(spec.popupWidth(activity), WindowManager.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = 0.2f }
        }
    }
}
