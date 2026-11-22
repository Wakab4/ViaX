package moe.via.browser.storage.font

import java.io.File

/** z8.c1.n/K/L use Java trim, a hyphen replacement and a 200-byte UTF-8 boundary. */
internal object NativeFontNames {
    fun clean(raw: String): String {
        var value = raw.trim { it <= ' ' }.replace(Regex("[/\\\\:*?\"<>|]"), "-")
        if (value.length > 200) value = value.substring(0, 200)
        if (value.toByteArray(Charsets.UTF_8).size <= 200) return value
        var low = 0
        var high = value.length
        while (low < high) {
            val middle = (low + high + 1) / 2
            if (value.substring(0, middle).toByteArray(Charsets.UTF_8).size <= 200) low = middle else high = middle - 1
        }
        return value.substring(0, low)
    }
    fun available(directory: File, name: String): String {
        val dot = name.lastIndexOf('.')
        val stem = if (dot < 0) name else name.substring(0, dot)
        val suffix = if (dot < 0) "" else name.substring(dot)
        var candidate = stem + suffix
        if (directory.exists() || directory.mkdirs()) {
            var number = 0
            while (File(directory, candidate).exists()) { number++; candidate = "$stem ($number)$suffix" }
        }
        return candidate
    }
}
