package moe.pure.browser.browser

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import moe.pure.browser.core.network.BrowserNetworkState

/** ab.b/c + s6.X8/O5: the browser window observes available/lost until destruction, not only resume. */
internal class BrowserNetworkObserver(context: Context, private val changed: (Int, Boolean) -> Unit) : AutoCloseable {
    private val app = context.applicationContext
    private val manager = app.getSystemService(ConnectivityManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var registered = false
    private var closed = false
    private var nonMetered = true
    private val deliver = Runnable {
        if (!closed && registered) {
            val type = BrowserNetworkState.type(app)
            val next = type != 2
            val different = next != nonMetered
            nonMetered = next; changed(type, different)
        }
    }
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { main.post(deliver) }
        override fun onLost(network: Network) { main.post(deliver) }
    }
    fun start() {
        if (closed || registered || manager == null) return
        nonMetered = BrowserNetworkState.type(app) != 2
        manager.registerDefaultNetworkCallback(callback); registered = true
    }
    override fun close() {
        closed = true; main.removeCallbacks(deliver)
        if (registered) { registered = false; runCatching { manager?.unregisterNetworkCallback(callback) } }
    }
}
