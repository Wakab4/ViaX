package moe.via.browser.feature.adblock.rules

data class NetworkRule(val pattern: String?, val flags: Int = 0, val domains: List<String>? = null) {
    val exception get() = flags and 1 != 0
    fun text(): String = buildString {
        if (exception) append("@@")
        pattern?.let { if (it.startsWith('|') || it.startsWith("http://") || it.startsWith("https://")) append('|'); append(it) }
        if (flags and -2 != 0 || !domains.isNullOrEmpty()) append('$')
        val types = flags and ALL_TYPES
        val inverse = Integer.bitCount(types) > 5
        for ((name, bit) in SERIAL_TYPES) {
            if (inverse && types and bit == 0) append("~$name,")
            else if (!inverse && types and bit != 0) append("$name,")
        }
        if (flags and 2 != 0) append("third-party,")
        if (flags and 4 != 0) append("~third-party,")
        domains?.let { append("domain="); append(it.joinToString("|")) }
    }
    fun matches(url: String, pageHost: String?, types: Int, thirdParty: Boolean): Boolean {
        val acceptedTypes = flags and ALL_TYPES
        if (acceptedTypes != 0 && acceptedTypes and types == 0) return false
        if (flags and 6 != 0 && ((flags and 2 == 0) == thirdParty)) return false
        if (!domains.isNullOrEmpty()) {
            if (!pageHost.isNullOrEmpty()) {
                var accepted = false
                for (domain in domains) {
                    if (domain.isEmpty()) continue
                    if (!domain.startsWith('~')) { if (pageHost.contains(domain)) { accepted = true; break } }
                    else { if (pageHost.contains(domain.substring(1))) return false; accepted = true }
                }
                if (!accepted) return false
            } else if (!domains.first().startsWith('~')) return false
        }
        return pattern.isNullOrEmpty() || FilterPattern.matches(pattern, url)
    }
    fun fastDomain(): String? {
        val value = pattern ?: return null
        if (flags != 0 || domains != null || value.length < 3 || !value.startsWith('|') || !value.endsWith('^')) return null
        val host = value.substring(1, value.length - 1)
        return host.takeIf { '.' in it && !it.startsWith('.') && !it.endsWith('.') && ".." !in it && it.all { char -> char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char in ".-" } }
    }
    companion object {
        private val SERIAL_TYPES = listOf("script" to 32, "other" to 16, "image" to 64, "stylesheet" to 128, "subdocument" to 256,
            "document" to 512, "media" to 1024, "font" to 2048, "popup" to 4096, "websocket" to 8192, "xmlhttprequest" to 16384)
        const val ALL_TYPES = 32752
        val TYPES = mapOf("other" to 16, "dtd" to 16, "xbl" to 16, "script" to 32, "image" to 64, "background" to 64,
            "stylesheet" to 128, "subdocument" to 256, "document" to 512, "media" to 1024, "font" to 2048,
            "popup" to 4096, "websocket" to 8192, "xmlhttprequest" to 16384)
        fun parse(raw: String): NetworkRule? {
            val text = raw.trim()
            if (text.length < 5 || text.startsWith('!') || '\\' in text) return null
            val space = text.indexOf(' ')
            if (space > 0) return if (text.startsWith("127.0.0.1")) NetworkRule("|${text.substring(space + 1).trim()}^") else null
            var start = if (text.startsWith("@@")) 2 else 0
            var flags = if (start == 2) 1 else 0
            val options = text.indexOf('$', start)
            var end = if (options < 0) text.length else options
            if (end - start > 1) {
                if (text[start] == '|') start += if (text.getOrNull(start + 1) == '|' && text.getOrNull(start + 2) == '*') 3 else 1
                else if (text[start] == '*') start++
                if (text[end - 1] == '*') end--
            }
            val pattern = text.substring(start, end).takeIf(String::isNotEmpty)
            var domains: List<String>? = null
            var types = 0
            var unsupported = false
            var position = if (options < 0) text.length else options + 1
            while (position < text.length) {
                val next = text.indexOf(',', position).takeIf { it >= 0 } ?: text.length
                val negated = text[position] == '~'
                val optionStart = position + if (negated) 1 else 0
                val option = text.substring(optionStart, next)
                val bit = TYPES[option]
                when {
                    bit != null -> {
                        if (negated) { if (types and ALL_TYPES == 0) types = ALL_TYPES; types = types and bit.inv() }
                        else types = types or bit
                    }
                    option == "third-party" -> flags = flags or if (negated) 4 else 2
                    option.startsWith("domain=") -> domains = option.substring(7).split('|').filter(String::isNotEmpty)
                    else -> { if (text.indexOf('=', optionStart) > 0) return null; unsupported = true }
                }
                position = next + 1
            }
            if (unsupported && types == 0) return null
            return NetworkRule(pattern, flags or types, domains)
        }
    }
}
