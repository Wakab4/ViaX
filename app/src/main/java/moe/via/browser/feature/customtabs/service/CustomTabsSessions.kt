package moe.via.browser.feature.customtabs.service

import android.app.PendingIntent
import android.os.IBinder
import android.os.RemoteException
import java.util.NoSuchElementException

/** Native session support registers callback death; it does not preload a page or emit callbacks. */
internal class CustomTabsSessions : AutoCloseable {
    private data class Session(val death: IBinder.DeathRecipient, val id: PendingIntent?)
    private val sessions = mutableMapOf<IBinder, Session>()
    private var closed = false
    fun register(callback: IBinder?, sessionId: PendingIntent?): Boolean {
        if (callback == null) return false
        val death = IBinder.DeathRecipient { remove(callback) }
        synchronized(sessions) {
            if (closed) return false
            return try {
                callback.linkToDeath(death, 0)
                sessions.put(callback, Session(death, sessionId))?.let { previous -> unlink(callback, previous.death) }
                true
            } catch (_: RemoteException) { false }
        }
    }
    private fun remove(callback: IBinder) {
        synchronized(sessions) { sessions.remove(callback)?.let { unlink(callback, it.death) } }
    }
    private fun unlink(callback: IBinder, death: IBinder.DeathRecipient) {
        try { callback.unlinkToDeath(death, 0) } catch (_: NoSuchElementException) { }
    }
    override fun close() {
        synchronized(sessions) {
            closed = true
            sessions.forEach { (callback, session) -> unlink(callback, session.death) }
            sessions.clear()
        }
    }
}
