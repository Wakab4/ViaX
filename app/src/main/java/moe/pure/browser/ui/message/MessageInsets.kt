package moe.pure.browser.ui.message

import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import moe.pure.browser.ui.components.dp

/** f6.h.d keeps its physical base margin and raises short notices above the visible keyboard. */
internal object MessageInsets {
    fun install(view: View, baseMargin: Int) {
        var previous = 0
        view.setOnApplyWindowInsetsListener { target, insets ->
            val keyboard = height(target, insets)
            if (keyboard != previous) {
                previous = keyboard
                val params = target.layoutParams as? FrameLayout.LayoutParams
                if (params != null) {
                    params.bottomMargin = if (keyboard <= 0) baseMargin else minOf(baseMargin, target.context.dp(32)) + keyboard
                    target.layoutParams = params
                }
            }
            insets
        }
        view.post { if (view.isAttachedToWindow) view.requestApplyInsets() }
    }
    private fun height(view: View, insets: WindowInsets): Int {
        if (Build.VERSION.SDK_INT >= 30) {
            val ime = insets.getInsets(WindowInsets.Type.ime())
            return ime.bottom - ime.top
        }
        @Suppress("DEPRECATION") val stable = insets.stableInsetBottom
        @Suppress("DEPRECATION") val system = insets.systemWindowInsetBottom
        if (system > stable) return system
        val frame = Rect(); val root = view.rootView
        root.getWindowVisibleDisplayFrame(frame)
        val position = IntArray(2); root.getLocationOnScreen(position)
        return (position[1] + root.height - frame.bottom).takeIf { it > stable } ?: 0
    }
}
