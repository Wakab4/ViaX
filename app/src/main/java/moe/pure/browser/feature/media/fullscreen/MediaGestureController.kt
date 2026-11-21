package moe.pure.browser.feature.media.fullscreen

import android.app.Activity
import android.media.AudioManager
import android.provider.Settings
import moe.pure.browser.feature.media.model.MediaGestureHud
import moe.pure.browser.feature.media.model.MediaGestureKind
import java.util.Locale
import kotlin.math.abs

internal class MediaGestureController(private val activity: Activity) {
    private val audio = activity.getSystemService(AudioManager::class.java)
    private var initialVolume = 0
    private var initialBrightness = -1f
    private var delta = 0
    private var kind: MediaGestureKind? = null

    fun start(value: MediaGestureKind) {
        kind = value
        delta = 0
        initialVolume = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        initialBrightness = activity.window.attributes.screenBrightness.takeIf { it >= 0f }
            ?: systemBrightness()
    }

    fun move(value: MediaGestureKind, fraction: Float, duration: Float): MediaGestureHud? {
        if (kind != value || !fraction.isFinite()) return null
        val text = when (value) {
            MediaGestureKind.BRIGHTNESS -> {
                val brightness = (initialBrightness + fraction).coerceIn(.01f, 1f)
                activity.window.attributes = activity.window.attributes.apply { screenBrightness = brightness }
                "${(brightness * 100).toInt()}%"
            }
            MediaGestureKind.VOLUME -> {
                val max = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
                val volume = ((if (max > 0) initialVolume.toFloat() / max else 0f) + fraction).coerceIn(0f, 1f)
                runCatching { audio?.setStreamVolume(AudioManager.STREAM_MUSIC, (max * volume).toInt(), 0) }
                "${(volume * 100).toInt()}%"
            }
            MediaGestureKind.SEEK -> {
                delta = (fraction * minOf(duration, 300f)).toInt()
                String.format(Locale.ROOT, "%s%02d:%02d", if (delta >= 0) "+" else "-", abs(delta / 60), abs(delta % 60))
            }
        }
        return MediaGestureHud(text, value)
    }

    fun end(value: MediaGestureKind, cancelled: Boolean): Int? {
        if (kind != value) return null
        kind = null
        return delta.takeIf { value == MediaGestureKind.SEEK && !cancelled }.also { delta = 0 }
    }

    private fun systemBrightness(): Float {
        val value = runCatching { Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 10) }.getOrDefault(10)
        val minimum = runCatching {
            val id = activity.resources.getIdentifier("config_screenBrightnessSettingMinimum", "integer", "android")
            if (id != 0) activity.resources.getInteger(id) else 0
        }.getOrDefault(0)
        return ((value - minimum).toFloat() / (255 - minimum).coerceAtLeast(1)).coerceIn(.01f, 1f)
    }

    fun dispose() {
        kind = null
        delta = 0
        activity.window.attributes = activity.window.attributes.apply { screenBrightness = -1f }
    }
}
