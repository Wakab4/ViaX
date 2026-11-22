package moe.via.browser.browser.page.permissions

import android.app.Activity
import android.content.pm.PackageManager
import java.util.ArrayDeque

/** Serializes platform permission dialogs; WebView requests can arrive together. */
class AndroidPermissionQueue(private val activity: Activity) {
    private data class Request(val permissions: List<String>, val result: (Set<String>) -> Unit)
    private val queue = ArrayDeque<Request>()
    private var current: Request? = null
    private var disposed = false
    private var draining = false

    fun ask(permissions: List<String>, result: (Set<String>) -> Unit) {
        if (disposed) { result(emptySet()); return }
        queue.addLast(Request(permissions.distinct(), result))
        next()
    }
    fun onResult(requestCode: Int): Boolean {
        if (requestCode != REQUEST) return false
        val request = current ?: return true
        current = null
        request.result(granted(request.permissions))
        next()
        return true
    }
    private fun next() {
        if (draining) return
        draining = true
        try {
            while (!disposed && current == null && queue.isNotEmpty()) {
                val request = queue.removeFirst()
                val allowed = granted(request.permissions)
                val missing = request.permissions.filterNot { it in allowed }
                if (missing.isEmpty()) request.result(allowed)
                else if (activity.isFinishing || activity.isDestroyed) request.result(emptySet())
                else {
                    current = request
                    activity.requestPermissions(missing.toTypedArray(), REQUEST)
                }
            }
        } finally { draining = false }
    }
    private fun granted(permissions: List<String>) = permissions.filterTo(mutableSetOf()) {
        activity.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }
    fun dispose() {
        disposed = true
        val pending = listOfNotNull(current) + queue.toList()
        current = null; queue.clear()
        pending.forEach { it.result(emptySet()) }
    }
    companion object { private const val REQUEST = 405 }
}
