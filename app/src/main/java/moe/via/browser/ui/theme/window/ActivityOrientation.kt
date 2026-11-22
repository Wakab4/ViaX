package moe.via.browser.ui.theme.window

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build

internal fun applyActivityOrientation(activity: Activity, orientation: Int) {
    if (Build.VERSION.SDK_INT >= 36 && activity.resources.configuration.smallestScreenWidthDp >= 600) return
    activity.requestedOrientation = when (orientation) {
        1 -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        2 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        3 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else -> ActivityInfo.SCREEN_ORIENTATION_USER
    }
}
