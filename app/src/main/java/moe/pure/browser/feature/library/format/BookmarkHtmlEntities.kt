package moe.pure.browser.feature.library.format

import java.util.regex.Pattern

/** z8.p.f..j deliberately use different conversions for titles and href attributes. */
internal object BookmarkHtmlEntities {
    private val decimal = Pattern.compile("&#(\\d+);")

    fun names(value: String?): String? = if (value == null || value.length < 4 || '&' !in value) value
        else value.replace("&apos;", "'").replace("&quot;", "\"").replace("&gt;", ">")
            .replace("&lt;", "<").replace("&amp;", "&")

    fun decimals(value: String?): String? {
        if (value.isNullOrEmpty() || !value.contains("&#")) return value
        val matcher = decimal.matcher(value)
        val result = StringBuffer()
        while (matcher.find()) matcher.appendReplacement(result, matcher.group(1)!!.toInt().toChar().toString())
        matcher.appendTail(result)
        return result.toString()
    }

    fun href(value: String?): String? = if (value == null || value.length < 4 || '&' !in value) value
        else value.replace("&quot;", "\"")

    fun title(value: String?): String? = if (value == null || value.length < 2) value
        else value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;")

    fun attribute(value: String?): String? = if (value == null || value.length < 2) value
        else value.replace("\"", "&quot;")
}
