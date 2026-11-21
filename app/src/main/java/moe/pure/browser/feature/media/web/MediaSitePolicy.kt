package moe.pure.browser.feature.media.web

/** GP b0.R/S/X/Y match the page authority, including substring behavior. */
internal data class MediaSitePolicy(val seekAndSpeed: Boolean, val volumeAndBrightness: Boolean, val toolbar: Boolean) {
    companion object {
        fun from(url: String?): MediaSitePolicy {
            val input = url.orEmpty()
            val start = input.indexOf("://")
            val authority = if (start < 0) "" else input.substring(start + 3, input.indexOf('/', start + 3).takeIf { it >= 0 } ?: input.length)
            // GP i0.f preserves case and truncates at the first colon.
            val host = authority.substringBefore(':')
            return MediaSitePolicy(
                seekAndSpeed = listOf("youku.com", "youtube.com", "v.qq.com", "film.qq.com", "mgtv.com", "iqiyi.com", "douyin.com").none(host::contains),
                volumeAndBrightness = listOf("youku.com", "youtube.com", "douyin.com", "gaze.run").none(host::contains),
                toolbar = !host.contains("youtube.com")
            )
        }
    }
}
