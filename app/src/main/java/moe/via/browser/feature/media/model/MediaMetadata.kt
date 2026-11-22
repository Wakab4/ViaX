package moe.via.browser.feature.media.model

data class MediaMetadata(
    val duration: Float = 0f,
    val speed: Float = 1f,
    val width: Int = 16,
    val height: Int = 9,
    val available: Boolean = false
) {
    val finiteDuration: Boolean get() = duration > 0f && duration.isFinite()
}
