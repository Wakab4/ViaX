package moe.pure.browser.feature.media.ui

import android.view.MotionEvent
import moe.pure.browser.feature.media.model.MediaGestureKind
import moe.pure.browser.feature.media.model.MediaOverlayState
import kotlin.math.abs

/** Observes dispatch without consuming the embedded player's controls. Distances are physical px. */
internal class MediaTouchDispatcher(
    private val actions: MediaActions,
    private val state: () -> MediaOverlayState,
    private val dp: (Int) -> Int,
    private val onTap: () -> Unit,
    private val onHudEnd: () -> Unit,
) {
    private var startX = 0
    private var startY = 0
    private var previousX = 0
    private var previousY = 0
    private var excluded = false
    private var kind: MediaGestureKind? = null

    fun observe(event: MotionEvent, width: Int, height: Int) {
        if (width <= 0 || height <= 0 || state().inPipMode) return
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x.toInt(); startY = event.y.toInt()
                previousX = startX; previousY = startY; kind = null
                val margin = dp(48)
                excluded = startY < margin || startY > height - margin * 3 / 2 ||
                    startX < margin / 2 || startX > width - margin
            }
            MotionEvent.ACTION_MOVE -> {
                if (excluded) return
                val x = event.x.toInt(); val y = event.y.toInt()
                val dx = x - previousX; val dy = y - previousY
                previousX = x; previousY = y
                val current = state()
                if (!current.locked && kind == null && (abs(dx) > 12 || abs(dy) > 12)) {
                    if (abs(dy) > abs(dx) * 1.4) {
                        kind = when {
                            startX < width / 3 && current.brightnessGestureEnabled -> MediaGestureKind.BRIGHTNESS
                            startX > width * 2 / 3 && current.volumeGestureEnabled -> MediaGestureKind.VOLUME
                            else -> null
                        }
                    } else if (abs(dx) <= abs(dy) * 3) {
                        excluded = true
                    } else if (current.progressGestureEnabled) {
                        kind = MediaGestureKind.SEEK
                    }
                    kind?.let(actions::onGestureStart)
                }
                kind?.let { gesture ->
                    val fraction = if (gesture == MediaGestureKind.SEEK) (x - startX).toFloat() / width
                    else (startY - y) * 2f / height
                    actions.onGestureMove(gesture, fraction)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val cancelled = event.actionMasked == MotionEvent.ACTION_CANCEL
                kind?.let { actions.onGestureEnd(it, cancelled); onHudEnd() }
                    ?: run { if (!cancelled && !excluded) onTap() }
                kind = null; excluded = false
            }
        }
    }
}
