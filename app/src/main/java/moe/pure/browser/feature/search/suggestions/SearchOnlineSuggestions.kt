package moe.pure.browser.feature.search.suggestions

import android.os.LocaleList
import android.text.Html
import moe.pure.browser.feature.search.model.SearchSuggestion
import moe.pure.browser.feature.search.model.SearchSuggestionType
import moe.pure.browser.feature.search.resolve.SearchTemplate
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class SearchSuggestionRequest {
    val cancelled = AtomicBoolean()
    val connection = AtomicReference<HttpURLConnection?>()
    fun cancel() { cancelled.set(true); runCatching { connection.getAndSet(null)?.disconnect() } }
}

internal object SearchOnlineSuggestions {
    fun fetch(query: String, provider: Int, request: SearchSuggestionRequest): List<SearchSuggestion>? {
        if (request.cancelled.get()) return null
        val encoded = SearchTemplate.encode(query)
        val locale = LocaleList.getAdjustedDefault()[0] ?: Locale.getDefault()
        val language = when (locale.language) { "in" -> "id"; "iw" -> "he"; "ji" -> "yi"; "jw" -> "jv"; "tl" -> "fil"; else -> locale.language }
        val hl = if (language == "zh") if (locale.country in listOf("TW", "HK")) "zh-hant" else "zh" else language
        val endpoint = when (provider) {
            1 -> "https://clients1.google.com/complete/search?hl=${SearchTemplate.encode(hl)}&output=toolbar&q=$encoded"
            3 -> "https://api.bing.com/qsonhs.aspx?type=cb&cb=s&q=$encoded"
            else -> return null
        }
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        request.connection.set(connection)
        try {
            if (request.cancelled.get()) return null
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connectTimeout = 3000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                buildString { while (!request.cancelled.get()) append((reader.readLine() ?: break).trim()) }
            }
            if (request.cancelled.get()) return null
            val words = when (provider) {
                1 -> Regex("<suggestion data=\"(.*?)\"/>").findAll(body).take(5).map { Html.fromHtml(it.groupValues[1], 0).toString() }.toList()
                3 -> {
                    val start = body.indexOf(",\"Suggests\":[")
                    val end = if (start < 0) -1 else body.indexOf("]}]}}", start + 13)
                    if (start < 0 || end <= start + 13) return null
                    val list = JSONArray(body.substring(start + 12, end + 1))
                    (0 until minOf(list.length(), 5)).map { list.optJSONObject(it)?.optString("Txt").orEmpty() }
                }
                else -> return null
            }.filter(String::isNotEmpty)
            return words.takeIf { it.isNotEmpty() }?.map { SearchSuggestion(SearchSuggestionType.ONLINE, title = it, value = it) }
        } finally {
            request.connection.compareAndSet(connection, null)
            connection.disconnect()
        }
    }

}
