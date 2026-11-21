package moe.pure.browser.feature.report

import android.os.LocaleList
import org.json.JSONObject
import java.util.Locale

/** Only the fields sent by the reference reporting form; no browser database or device identifiers. */
data class ReportPayload(
    val url: String,
    val reason: Int,
    val title: String?,
    val note: String?,
    val locale: String = deviceLocale()
) {
    internal fun toJson(): String = JSONObject().apply {
        put("url", url)
        put("reason", reason)
        if (!title.isNullOrEmpty()) put("title", title)
        if (!note.isNullOrEmpty()) put("note", note)
        put("locale", locale)
    }.toString()

    companion object {
        private fun deviceLocale(): String {
            val locale = LocaleList.getAdjustedDefault()[0] ?: Locale.getDefault()
            val language = when (locale.language) {
                "in" -> "id"; "iw" -> "he"; "ji" -> "yi"; "jw" -> "jv"; "tl" -> "fil"
                else -> locale.language
            }
            if (language == "no" && locale.country == "NO" && locale.variant == "NY") return "nn-NO"
            return language + locale.country.takeIf(String::isNotEmpty)?.let { "-$it" }.orEmpty()
        }
    }
}
