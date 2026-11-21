package moe.pure.browser.feature.offline.save

import java.io.File

/** c1.n/L: Java trim, filename replacement and the original UTF-16/UTF-8 length limits. */
internal object SavedArchiveNames {
    fun clean(value: String): String {
        val name = value.trim { it <= ' ' }.replace(Regex("[/\\\\:*?\"<>|]"), "-").take(200)
        if (name.toByteArray(Charsets.UTF_8).size <= 200) return name
        var low = 0
        var high = name.length
        while (low < high) {
            val middle = (low + high + 1) / 2
            if (name.substring(0, middle).toByteArray(Charsets.UTF_8).size <= 200) low = middle else high = middle - 1
        }
        return name.substring(0, low)
    }

    fun unique(directory: File, name: String, extension: String): File {
        var candidate = File(directory, name + extension)
        if (directory.exists() || directory.mkdirs()) {
            var suffix = 0
            while (candidate.exists()) candidate = File(directory, "$name (${++suffix})$extension")
        }
        return candidate
    }
}
