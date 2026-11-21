package moe.pure.browser.feature.search.resolve

import moe.pure.browser.feature.search.model.ResolvedSearchInput
import java.util.Locale

object SearchUrlResolver {
    private val schemePattern = Regex("^[A-Za-z][A-Za-z0-9+.-]*$")
    private const val QUOTES = "''\"\"{}()[]<>「」『』【】〔〕〖〗〘〙〚〛（）［］｛｝＜＞〈〉《》‘’“”"

    fun resolve(input: String, searchTemplate: String, forceSearch: Boolean = false): ResolvedSearchInput? {
        val text = input.trim()
        if (text.isEmpty()) return null
        // s6 receives this address-input result before ordinary URL parsing; preserve its opaque tab ID.
        if (text.startsWith("VIA-SWITCH-TAB:")) return ResolvedSearchInput(text, false)
        if (!forceSearch && isAddress(text)) return ResolvedSearchInput(normalize(text), false)
        return ResolvedSearchInput(SearchTemplate.apply(searchTemplate, text), true, text)
    }

    fun normalize(input: String): String {
        val text = input.trim()
        val parsed = parse(text)
        if (!parsed.valid) return text
        val prefix = if (parsed.scheme == null) if (SearchHostPolicy.prefersHttp(normalizeDots(parsed.host.orEmpty()).lowercase(Locale.ROOT))) "http://" else "https://" else ""
        val host = parsed.host ?: return if (parsed.scheme == "javascript") "javascript:" + text.substringAfter(':') else text
        // Native encodes the explicit HTTP(S) host; bare addresses are handed to WebView after prefixing.
        if (!isHttp(text)) return prefix + text
        val normalized = normalizeDots(host)
        val encoded = if (normalized.startsWith('[')) normalized else runCatching { SearchPunycode.host(normalized) }.getOrDefault(normalized)
        return prefix + text.substring(0, parsed.hostStart) + encoded + text.substring(parsed.hostStart + host.length)
    }

    fun isHttp(text: String): Boolean = text.startsWith("http://", true) || text.startsWith("https://", true)
    fun isAddress(text: String): Boolean = parse(text.trim()).valid

    /** GP g0.e: paired delimiters and the whole-input/no-delimiter empty-list branch are intentional. */
    fun httpLinks(input: String): List<String> {
        if (input.length < 8) return emptyList()
        val text = input.trim()
        val output = mutableListOf<String>()
        var position = 0
        while (position < text.length) {
            val separator = text.indexOf("://", position)
            if (separator < 0) break
            val start = when {
                separator >= 5 && text.substring(separator - 5, separator).equals("https", true) -> separator - 5
                separator >= 4 && text.substring(separator - 4, separator).equals("http", true) -> separator - 4
                else -> { position = separator + 3; continue }
            }
            val previous = text.getOrNull(start - 1) ?: ' '
            val quote = QUOTES.indexOf(previous)
            val close = if (quote >= 0 && quote % 2 == 0) QUOTES[quote + 1] else ' '
            var end = separator + 3
            while (end < text.length && text[end] != close && !white(text[end])) end++
            if (end == text.length && start == 0) return emptyList()
            if (end > separator + 3) text.substring(start, end).takeIf(::isAddress)?.let(output::add)
            position = end + 1
        }
        return output
    }

    private data class Parsed(val valid: Boolean, val scheme: String? = null, val host: String? = null, val hostStart: Int = 0)
    private fun parse(text: String): Parsed {
        if (text.isEmpty()) return Parsed(false)
        var scheme: String? = null
        val colon = text.indexOf(':')
        if (colon > 0) {
            val candidate = text.substring(0, colon)
            // h0.t recursively refuses a scheme token that is itself a valid bare host.
            if (schemePattern.matches(candidate) && !bareHost(candidate, "")) scheme = candidate.lowercase(Locale.ROOT)
        }
        if (scheme != null && !text.regionMatches(colon + 1, "//", 0, 2)) {
            val value = text.substring(colon + 1)
            val valid = when (scheme) {
                "javascript", "data", "view-source" -> true
                "http", "https", "ftp" -> false
                "about" -> value.isEmpty() || value.all { it in 'A'..'Z' || it in 'a'..'z' || it == '-' }
                "magnet", "sms", "tel", "mailto", "geo", "tg" -> true
                else -> false
            }
            return Parsed(valid, scheme)
        }
        val start = if (scheme != null) colon + 3 else 0
        var limit = text.lastIndexOf('#').takeIf { it >= 0 } ?: text.length
        val queryAt = text.indexOf('?', start)
        if (queryAt in start..limit) limit = queryAt
        val pathAt = text.indexOf('/', start).takeIf { it in start..limit }
        val end = pathAt ?: limit
        val authority = text.substring(start, end)
        val user = authority.indexOf('@').takeIf { it >= 0 && start + it > 0 } ?: -1
        val hostStart = start + user + 1
        val hostAndPort = authority.substring(user + 1)
        val portColon = hostAndPort.lastIndexOf(':')
        val host = if (portColon > 0 && hostAndPort.substring(portColon + 1).all { it in '0'..'9' }) hostAndPort.substring(0, portColon) else hostAndPort
        val web = scheme in listOf("http", "https", "ftp")
        if (start >= text.length && web) return Parsed(false, scheme)
        val path = if (pathAt != null) text.substring(pathAt, limit) else ""
        if (scheme != null && !web) return Parsed(!text.any(::white), scheme, host, hostStart)
        val forbidden = if (web) "!#$&\"'()*+,/;<=>?@\\^`{|}~" else "!#$%&\"'()*+,/;<=>?@\\^_`{|}~"
        if (host.any { it in forbidden }) return Parsed(false, scheme)
        val firstWhite = text.indexOfFirst(::white)
        val valid = when {
            web -> firstWhite < 0 || firstWhite > hostStart
            scheme != null -> firstWhite < 0
            firstWhite > 0 && firstWhite < end -> false
            else -> bareHost(host, path)
        }
        return Parsed(valid, scheme, host, hostStart)
    }

    private fun bareHost(raw: String, path: String): Boolean {
        val host = normalizeDots(raw).lowercase(Locale.ROOT)
        if (host == "localhost") return true
        if (host.startsWith('[') && host.endsWith(']')) return host.length > 3 && host.substring(1, host.lastIndex).all { it in "0123456789abcdef:" }
        if (host.isEmpty() || host.startsWith('.') || host.endsWith('.') || host.contains("..")) return false
        if (host.any { white(it) || it in "!#$%&\"'()*+,/;<=>?@\\^_`{|}~" }) return false
        if (path == "/") return true
        val suffix = host.substringAfterLast('.', "")
        if (suffix.isEmpty()) return false
        if (suffix.all { it in '0'..'9' }) {
            val parts = host.split('.')
            return parts.size == 4 && host.length >= 7 && parts.all { it.isNotEmpty() && it.toIntOrNull()?.let { number -> number in 0..255 } == true }
        }
        return SearchHostPolicy.knownSuffix(suffix)
    }
    private fun normalizeDots(host: String) = host.replace('\u3002', '.').replace('\uff0e', '.').replace('\uff61', '.')
    private fun white(char: Char) = char == '\u00a0' || Character.isWhitespace(char) || Character.isSpaceChar(char)
}
