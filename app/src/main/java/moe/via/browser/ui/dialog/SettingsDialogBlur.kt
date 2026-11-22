package moe.via.browser.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import moe.via.browser.storage.preferences.BrowserPreferences
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.function.Consumer

/** Native z8.l / p8.l uses 80 px background blur and 30 px behind/fallback blur. */
internal object SettingsDialogBlur {
    private val bindings = WeakHashMap<Window, WeakReference<Binding>>()
    private val fallbacks = WeakHashMap<View, Int>()
    fun attach(dialog: Dialog): AutoCloseable {
        if (Build.VERSION.SDK_INT < 31) return AutoCloseable {}
        val window = dialog.window ?: return AutoCloseable {}
        val binding = bindings[window]?.get()?.takeUnless { it.closed } ?: Binding(dialog, window).also { bindings[window] = WeakReference(it) }
        return binding.retain()
    }
    private class Binding(dialog: Dialog, private val window: Window) : View.OnAttachStateChangeListener {
        private val root = window.decorView
        private val app = dialog.context.applicationContext
        private val preferences = app.getSharedPreferences("browser", Context.MODE_PRIVATE)
        private val manager = window.windowManager
        private val owner = activity(dialog.context)?.window?.decorView
        private val behind = dialog !is BrowserDialog || !dialog.isAnchored
        private val originalBackground = root.background?.mutate()?.also { root.background = it }
        private val originalAlpha = originalBackground?.alpha ?: 255
        private val originalDim = window.attributes.dimAmount
        private val originalFlags = window.attributes.flags
        private val changed = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key == "labflag" || key == null) update() }
        private val blurChanged = Consumer<Boolean> { update() }
        private var registered = false
        private var fallback = false
        private var references = 0
        var closed = false
            private set
        init {
            root.addOnAttachStateChangeListener(this)
            preferences.registerOnSharedPreferenceChangeListener(changed)
            if (root.isAttachedToWindow) register()
            update()
        }
        fun retain(): AutoCloseable {
            references++; var released = false
            return AutoCloseable { if (!released) { released = true; references--; if (references <= 0) close() } }
        }
        private fun register() {
            if (registered || closed || Build.VERSION.SDK_INT < 31) return
            registered = true
            manager.addCrossWindowBlurEnabledListener(root.context.mainExecutor, blurChanged)
        }
        private fun update() {
            if (closed || Build.VERSION.SDK_INT < 31) return
            val enabled = BrowserPreferences(preferences).blurEnabled
            if (!enabled) { restore(); return }
            val night = moe.via.browser.storage.preferences.BrowserNightMode.isNight(root.context)
            val native = manager.isCrossWindowBlurEnabled
            originalBackground?.alpha = if (native) if (night) 216 else 204 else if (behind) if (night) 229 else 216 else originalAlpha
            window.setBackgroundBlurRadius(if (native) 80 else 0)
            if (behind) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(.2f)
                if (native) window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND) else window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply { setBlurBehindRadius(if (native) 30 else 0) }
            }
            setFallback(behind && !native)
        }
        private fun setFallback(enabled: Boolean) {
            val view = owner ?: return
            if (enabled == fallback || Build.VERSION.SDK_INT < 31) return
            fallback = enabled
            val count = (fallbacks[view] ?: 0) + if (enabled) 1 else -1
            if (count <= 0) { fallbacks.remove(view); view.setRenderEffect(null) }
            else { fallbacks[view] = count; view.setRenderEffect(RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.MIRROR)) }
        }
        private fun restore() {
            if (Build.VERSION.SDK_INT < 31) return
            setFallback(false)
            window.setBackgroundBlurRadius(0)
            window.attributes = window.attributes.apply {
                setBlurBehindRadius(0)
                dimAmount = originalDim
                flags = (flags and (WindowManager.LayoutParams.FLAG_BLUR_BEHIND or WindowManager.LayoutParams.FLAG_DIM_BEHIND).inv()) or
                    (originalFlags and (WindowManager.LayoutParams.FLAG_BLUR_BEHIND or WindowManager.LayoutParams.FLAG_DIM_BEHIND))
            }
            originalBackground?.alpha = originalAlpha
        }
        override fun onViewAttachedToWindow(view: View) { register(); update() }
        override fun onViewDetachedFromWindow(view: View) = close()
        private fun close() {
            if (closed) return
            closed = true
            preferences.unregisterOnSharedPreferenceChangeListener(changed)
            root.removeOnAttachStateChangeListener(this)
            if (registered && Build.VERSION.SDK_INT >= 31) manager.removeCrossWindowBlurEnabledListener(blurChanged)
            registered = false
            restore(); bindings.remove(window)
        }
    }
    private fun activity(context: Context): Activity? {
        var current = context
        while (current is ContextWrapper) { if (current is Activity) return current; val base = current.baseContext; if (base === current) break; current = base }
        return current as? Activity
    }
}
