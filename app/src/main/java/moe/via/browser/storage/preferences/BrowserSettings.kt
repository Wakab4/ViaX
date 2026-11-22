package moe.via.browser.storage.preferences

import android.content.Context
import android.content.SharedPreferences
class BrowserSettings(private val context: Context, val values: SharedPreferences = context.getSharedPreferences("browser", Context.MODE_PRIVATE)) {
    val preferences = BrowserPreferences(values)
    var night: Boolean
        get() = BrowserNightMode.global(context)
        set(value) { BrowserNightMode.set(context, value) }
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
}
