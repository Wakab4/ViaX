package moe.pure.browser.feature.reader

import java.util.Locale

/** i6.i0.c/d/s and c0.s keep their raw URL decisions before the original state script. */
internal object ReaderProbeRules {
    fun network(url: String?) = url != null &&
        (url.startsWith("http://", true) || url.startsWith("https://", true))

    fun restricted(url: String): Boolean {
        if (url.isEmpty()) return true
        if (extension(url) == "js") return false
        return listOf(".baidu.com/" to "/s?", ".bing.com/" to "/search?", ".sogou.com/" to "/sl?",
            ".so.com/" to "/s?", ".google.com/" to "/search?", ".metaso.cn/" to "/search/")
            .any { (host, path) -> url.contains(host) && url.contains(path) } ||
            url.contains("://tool.lu/") || url.endsWith(".user.js")
    }

    private fun extension(url: String): String? {
        var end = url.length
        var start = -1
        for (index in url.indices.reversed()) {
            when (url[index]) {
                '#', '?' -> end = index
                '.' -> if (start == -1 || start > end) start = index + 1
                '/' -> {
                    if (start != -1 && start <= end &&
                        (index < 2 || url[index - 1] != '/' || url[index - 2] != ':')) break
                    return null
                }
            }
        }
        return if (start != -1 && start < end) url.substring(start, end).lowercase(Locale.ROOT) else null
    }

}
