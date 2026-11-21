package moe.pure.browser.feature.library.format

import java.io.Reader
import java.util.Locale

/** z8.e1 reads one character at a time and retains its restricted attribute syntax. */
internal class BookmarkHtmlTokens(private val reader: Reader) {
    private val buffer = StringBuilder()
    private var type = 0
    var element: String = ""
        private set

    fun next(): Int {
        while (true) {
            val read = reader.read()
            if (read == -1) return END
            val character = read.toChar()
            if (character == '<' && complete()) {
                buffer.append(character)
                return type
            }
            buffer.append(character)
            if (character == '>' && complete()) return type
        }
    }

    fun attribute(name: String): String? {
        if (element.isEmpty() || name.isEmpty() || element.length < name.length || element[0] != '<') return null
        var index = element.indexOf(" ${name.uppercase(Locale.ROOT)}=\"")
        if (index < 0) index = element.indexOf(" ${name.lowercase(Locale.ROOT)}=\"")
        if (index <= 0 || element.length <= index + 1) return null
        val start = index + name.length + 3
        var end = element.indexOf("\" ", start)
        if (end < 0) end = element.indexOf("\">", start)
        return if (end > start) element.substring(start, end).replace("\\\"", "\"") else null
    }

    private fun complete(): Boolean {
        trimWhitespace()
        val length = buffer.length
        if (length == 0) return false
        if (buffer[0] == '<') {
            var index = 1
            var character = '<'
            while (index < length) {
                character = buffer[index]
                if (character != ':' && character != '!' && character != '-' && character != '/' &&
                    character !in 'a'..'z' && character !in 'A'..'Z' && character !in '0'..'9') break
                index++
            }
            if (character == ' ') character = buffer[length - 1]
            if (character != '>') return false
            var quotes = 0
            var quote = '-'
            while (index < length - 1) {
                val current = buffer[index]
                if (current == '\\') index++
                else if (current == '"' || current == '\'') {
                    if (quotes % 2 == 0) quote = current
                    if (current == quote) quotes++
                }
                index++
            }
            if (quotes % 2 != 0) return false
        }
        element = buffer.toString()
        type = if (element[0] == '<' && element.last() == '>') {
            if (element[1] == '/') CLOSE else OPEN
        } else TEXT
        buffer.setLength(0)
        return true
    }

    private fun trimWhitespace() {
        var start = 0
        var end = buffer.length
        while (start < end && Character.isWhitespace(buffer[start])) start++
        while (start < end && Character.isWhitespace(buffer[end - 1])) end--
        buffer.delete(end, buffer.length)
        buffer.delete(0, start)
    }

    companion object {
        const val END = 1
        const val OPEN = 2
        const val CLOSE = 3
        const val TEXT = 4
    }
}
