package moe.via.browser.feature.adblock.rules

data class ElementRule(val selector: String, val domains: List<String>? = null, val needsHas: Boolean = false) {
    companion object {
        private val unsupported = setOf(":has-text(", ":matches-attr(", ":matches-css(", ":matches-css-before(", ":matches-css-after(",
            ":matches-media(", ":matches-path(", ":min-text-length(", ":NOT(", ":others(", ":upward(", ":watch-attr(", ":xpath(",
            ":remove(", ":style(", ":remove-attr(", ":remove-class(")
        fun parse(raw: String): ElementRule? {
            val text = raw.trim()
            if (text.isEmpty() || text.startsWith('!') || "->m:" in text || "#?#" in text) return null
            var marker = text.indexOf("##")
            val exception = marker < 0 && text.indexOf("#@#").also { marker = it } >= 0
            if (marker < 0 || exception && marker == 0) return null
            val domains = if (marker == 0) null else text.substring(0, marker).split(',').filter(String::isNotEmpty).map {
                if (exception && !it.startsWith('~')) "~$it" else it
            }
            val selector = text.substring(marker + if (exception) 3 else 2)
            if (selector.isEmpty() || selector.startsWith('^') || "js(" in selector) return null
            val colon = selector.indexOf(':')
            val paren = selector.indexOf('(', (colon + 1).coerceAtLeast(0))
            val pseudo = if (colon >= 0 && paren > colon) selector.substring(colon, paren + 1) else null
            if (pseudo in unsupported || !validSelector(selector)) return null
            return ElementRule(selector, domains, pseudo == ":has(")
        }
        fun validSelector(value: String): Boolean {
            var quote: Char? = null; var brackets = 0; var parentheses = 0; var content = false; var index = 0
            while (index < value.length) {
                val char = value[index++]
                if (char == '\\') { if (index >= value.length) return false; index++; content = true; continue }
                if (quote != null) { if (char == quote) quote = null; content = true; continue }
                when (char) {
                    '\'', '"' -> quote = char
                    '{', '}', ';', '!', '&', '?' -> return false
                    '[' -> brackets++
                    ']' -> if (brackets-- == 0) return false
                    '(' -> parentheses++
                    ')' -> if (parentheses-- == 0) return false
                    '=' -> if (brackets == 0) return false
                    ',' -> if (brackets == 0 && parentheses == 0) { if (!content) return false; content = false; continue }
                }
                if (!char.isWhitespace()) content = true
            }
            return quote == null && brackets == 0 && parentheses == 0 && content
        }
    }
}
