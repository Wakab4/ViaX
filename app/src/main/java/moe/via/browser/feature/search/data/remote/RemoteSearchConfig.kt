package moe.via.browser.feature.search.data.remote

import moe.via.browser.core.config.RemoteFeatureDocument

import android.content.Context
import android.os.Handler
import android.os.Looper
import moe.via.browser.core.concurrent.BrowserExecutors
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

/** One Shell-equivalent request owner. Construction does no preference, file or network work. */
class RemoteSearchConfig(context: Context, private val channel: String = "viayoo") : AutoCloseable {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var started = false
    @Volatile private var closed = false
    private var call: Call? = null

    fun start() {
        synchronized(this) {
            if (started || closed) return
            started = true
        }
        BrowserExecutors.persistence.execute {
            if (closed) return@execute
            val store = runCatching { RemoteSearchConfigStore(app, channel) }.getOrNull() ?: return@execute
            val cached = runCatching { store.load() }.getOrNull() ?: return@execute
            if (!cached.needsFetch()) deliver(cached.document)
            else fetch(store, cached)
        }
    }

    private fun fetch(store: RemoteSearchConfigStore, cached: RemoteSearchConfigStore.Cached) {
        val request = RemoteSearchHttp.call(cached.userId)
        synchronized(this) {
            if (closed) { request.cancel(); return }
            call = request
        }
        request.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                finished(call)
                deliver(cached.document)
            }

            override fun onResponse(call: Call, response: Response) {
                val result = RemoteSearchHttp.read(response)
                finished(call)
                if (result.status != 200) { deliver(cached.document); return }
                val raw = result.body.orEmpty()
                val value = RemoteFeatureDocument.parse(raw)
                BrowserExecutors.persistence.execute {
                    // A response already handed to persistence can outlive its window, as in Native.
                    if (runCatching { store.successful(raw) }.isSuccess) deliver(value)
                }
            }
        })
    }

    private fun finished(request: Call) { synchronized(this) { if (call === request) call = null } }

    private fun deliver(value: RemoteFeatureDocument) {
        if (closed) return
        main.post {
            if (!closed) RemoteSearchValues.activate(value)
        }
    }

    override fun close() {
        synchronized(this) {
            if (closed) return
            closed = true
            call?.cancel(); call = null
        }
        main.removeCallbacksAndMessages(null)
    }
}
