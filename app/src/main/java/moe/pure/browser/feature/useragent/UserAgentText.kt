package moe.pure.browser.feature.useragent

import java.security.MessageDigest

/** Native editor normalization and b4.h reduction, kept out of both UI and database code. */
object UserAgentText {
    fun cleanName(value: String): String = buildString {
        value.forEach { character ->
            when {
                Character.isWhitespace(character) -> if (character != '\r') append(' ')
                character == '\u00a0' -> append(' ')
                else -> append(character)
            }
        }
    }.trim { it <= ' ' }

    fun cleanValue(value: String): String = value.replace(Regex("[\\t\\n\\r]+"), " ").trim { it <= ' ' }

    fun webViewDefault(value: String): String {
        val marker = value.indexOf("; wv")
        if (marker < 0) return value
        return StringBuilder(value).apply {
            delete(marker, marker + 4)
            val version = indexOf(" Version/")
            if (version >= 0) {
                val end = indexOf(" ", version + 1).takeIf { it >= 0 } ?: length
                if (end > version) delete(version, end)
            }
        }.toString()
    }

    fun fingerprint(name: String?, value: String?): String {
        val joined = (name ?: "__NULL__") + (value ?: "__NULL__")
        if (joined.isEmpty()) return ""
        return runCatching { MessageDigest.getInstance("MD5").digest(joined.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) } }
            .getOrElse { Integer.toHexString(joined.hashCode()) }
    }

    fun reduce(value: String): String {
        if (value.length < 8) return value
        val android = value.indexOf("Android").takeIf { it >= 0 } ?: return value
        val end = value.indexOf(") AppleWebKit", android).takeIf { it >= 0 } ?: return value
        return buildString {
            append(value, 0, android + 7); append(" 10; K")
            val chrome = value.indexOf("Chrome/", end)
            if (chrome < 0) append(value, end, value.length)
            else {
                val dot = value.indexOf('.', chrome + 7)
                append(value, end, dot + 1); append("0.0.0")
                val suffix = value.indexOf(' ', dot)
                if (suffix > 0) append(value, suffix, value.length)
            }
        }
    }
}
