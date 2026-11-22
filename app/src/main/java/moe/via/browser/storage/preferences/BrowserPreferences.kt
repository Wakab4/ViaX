package moe.via.browser.storage.preferences

import android.content.SharedPreferences
import java.util.Locale
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import org.json.JSONObject

/** Typed access to Via's preference words and scalar fields. */
class BrowserPreferences(val values: SharedPreferences) {
    private fun flags(key: String): Int {
        val default = when (key) { APP -> DEFAULT_APP_FLAGS; WEB -> DEFAULT_WEB_FLAGS; else -> 0 }
        if (key != WEB) return int(key, default)
        // Via converts its original webflag Long value to the current Int representation.
        return try { int(key, default) } catch (_: ClassCastException) {
            values.getLong(key, default.toLong()).toInt().also { values.edit().putInt(key, it).apply() }
        }
    }
    private fun flag(word: String, mask: Int, inverted: Boolean = false) = object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = (flags(word) and mask != 0) xor inverted
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            val next = if (value xor inverted) flags(word) or mask else flags(word) and mask.inv()
            values.edit().putInt(word, next).apply()
        }
    }
    private fun integer(key: String, default: Int, range: IntRange? = null) = object : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            val value = int(key, default)
            return if (range == null) value else value.coerceIn(range)
        }
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            values.edit().putInt(key, if (range == null) value else value.coerceIn(range)).apply()
        }
    }
    private fun string(key: String, default: String = "") = object : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = values.getString(key, default).orEmpty()
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) { values.edit().putString(key, value).apply() }
    }

    var fullscreen by flag(APP, 1)
    var colorMode by flag(APP, 2)
    var autoSniff by flag(APP, 4)
    var volumePaging by flag(APP, 8)
    var confirmExit by flag(APP, 16)
    var swipeNavigation by flag(APP, 32)
    var builtinFilters by flag(APP, 64)
    var videoGestures by flag(APP, 128)
    var tabBar by flag(APP, 256)
    var undoClosePrompt by flag(APP, 512)
    var fulltextExpansion by flag(APP, 1024)
    var readerConfirmation by flag(APP, 8192)
    var searchToolbar: Boolean
        get() = values.getBoolean(SEARCH_TOOLBAR_KEY, false)
        set(value) { values.edit().putBoolean(SEARCH_TOOLBAR_KEY, value).apply() }
    var promptSavePasswords by flag(APP, 131072, inverted = true)
    var customTabsDisabled by flag(APP, 262144)
    var predictiveBackDisabled by flag(APP, 524288)

    var adblock by flag(WEB, 1)
    var locationEnabled by flag(WEB, 2)
    var javascript by flag(WEB, 8)
    var imagesEnabled by flag(WEB, 16)
    var wifiImages by flag(WEB, 32)
    var incognito by flag(WEB, 64)
    var doNotTrack by flag(WEB, 128)
    var saveData by flag(WEB, 256)
    var backCache by flag(WEB, 512)
    var desktop by flag(WEB, 2048)
    var webDebugging by flag(WEB, 4096)
    var cookies by flag(WEB, 8192)
    var thirdPartyCookies by flag(WEB, 16384)
    var popupAllowed by flag(WEB, 32768, inverted = true)
    var globalPrivacyControl by flag(WEB, 65536)
    var forceDark: Boolean
        get() = values.getBoolean("nightcss", DARKENING_DEFAULT)
        set(value) { values.edit().putBoolean("nightcss", value).apply() }
    var forceZoom by flag(WEB, 524288)
    var safeBrowsingDisabled by flag(WEB, 2097152)
    var cameraAsk by flag(WEB, 8388608)
    var microphoneAsk by flag(WEB, 16777216)
    var redirectsAllowed by flag(WEB, 134217728, inverted = true)
    var scriptsEnabled by flag(WEB, 268435456, inverted = true)
    var disableWebRtc by flag(WEB, 536870912)
    var vibrationAllowed by flag(WEB, 1073741824)
    var supportMultipleWindows by flag(WEB, 4194304)

    var toolbarLayout by integer("appui2", -1, -1..3)
    var autoHide by integer("fullscreenmode", 0, 0..2)
    var addressContent by integer("urlbox", 0, 0..2)
    var restoreTabs by integer("restoreclosedtabs", 0, 0..2)
    var ignoredSslWarnings by integer("ignoredsslwarning", 0, 0..63)
    var nightFilter by integer("nightfilter", 77, 0..255)
    var clearDataMask: Int
        get() = int("cleardata2", 7) and 127
        set(value) { values.edit().putInt("cleardata2", value and 127).apply() }
    var exitClearMask: Int
        get() = int("cleardataonexit2", 0) and 127
        set(value) { values.edit().putInt("cleardataonexit2", value and 127).apply() }
    var language by string("language")
    var uiFont: String
        get() = values.getString("uifont", "").orEmpty()
        set(value) {
            moe.via.browser.ui.theme.fonts.NativeFontSelection.changed()
            values.edit().putString("uifont", value).apply()
        }
    var skin by string("skin")
    var externalVideoPlayer by string("videoplayer")
    var homepage by string("home", "about:home")
    var simplifyUserAgent by flag("webflag2", 1)
    var desktopUserAgentId by integer("duachoice", 0)
    var labsEnabled by flag("labflag", 1)
    var blurEnabled by flag("labflag", 2)
    var settingsBackground by flag("labflag", 4)

    var imageMode: Int
        get() = if (wifiImages) 2 else if (imagesEnabled) 0 else 1
        set(value) {
            val mode = value.coerceIn(0, 2)
            val next = (flags(WEB) and (16 or 32).inv()) or (if (mode != 1) 16 else 0) or (if (mode == 2) 32 else 0)
            values.edit().putInt(WEB, next).apply()
        }
    var orientation: Int
        get() = (int("screenOrientation", 1) - 1).coerceIn(0, 3)
        set(value) { values.edit().putInt("screenOrientation", value.coerceIn(0, 3) + 1).apply() }
    var textZoom: Int
        get() {
            val stored = int("textsize", 100)
            if (stored > 5) return stored
            return fontPercentage(stored).also { values.edit().putInt("textsize", it).apply() }
        }
        set(value) { values.edit().putInt("textsize", fontPercentage(value)).apply() }
    var userAgentId by integer("uachoice", 0)
    var longMenuDisabled: Set<Int>
        get() = if (!values.contains("hiddenctxmenus")) setOf(6) else values.getString("hiddenctxmenus", "").orEmpty().split(',').mapNotNull(String::toIntOrNull).toSet()
        set(value) { values.edit().putString("hiddenctxmenus", value.sorted().joinToString(",")).apply() }
    fun resetLongMenu() { values.edit().remove("hiddenctxmenus").apply() }
    var clipboardPolicy: Int
        get() = if (flags(WEB) and 262144 != 0) 3 else if (flags(WEB) and 131072 != 0) 2 else 1
        set(value) { policy(131072, 262144, value) }
    var openAppsPolicy: Int
        get() = if (flags(WEB) and 67108864 != 0) 3 else if (flags(WEB) and 33554432 != 0) 2 else 1
        set(value) { policy(33554432, 67108864, value) }

    private fun policy(denied: Int, ask: Int, value: Int) {
        val next = (flags(WEB) and (denied or ask).inv()) or when (value) { 2 -> denied; 3 -> ask; else -> 0 }
        values.edit().putInt(WEB, next).apply()
    }
    private fun fontPercentage(value: Int) = if (value > 5) value else intArrayOf(130, 115, 100, 85, 70)[(value.takeIf { it in 1..5 } ?: 3) - 1]
    fun action(binding: ActionBinding) = NativeAction.fromId(int(binding.key, binding.defaultAction.id))
    fun setAction(binding: ActionBinding, action: NativeAction) { values.edit().putInt(binding.key, action.id).apply() }
    fun int(key: String, default: Int) = values.getInt(key, default)

    /** Apply Native backup values using their stored preference types. */
    fun applyNativeSettings(json: JSONObject) {
        for (key in NativeBackupPreferenceKeys.booleans) if (!json.isNull(key)) values.edit().putBoolean(key, json.optBoolean(key)).apply()
        for (key in NativeBackupPreferenceKeys.strings) if (!json.isNull(key)) values.edit().putString(key, json.optString(key)).apply()
        for (key in NativeBackupPreferenceKeys.integers) if (!json.isNull(key)) values.edit().putInt(key, json.optInt(key)).apply()
        for (key in NativeBackupPreferenceKeys.longs) if (!json.isNull(key)) values.edit().putLong(key, json.optLong(key)).apply()
    }

    companion object {
        const val APP = "appflag"
        const val WEB = "webflag"
        const val SEARCH_TOOLBAR_KEY = "searchtoolbar"
        val DEFAULT_APP_FLAGS = 226 or if (Locale.getDefault().country.equals("CN", true)) 1024 else 0
        const val DEFAULT_WEB_FLAGS = 96495261
        private val DARKENING_DEFAULT by lazy { moe.via.browser.webview.compat.WebViewCapabilities.darkeningSupported() }
    }
}
