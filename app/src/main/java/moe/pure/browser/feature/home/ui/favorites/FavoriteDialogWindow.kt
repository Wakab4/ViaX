package moe.pure.browser.feature.home.ui.favorites

import android.app.Dialog
import android.os.Build
import android.view.Gravity
import android.view.View
import moe.pure.browser.R
import moe.pure.browser.feature.settings.data.BrowserPreferences
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette
import java.util.function.Consumer

/** k8.a + h0.g3=2: 384dp ceiling and background blur only, without an Activity blur fallback. */
internal object FavoriteDialogWindow {
    fun bind(dialog: Dialog): AutoCloseable {
        val context = dialog.context
        val window = checkNotNull(dialog.window)
        val metrics = context.resources.displayMetrics
        window.attributes = window.attributes.apply {
            width = minOf(context.dp(384), minOf(metrics.widthPixels, metrics.heightPixels) - context.dp(72))
            gravity = Gravity.CENTER
        }
        if (Build.VERSION.SDK_INT < 31 || !BrowserPreferences(context.getSharedPreferences("browser", 0)).blurEnabled) return AutoCloseable {}
        val background = checkNotNull(context.getDrawable(R.drawable.browser_dialog_background))
        window.setBackgroundDrawable(background)
        val root = window.decorView
        val manager = window.windowManager
        var registered = false
        val changed = Consumer<Boolean> { enabled ->
            if (enabled) {
                background.alpha = if (context.palette.night) 216 else 204
                window.setDimAmount(.2f)
                window.setBackgroundBlurRadius(80)
                window.attributes = window.attributes
            }
        }
        fun register() { if (!registered) { registered = true; manager.addCrossWindowBlurEnabledListener(changed) } }
        fun unregister() { if (registered) { registered = false; manager.removeCrossWindowBlurEnabledListener(changed) } }
        val attach = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = register()
            override fun onViewDetachedFromWindow(view: View) = unregister()
        }
        root.addOnAttachStateChangeListener(attach)
        if (root.isAttachedToWindow) register()
        return AutoCloseable { root.removeOnAttachStateChangeListener(attach); unregister() }
    }
}
