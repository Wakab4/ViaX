package moe.via.browser.feature.appearance

/** Native d8.g is process memory only and shares one entry between pages with the same authority. */
internal object PageColorCache {
    private val entries = mutableMapOf<String, Int>()

    fun color(url: String): Int? = key(url).takeIf(String::isNotEmpty)?.let { entries[it] }
    fun contains(url: String): Boolean = key(url).let { it.isNotEmpty() && entries.containsKey(it) }

    /** Native zero is a shared pending marker; a late sample still replaces its captured URL key. */
    fun put(url: String, color: Int) { key(url).takeIf(String::isNotEmpty)?.let { entries[it] = color } }
    fun invalidate(url: String) { entries.remove(key(url)) }

    private fun key(url: String): String {
        if (url.length < 3) return ""
        val scheme = url.indexOf("://")
        if (scheme <= 0) return ""
        if (scheme > 12 && url.take(12).equals("view-source:", true)) return "S"
        val end = if (scheme == 4 && url.take(4).equals("file", true)) {
            url.lastIndexOf('?').takeIf { it >= 0 } ?: url.length
        } else {
            val start = scheme + 3
            val delimiter = url.indexOf('/', start).takeIf { it >= 0 }
                ?: url.indexOf('?', start).takeIf { it >= 0 } ?: url.length
            minOf(delimiter, scheme + 128)
        }
        return url.substring(scheme + 3, end)
    }
}
