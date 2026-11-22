package moe.via.browser.browser.ui.chrome.progress

/** In-memory animation state for a chrome tree replaced during the same Activity's configuration change. */
class NativeProgressState internal constructor(
    internal val progress: Int,
    internal val alpha: Float,
    internal val showing: Boolean,
    internal val motion: NativeProgressMotion?
)

internal data class NativeProgressMotion(
    val initial: Int,
    val target: Int,
    val towardTarget: Boolean,
    val playTime: Long,
    val capturedAt: Long
)
