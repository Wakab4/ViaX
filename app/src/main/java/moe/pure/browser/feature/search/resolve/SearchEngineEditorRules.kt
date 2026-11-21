package moe.pure.browser.feature.search.resolve

import java.util.Locale

object SearchEngineEditorRules {
    fun cleanTitle(raw: String): String = buildString {
        raw.forEach { char -> if (char != '\r') append(if (char == '\u00a0' || Character.isWhitespace(char)) ' ' else char) }
    }.trim { it <= ' ' }
    fun normalizeTemplate(raw: String): String = raw.trim { it <= ' ' }.let { if (it.isEmpty() || ':' in it) it else SearchUrlResolver.normalize(it) }
    fun isRecursive(template: String): Boolean = template.length >= 10 && template.lowercase(Locale.ROOT).let {
        it.contains("via.search") || it.contains("v://search") || it.contains("via://search")
    }
}
