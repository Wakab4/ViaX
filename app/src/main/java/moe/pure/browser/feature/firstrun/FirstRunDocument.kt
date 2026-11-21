package moe.pure.browser.feature.firstrun

import android.content.SharedPreferences
import moe.pure.browser.R
import java.util.Locale

/** The two fixed reference documents are opened only after the user selects a link. */
internal enum class FirstRunDocument(val title: Int, private val filename: String) {
    TERMS(R.string.ref_first_run_terms, "terms-of-use.html"),
    PRIVACY(R.string.ref_first_run_privacy, "privacy-policy.html");

    fun url(preferences: SharedPreferences): String {
        val defaultServer = if (Locale.getDefault().country.equals("CN", true)) 1 else 0
        val server = (preferences.all["cloudserver"] as? Number)?.toInt() ?: defaultServer
        return "https://viayoo.com/${if (server == 1) "zh-cn" else "en"}/docs/$filename"
    }

    fun accepts(url: String): Boolean = url == "https://viayoo.com/zh-cn/docs/$filename" ||
        url == "https://viayoo.com/en/docs/$filename"
}
