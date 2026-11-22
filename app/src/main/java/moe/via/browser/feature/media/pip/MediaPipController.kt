package moe.via.browser.feature.media.pip

import android.app.Activity
import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Process
import android.util.Rational
import android.view.View
import moe.via.browser.R
import kotlin.math.abs

/** Explicit PiP entry only. Playback actions are routed back to the owning WebView. */
internal class MediaPipController(
    private val activity: Activity,
    private val rewind: () -> Unit,
    private val toggle: () -> Unit,
    private val fastForward: () -> Unit
) {
    private val prefix = activity.packageName
    private val rewindAction = "$prefix.MEDIA_REWIND"
    private val playAction = "$prefix.MEDIA_PLAY"
    private val forwardAction = "$prefix.MEDIA_FASTFORWARD"
    private var registered = false
    private var ratio = Rational(16, 9)
    private var sourceRect: Rect? = null
    private var playingState = 2
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                rewindAction -> rewind()
                playAction -> toggle()
                forwardAction -> fastForward()
            }
        }
    }

    val supported: Boolean get() = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    fun attach() {
        if (!supported || registered) return
        val filter = IntentFilter().apply { addAction(rewindAction); addAction(playAction); addAction(forwardAction) }
        if (Build.VERSION.SDK_INT >= 33) activity.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else { @Suppress("UnspecifiedRegisterReceiverFlag") activity.registerReceiver(receiver, filter) }
        registered = true
        update()
    }

    fun permitted(): Boolean {
        if (!supported) return false
        val appOps = activity.getSystemService(AppOpsManager::class.java) ?: return false
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= 29) appOps.unsafeCheckOpNoThrow("android:picture_in_picture", Process.myUid(), prefix)
        else appOps.checkOpNoThrow("android:picture_in_picture", Process.myUid(), prefix)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun enter(): Boolean = supported && permitted() && runCatching { activity.enterPictureInPictureMode(params()) }.getOrDefault(false)

    fun dimensions(width: Int, height: Int) {
        val size = closestRatio(width, height)
        ratio = Rational(size.first, size.second)
        update()
    }

    fun position(view: View) {
        val rect = Rect()
        if (view.getGlobalVisibleRect(rect) && !rect.isEmpty) { sourceRect = rect; update() }
    }

    fun playing(value: Int) { playingState = value; update() }

    private fun update() {
        if (supported && registered) runCatching { activity.setPictureInPictureParams(params()) }
    }

    private fun params() = PictureInPictureParams.Builder().setAspectRatio(ratio).apply {
        sourceRect?.let(::setSourceRectHint)
        setActions(if (playingState <= 0) emptyList() else listOf(
            action(R.drawable.ref_media_rewind, "倒带", rewindAction, 0),
            if (playingState == 1) action(R.drawable.ref_media_play, "恢复", playAction, 1)
            else action(R.drawable.ref_media_pause, "暂停", playAction, 1),
            action(R.drawable.ref_media_fast_forward, "快进", forwardAction, 2)
        ))
        if (Build.VERSION.SDK_INT >= 31) setAutoEnterEnabled(false)
    }.build()

    private fun action(icon: Int, title: String, action: String, code: Int): RemoteAction {
        val intent = Intent(action).setPackage(prefix)
        val pending = PendingIntent.getBroadcast(activity, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return RemoteAction(Icon.createWithResource(activity, icon), title, title, pending)
    }

    fun dispose() {
        if (registered) {
            runCatching { activity.unregisterReceiver(receiver) }
            registered = false
            if (supported) runCatching { activity.setPictureInPictureParams(PictureInPictureParams.Builder().setActions(emptyList()).build()) }
        }
        sourceRect = null
    }

    companion object {
        private val RATIOS = listOf(4 to 3, 3 to 4, 16 to 9, 9 to 16, 1 to 1, 21 to 9, 2 to 1)
        internal fun closestRatio(width: Int, height: Int): Pair<Int, Int> {
            if (width <= 0 || height <= 0) return 16 to 9
            val ratio = width.toDouble() / height
            return RATIOS.minBy { abs(ratio - it.first.toDouble() / it.second) }
        }
    }
}
