package moe.pure.browser.feature.media.model

enum class MediaGestureKind { BRIGHTNESS, VOLUME, SEEK }

data class MediaGestureHud(val text: String, val kind: MediaGestureKind)

/** Presentation values only; the WebView retains ownership of the video element. */
data class MediaOverlayState(
    val title: String = "",
    val toolbarEnabled: Boolean = true,
    val locked: Boolean = false,
    val speed: Float = 1f,
    /** Native videoorientation: 0 automatic, 1 landscape, 2 portrait, 3 system. */
    val orientation: Int = 0,
    val canEnterPip: Boolean = true,
    val orientationAdjustable: Boolean = true,
    val speedAdjustable: Boolean = false,
    val brightnessGestureEnabled: Boolean = false,
    val volumeGestureEnabled: Boolean = false,
    val progressGestureEnabled: Boolean = false,
    val inPipMode: Boolean = false,
    val gestureHud: MediaGestureHud? = null
)
