package moe.pure.browser.feature.offline.save

import android.content.Context
import moe.pure.browser.feature.settings.data.BrowserPreferences

/** w9.a.p/I is appflag bit 16384; changing the PDF choice leaves every other Native bit intact. */
internal class SavedPagePreferences(context: Context) {
    private val values = context.applicationContext.getSharedPreferences("browser", Context.MODE_PRIVATE)
    private val preferences = BrowserPreferences(values)
    var pdf: Boolean
        get() = preferences.int("appflag", BrowserPreferences.DEFAULT_APP_FLAGS) and 16384 != 0
        set(value) {
            val flags = preferences.int("appflag", BrowserPreferences.DEFAULT_APP_FLAGS)
            values.edit().putInt("appflag", if (value) flags or 16384 else flags and 16384.inv()).apply()
        }
}
