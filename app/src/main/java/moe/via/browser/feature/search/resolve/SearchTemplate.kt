package moe.via.browser.feature.search.resolve

import java.net.URLDecoder
import java.net.URLEncoder

object SearchTemplate {
    fun encode(query: String): String = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
    fun apply(template: String, query: String): String {
        if (template.isEmpty() || query.isEmpty()) return query
        val index = listOf("%@", "%s", "%S").firstNotNullOfOrNull { marker -> template.indexOf(marker).takeIf { it >= 0 } } ?: -1
        if (index == 0 && template.length == 2) return query
        val encoded = encode(query.trim())
        return if (index < 0) template + encoded else template.substring(0, index) + encoded + template.substring(index + 2)
    }

    /** GP g0.g, including its DEX-confirmed host-prefix end-minus-one and placeholder priority. */
    fun extract(url: String, template: String): String? = extract(url, template, ::decode)

    internal fun extract(url: String, template: String, decode: (String) -> String): String? {
        if (url.isEmpty() || template.isEmpty()) return null
        val templateSlash = template.indexOf('/', 8)
        if (templateSlash > 0) {
            val actualSlash = url.indexOf('/', 8)
            if (actualSlash < 0) return null
            val samePrefix = actualSlash == templateSlash && url.startsWith(template.substring(0, templateSlash - 1))
            if (!samePrefix && listOf(".baidu.com", ".bing.com", "/www.google.com", ".sogou.com").none {
                    template.lastIndexOf(it, templateSlash) > 0 && url.lastIndexOf(it, actualSlash) > 0
                }) return null
            if (url.lastIndexOf(".baidu.com", actualSlash) > 0 && template.indexOf("/s?", templateSlash) > 0 && url.indexOf("/s?", actualSlash) < 0) return null
        }
        val slot = listOf("%s", "%S", "%@").firstNotNullOfOrNull { template.indexOf(it).takeIf { position -> position >= 0 } } ?: template.length
        if (slot == 0 && template.length == 2) return url
        var queryStart = template.indexOf('?')
        if (templateSlash < 0 && !url.startsWith(template.substring(0, if (queryStart > 0 && slot > queryStart) queryStart else slot))) return null
        if (queryStart < 0 || slot < queryStart) {
            val prefix = template.substring(0, slot)
            if (!url.startsWith(prefix)) return null
            val suffix = if (slot < template.length) template.substring(slot + 2) else ""
            val end = if (suffix.isNotEmpty()) url.indexOf(suffix, slot).takeIf { it >= 0 } ?: return null
                else url.indexOf('?', slot).takeIf { it >= 0 } ?: url.indexOf('#', slot).takeIf { it >= 0 } ?: url.length
            return decode(url.substring(slot, end))
        }
        queryStart = maxOf(queryStart, template.lastIndexOf('&', slot))
        val start = queryStart + 1
        if (start >= slot) return null
        var key = template.substring(start, slot)
        var value = parameterTail(url, key, 0)
        if (value == null && template.lastIndexOf(".baidu.com", templateSlash) > 0) {
            key = "wd="
            value = parameterTail(url, key, 0)
        }
        if (value == null) return null
        if (slot == template.length || slot == template.length - 2) return decode(value)
        val expected = parameterTail(template, key, queryStart) ?: return null
        if (value.length < expected.length - 2) return null
        if (expected.length == 2) return decode(value)
        val inner = listOf("%s", "%S", "%@").firstNotNullOfOrNull { expected.indexOf(it).takeIf { position -> position >= 0 } } ?: return null
        val end = value.indexOf(expected.substring(inner + 2))
        if (end < inner) return null
        return decode(value.substring(inner, end))
    }

    private fun parameterTail(url: String, needle: String, start: Int): String? {
        if (needle.isEmpty() || needle.length + start >= url.length) return null
        var offset = start
        while (offset < url.length) {
            val index = url.indexOf(needle, offset)
            if (index < 0) return null
            offset = index + needle.length
            if (index > 0 && url[index - 1] in "&?") {
                val end = listOf(url.indexOf('&', offset), url.indexOf('#', offset)).filter { it >= 0 }.minOrNull() ?: url.length
                return url.substring(offset, end)
            }
        }
        return null
    }
    private fun decode(value: String) = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
}
