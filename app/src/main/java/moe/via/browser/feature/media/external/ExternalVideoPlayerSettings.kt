package moe.via.browser.feature.media.external

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.media.data.MediaPreferences
import moe.via.browser.ui.dialog.BrowserDialog

/** Merely queries installed video handlers. The reference sample URL is never loaded. */
class ExternalVideoPlayerSettings(
    private val activity: Activity,
    private val read: () -> String = { MediaPreferences(activity).externalPlayer },
    private val write: (String) -> Unit = { MediaPreferences(activity).externalPlayer = it }
) {
    private data class Player(val packageName: String, val label: String)
    private val handler = Handler(Looper.getMainLooper())
    private var dialog: BrowserDialog? = null
    private var generation = 0

    fun show(onChanged: (String) -> Unit = {}) {
        val request = ++generation
        val app = activity.applicationContext
        val selected = read()
        BrowserExecutors.persistence.execute {
            val players = runCatching {
                val manager = app.packageManager
                val result = mutableListOf(Player("", "系统分享"))
                val intent = ExternalVideoPlayer.intent("https://file-examples.com/wp-content/uploads/2017/04/file_example_MP4_480_1_5MG.mp4")
                @Suppress("DEPRECATION")
                val handlers = manager.queryIntentActivities(intent, 0)
                handlers.forEach { info ->
                    val component = info.activityInfo
                    if (component.enabled) result += Player(component.packageName, component.loadLabel(manager).toString())
                }
                result
            }.getOrDefault(listOf(Player("", "系统分享")))
            handler.post {
                if (generation != request || activity.isFinishing || activity.isDestroyed) return@post
                dialog?.dismiss()
                dialog = BrowserDialog(activity, "外置视频播放器").singleChoice(players.map { it.label }, players.indexOfFirst { it.packageName == selected }.coerceAtLeast(0)) { index ->
                    val player = players[index]
                    write(player.packageName)
                    onChanged(player.label)
                }.also { it.show() }
            }
        }
    }

    fun label(): String {
        val selected = read()
        if (selected.isEmpty()) return "系统分享"
        return try {
            @Suppress("DEPRECATION")
            activity.packageManager.getApplicationInfo(selected, 0).loadLabel(activity.packageManager).toString()
        } catch (_: PackageManager.NameNotFoundException) { "系统分享" }
    }

    fun dispose() {
        generation++
        dialog?.dismiss()
        dialog = null
    }
}
