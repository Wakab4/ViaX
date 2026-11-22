package moe.via.browser.ui.components

import android.os.Build
import android.view.View
import android.view.WindowInsets

fun View.applySafeArea() {
    setOnApplyWindowInsetsListener { view, insets ->
        if (Build.VERSION.SDK_INT >= 30) {
            val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val keyboard = insets.getInsets(WindowInsets.Type.ime())
            view.setPadding(safe.left, safe.top, safe.right, maxOf(safe.bottom, keyboard.bottom))
        } else {
            @Suppress("DEPRECATION")
            view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
        }
        insets
    }
}
