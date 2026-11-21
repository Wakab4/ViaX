package moe.pure.browser.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.app.Activity
import android.os.Build
import moe.pure.browser.feature.settings.data.BrowserPreferences

class BrowserSettings(private val context: Context, val values: SharedPreferences = context.getSharedPreferences("browser", Context.MODE_PRIVATE)) {
    val preferences = BrowserPreferences(values)
    var night: Boolean
        get() = moe.pure.browser.ui.theme.BrowserNightMode.global(context)
        set(value) { moe.pure.browser.ui.theme.BrowserNightMode.set(context, value) }
    var fullscreen: Boolean
        get() = preferences.fullscreen
        set(value) { preferences.fullscreen = value }
    var imageMode: Int
        get() = preferences.imageMode
        set(value) { preferences.imageMode = value }
    var textZoom: Int
        get() = preferences.textZoom
        set(value) { preferences.textZoom = value }
    var orientation: Int
        get() = preferences.orientation
        set(value) { preferences.orientation = value }
    var userAgentId: Int
        get() = preferences.userAgentId
        set(value) { preferences.userAgentId = value }

    fun applyOrientation(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 36 && activity.resources.configuration.smallestScreenWidthDp >= 600) return
        activity.requestedOrientation = when (orientation) {
            1 -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            2 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            3 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_USER
        }
    }
}
