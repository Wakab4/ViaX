package moe.via.browser.core.config

import java.util.Locale

/** Reader exclusions supplied by the active remote feature document. */
internal class NativeSiteExclusions(document: RemoteFeatureDocument) {
    private val reading = parse(document.getString("wld", ""))

    fun reader(url: String?) = contains(reading, authority(url))

    private fun parse(raw: String?): Array<String>? {
        if (raw.isNullOrEmpty()) return null
        val values = HashSet<String>()
        var start = 0
        while (start < raw.length) {
            val end = raw.indexOf(',', start).takeIf { it >= 0 } ?: raw.length
            values.add(raw.substring(start, end).trim { it <= ' ' }.lowercase(Locale.ROOT))
            start = end + 1
        }
        return values.toTypedArray()
    }

    private fun authority(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if ('/' !in raw) return raw
        val scheme = raw.indexOf("://")
        if (scheme < 0) return null
        return raw.substring(scheme + 3).substringBefore('/').lowercase(Locale.ROOT).takeIf(String::isNotEmpty)
    }

    private fun contains(entries: Array<String>?, host: String?): Boolean {
        if (entries.isNullOrEmpty() || host.isNullOrEmpty()) return false
        return entries.any { entry ->
            val index = host.indexOf(entry)
            index >= 0 && (index == 0 || host[index - 1] == '.') &&
                (index + entry.length >= host.length || host[index + entry.length] == '.')
        }
    }
}
