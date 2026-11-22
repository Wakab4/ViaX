package moe.via.browser.browser.ui.chrome

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.insets.NativeFullscreenState

/** Via's browser-root inset distribution: consume bars for children, then position chrome. */
internal object ChromeWindowInsets {
    data class State(
        val gameMode: Boolean,
        val fullscreenSetting: Boolean,
        val topToolbar: Boolean,
        val tabStripPresent: Boolean,
        val revealView: View?
    )

    data class Result(val childInsets: WindowInsetsCompat, val keyboardVisible: Boolean)

    fun apply(root: ChromeViewportLayout, insets: WindowInsetsCompat, state: State): Result {
        val keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
        val consumedTypes = WindowInsetsCompat.Type.systemBars() or
            WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()
        val childInsets = WindowInsetsCompat.Builder(insets)
            .setInsets(consumedTypes, Insets.NONE)
            .setVisible(WindowInsetsCompat.Type.ime(), false)
            .build()
        val result = Result(childInsets, keyboardVisible)

        if (!NativeFullscreenState.requested(root, insets)) {
            if (Build.VERSION.SDK_INT >= 29) {
                val safe = insets.getInsetsIgnoringVisibility(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                root.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            }
            clear(root.topBars, root.bottomBars, state.revealView)
            return result
        }

        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        if (cutout == Insets.NONE) {
            root.setPadding(0, 0, 0, 0)
            return result
        }

        if (!state.fullscreenSetting && state.gameMode) {
            if (Resources.getSystem().configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                root.setPadding(cutout.left, 0, cutout.right, 0)
            else root.setPadding(0, cutout.top, 0, cutout.bottom)
            clear(root.topBars, root.bottomBars, state.revealView)
            return result
        }

        val displayCutout = insets.displayCutout ?: return result
        val width = root.measuredWidth
        val height = root.measuredHeight
        if (width <= 0 || height <= 0) return result

        val top = IntArray(4)
        val bottom = IntArray(4)
        val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val edge = root.dp(16)
        for (rect in displayCutout.boundingRects) {
            if (rect.top <= edge) {
                if (rect.left < edge * 2) top[0] = maxOf(top[0], rect.right)
                else if (rect.right >= width - edge * 2) top[2] = maxOf(top[2], width - rect.left)
                top[1] = maxOf(top[1], rect.bottom)
            } else if (imeBottom <= 0 && rect.bottom >= height - edge) {
                if (rect.left < edge * 2) bottom[0] = maxOf(bottom[0], rect.right)
                else if (rect.right >= width - edge * 2) bottom[2] = maxOf(bottom[2], width - rect.left)
                bottom[3] = maxOf(bottom[3], height - rect.top)
            }
        }

        val barHeight = if (state.topToolbar) root.topBars.measuredHeight else root.bottomBars.measuredHeight
        val cutoutDepth = if (state.topToolbar) top[1] else bottom[3]
        if (!state.tabStripPresent || state.revealView == null || cutoutDepth >= barHeight * 3 / 2) {
            root.topBars.setPadding(top[0], 0, top[2], 0)
            root.bottomBars.setPadding(bottom[0], 0, bottom[2], 0)
            state.revealView?.setPadding(0, 0, 0, 0)
        } else {
            clear(root.topBars, root.bottomBars)
            val side = if (state.topToolbar) top else bottom
            state.revealView.setPadding(side[0], 0, side[2], 0)
        }
        root.setPadding(0, 0, 0, 0)
        return result
    }

    private fun clear(vararg views: View?) { views.forEach { it?.setPadding(0, 0, 0, 0) } }
}
