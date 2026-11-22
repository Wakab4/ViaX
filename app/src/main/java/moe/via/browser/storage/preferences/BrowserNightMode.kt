package moe.via.browser.storage.preferences

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build

interface BrowserThemeOwner {
    val browserNightOverride: Boolean?
}

data class NightModeChange(val night: Boolean, val mode: Int, val followingSystem: Boolean,
    val showFollowingSystemNotice: Boolean)

/** Native nightmode2: 0/1 explicit; 2/3 follow the system's current light/dark value. */
object BrowserNightMode {
    fun windowOverride(context: Context): Boolean? {
        var current = context
        while (true) {
            if (current is BrowserThemeOwner) current.browserNightOverride?.let { return it }
            val next = (current as? ContextWrapper)?.baseContext ?: return null
            if (next === current) return null
            current = next
        }
    }
    private fun systemNight() = Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    fun global(context: Context): Boolean {
        val values = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
        val stored = values.getInt("nightmode2", 2)
        return stored == 1 || stored == 3
    }
    fun isNight(context: Context): Boolean = windowOverride(context) ?: global(context)
    fun set(context: Context, night: Boolean) {
        val native = if (night == systemNight()) if (night) 3 else 2 else if (night) 1 else 0
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().putInt("nightmode2", native).apply()
    }
    fun toggle(context: Context): NightModeChange {
        val next = !global(context)
        val following = next == systemNight()
        set(context, next)
        return NightModeChange(next, if (following) if (next) 3 else 2 else if (next) 1 else 0,
            following, following && Build.VERSION.SDK_INT >= 29)
    }
    /** w9.k.m0 updates following-system modes only when the effective light/dark value changes. */
    fun synchronize(context: Context): Boolean {
        val values = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
        val stored = values.getInt("nightmode2", 2)
        if (stored == 0 || stored == 1) return false
        val night = systemNight()
        if (night == (stored == 3)) return false
        values.edit().putInt("nightmode2", if (night) 3 else 2).apply()
        return true
    }
}
