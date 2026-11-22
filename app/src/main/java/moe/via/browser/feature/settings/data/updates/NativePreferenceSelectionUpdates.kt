package moe.via.browser.feature.settings.data.updates

import android.content.SharedPreferences
import java.util.Locale

/** x9.k/h use only Via's original agent/search fields and original index tables. */
internal class NativePreferenceSelectionUpdates(private val values: SharedPreferences) {
    fun userAgent() {
        val choice = when (values.getInt("agentchoose", 0)) {
            2 -> -3
            3 -> -4
            4 -> -6
            5 -> -8
            6 -> -999
            else -> 0
        }
        values.edit().putInt("uachoice", choice).apply()
        if (choice > 0 || choice <= -999) values.edit().putString("uastring", values.getString("userAgentString", "")).apply()
    }

    fun search() {
        val index = values.getInt("searchweb", -1)
        if (index == -1) return
        val locale = Locale.getDefault()
        val choices = when {
            locale.country.uppercase(Locale.ROOT).equals("CN", ignoreCase = true) -> intArrayOf(-999, -2, -1, -3, -4, -5, -6)
            locale.language.uppercase(Locale.ROOT).equals("RU", ignoreCase = true) -> intArrayOf(-999, -2, -1, -3, -7, -9, -10)
            else -> intArrayOf(-999, -2, -1, -3, -8, -9, -10)
        }
        if (index >= 0 && index < choices.size) values.edit().putInt("search2", choices[index]).apply()
    }
}
