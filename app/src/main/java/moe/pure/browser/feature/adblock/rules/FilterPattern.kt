package moe.pure.browser.feature.adblock.rules

/** Via's wildcard matcher treats | as a slash/dot boundary and ^ as an ASCII separator. */
object FilterPattern {
    private fun separator(char: Char) = char !in 'a'..'z' && char !in 'A'..'Z' && char !in '0'..'9' && char !in "_-.%"
    fun matches(content: String, url: String): Boolean {
        if (content.none { it in "*^|" }) return url.contains(content)
        val pattern = (if (content.startsWith('*')) "" else "*") + content + if (content.endsWith('*')) "" else "*"
        var input = 0; var token = 0; var star = -1; var retry = -1
        while (input < url.length) {
            val char = pattern.getOrNull(token)
            when {
                char != null && (char == url[input] || char == '^' && separator(url[input]) || char == '|' && url[input] in "/.") -> { input++; token++ }
                char == '*' -> { star = token++; retry = input }
                star >= 0 -> { token = star + 1; input = ++retry }
                else -> return false
            }
        }
        while (pattern.getOrNull(token) == '*') token++
        return token == pattern.length
    }
    fun tokens(content: String?): Sequence<String> = sequence {
        if (content == null) return@sequence
        var start = content.indexOf("://").let { if (it < 0) 0 else it + 3 }
        val end = content.indexOf('$', start).takeIf { it >= 0 } ?: content.length
        while (start <= end - 5) {
            var offset = 0
            while (offset < 5 && content[start + offset] !in "@|*^") offset++
            if (offset < 5) start += offset + 1 else { yield(content.substring(start, start + 5)); start++ }
        }
    }
}
