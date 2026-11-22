package moe.via.browser.ui.components.insets

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import moe.via.browser.ui.components.dp

/** l3.c/d distributes page insets to the toolbar, scrolling tail and root independently. */
internal class NativePageInsets(private val root: View, private val toolbar: View?) {
    private var bottom: View? = null

    init {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets -> apply(insets); insets }
    }

    fun bottom(view: View?) {
        bottom = view
        ViewCompat.requestApplyInsets(root)
    }

    private fun apply(insets: WindowInsetsCompat) {
        val mask = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        if (!NativeFullscreenState.requested(root, insets)) {
            distribute(insets, true)
            return
        }
        val cutout = insets.getInsets(mask)
        if (cutout.left == 0 && cutout.top == 0 && cutout.right == 0 && cutout.bottom == 0) {
            distribute(insets, false)
            return
        }
        if (Resources.getSystem().configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            root.setPadding(cutout.left, 0, cutout.right, 0)
            clear(toolbar, bottom)
            return
        }
        if (toolbar == null && bottom == null) {
            root.setPadding(0, cutout.top, 0, if (insets.getInsets(WindowInsetsCompat.Type.ime()).bottom > 0) 0 else cutout.bottom)
            return
        }
        val displayCutout = insets.displayCutout
        val width = root.measuredWidth
        val height = root.measuredHeight
        if (displayCutout != null && width > 0 && height > 0) {
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val edge = root.dp(16)
            var topLeft = 0; var topRight = 0; var bottomLeft = 0; var bottomRight = 0
            for (rect in displayCutout.boundingRects) {
                if (rect.top <= edge) {
                    if (rect.left < edge * 2) topLeft = maxOf(topLeft, rect.right)
                    else if (rect.right >= width - edge * 2) topRight = maxOf(topRight, width - rect.left)
                } else if (imeBottom <= 0 && rect.bottom >= height - edge) {
                    if (rect.left < edge * 2) bottomLeft = maxOf(bottomLeft, rect.right)
                    else if (rect.right >= width - edge * 2) bottomRight = maxOf(bottomRight, width - rect.left)
                }
            }
            toolbar?.setPadding(topLeft, 0, topRight, 0)
            bottom?.setPadding(bottomLeft, 0, bottomRight, 0)
            root.setPadding(0, 0, 0, 0)
        }
    }

    private fun distribute(insets: WindowInsetsCompat, apply: Boolean) {
        if (Build.VERSION.SDK_INT < 29) return
        if (!apply) { clear(toolbar, bottom, root); return }
        val safe = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        toolbar?.setPadding(0, safe.top, 0, 0)
        bottom?.setPadding(0, 0, 0, safe.bottom)
        root.setPadding(safe.left, if (toolbar == null) safe.top else 0, safe.right, if (bottom == null) safe.bottom else 0)
    }

    private fun clear(vararg views: View?) { views.forEach { it?.setPadding(0, 0, 0, 0) } }

    companion object {
        @Suppress("DEPRECATION")
        fun prepareWindow(window: Window) {
            if (Build.VERSION.SDK_INT >= 29 && Build.VERSION.SDK_INT < 35) {
                if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false)
                else {
                    window.decorView.systemUiVisibility = 1792
                    window.statusBarColor = Color.TRANSPARENT
                    window.navigationBarColor = Color.TRANSPARENT
                }
            }
        }
    }
}
