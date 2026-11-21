package moe.pure.browser.feature.download.actions

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference
import java.util.concurrent.Future
import moe.pure.browser.R
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.download.DownloadClipboard
import moe.pure.browser.feature.download.DownloadFiles
import moe.pure.browser.feature.download.DownloadPayloads
import moe.pure.browser.feature.download.DownloadTask
import moe.pure.browser.ui.message.BrowserMessages

/** b1.b4/v4: share a local stream first, then text; inline links are reconstructed from the real payload. */
internal class DownloadFileSharing(private val activity: FragmentActivity) : AutoCloseable {
    private enum class LinkAction { COPY, SHARE }
    private val files = DownloadFiles(activity)
    private val main = Handler(Looper.getMainLooper())
    private val work = mutableListOf<Future<*>>()
    private var closed = false
    fun copy(task: DownloadTask) = withLink(task, LinkAction.COPY)
    fun share(task: DownloadTask) {
        val target = Uri.parse(task.target)
        if (target.scheme != "file" || files.exists(task)) {
            val stream = Intent(Intent.ACTION_SEND).setType(task.mime).putExtra(Intent.EXTRA_STREAM, files.shareUri(task))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (launch(stream)) return
        }
        withLink(task, LinkAction.SHARE)
    }
    private fun launch(intent: Intent) = runCatching {
        activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.ref_download_ui_share))); true
    }.getOrDefault(false)
    private fun withLink(task: DownloadTask, action: LinkAction) {
        if (closed || task.url.isEmpty()) return
        val token = task.payload
        if (token == null) { link(task.url, action); return }
        val app = activity.applicationContext
        val reference = WeakReference(this)
        val dispatcher = main
        work += BrowserExecutors.network.submit {
            val result = runCatching { DownloadPayloads.get(app).dataUrl(token) }
            dispatcher.post { reference.get()?.takeUnless { it.closed }?.let { owner -> result.onSuccess { owner.link(it, action) }
                .onFailure { moe.pure.browser.core.logging.NativeLog.warn("DownloadPayload", "Cannot reconstruct inline link", it) } } }
        }
    }
    private fun link(value: String, action: LinkAction) {
        when (action) {
            LinkAction.COPY -> DownloadClipboard.put(activity, value).onSuccess { BrowserMessages.show(activity, R.string.ref_download_action_copied) }
                .onFailure { moe.pure.browser.core.logging.NativeLog.warn("DownloadClipboard", "Cannot copy link", it) }
            LinkAction.SHARE -> { launch(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, value)) }
        }
    }
    override fun close() { closed = true; work.forEach { it.cancel(true) }; work.clear(); main.removeCallbacksAndMessages(null) }
}
