package moe.pure.browser.feature.readaloud

data class ReadAloudTask(
    val id: String,
    val url: String,
    val title: String,
    val segments: List<String>,
    val segmentIndex: Int = 0,
    val playing: Boolean = false
) {
    val hasSegment: Boolean get() = segmentIndex in segments.indices
    val completed: Boolean get() = !hasSegment
}
