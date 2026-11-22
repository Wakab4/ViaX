package moe.via.browser.feature.settings.data.updates

import android.content.SharedPreferences
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.storage.preferences.BrowserPreferences
import java.util.Locale

/** x9.m/f/a/l/b/i retain the original flag defaults, inversions and GP channel conditions. */
internal class NativePreferenceFlagUpdates(private val values: SharedPreferences) {
    fun webSettings() {
        val default = BrowserPreferences.DEFAULT_WEB_FLAGS
        var flags = 0
        fun old(key: String, mask: Int, inverted: Boolean = false) {
            val initial = bit(default, mask) xor inverted
            flags = set(flags, mask, values.getBoolean(key, initial) xor inverted)
        }
        old("adblockplus", 1)
        old("locationaccess", 2)
        old("passwords", 4)
        old("java", 8)
        old("blockimages", 16, inverted = true)
        old("imageswitch", 32)
        old("incognitoMode", 64)
        old("donottrack", 128)
        old("datasaving", 256)
        old("smartback", 512)
        old("ignoresecondarysslerror", 1024, inverted = true)
        old("requestdesktopsite", 2048)
        old("webpagedebug", 4096)
        flags = set(flags, 8192, true)
        flags = set(flags, 16384, !values.getBoolean("3rdcookies", bit(default, 16384)))
        old("blockpopup", 32768)
        values.edit().putInt("webflag", flags).apply()
    }

    fun thirdPartyMode() {
        val flags = set(set(webFlags(), 33554432, false), 67108864, true)
        values.edit().putInt("webflag", flags).apply()
    }

    fun webPermissionDefaults() {
        values.edit().putInt("webflag", webFlags() or 8388608 or 16777216).apply()
    }

    fun appSettings() {
        val initial = BrowserPreferences.DEFAULT_APP_FLAGS
        var flags = initial
        fun old(key: String, mask: Int, initialMask: Int = mask) {
            flags = set(flags, mask, values.getBoolean(key, bit(initial, initialMask)))
        }
        old("hidestatus", 1, initialMask = 4)
        old("colormode", 2)
        old("autovideobtn", 4)
        old("volume", 8)
        old("pulltorefresh", 16)
        old("backforwardgesture", 32)
        old("builtin", 64)
        values.edit().putInt("appflag", flags).apply()
    }

    fun countryFlag() {
        values.edit().putInt("appflag", set(values.getInt("appflag", BrowserPreferences.DEFAULT_APP_FLAGS), 1024, inChina())).apply()
    }

    fun searchHistoryFlag() {
        val suggestions = values.getInt("searchsuggestion", 31)
        if (suggestions and 8 != 0) values.edit().putInt("searchsuggestion", suggestions or 32).apply()
    }

    private fun webFlags(): Int = try { values.getInt("webflag", BrowserPreferences.DEFAULT_WEB_FLAGS) }
    catch (error: Exception) {
        NativeLog.warn(null, error)
        values.getLong("webflag", BrowserPreferences.DEFAULT_WEB_FLAGS.toLong()).toInt().also {
            values.edit().putInt("webflag", it).apply()
        }
    }

    private fun inChina() = Locale.getDefault().country.equals("CN", ignoreCase = true)
    private fun bit(flags: Int, mask: Int) = flags and mask != 0
    private fun set(flags: Int, mask: Int, enabled: Boolean) = if (enabled) flags or mask else flags and mask.inv()
}
