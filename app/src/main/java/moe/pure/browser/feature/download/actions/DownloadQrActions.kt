package moe.pure.browser.feature.download.actions

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference
import java.util.concurrent.Future
import moe.pure.browser.BrowserActivity
import moe.pure.browser.R
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.download.DownloadClipboard
import moe.pure.browser.feature.download.DownloadState
import moe.pure.browser.feature.download.DownloadTask
import moe.pure.browser.feature.scan.decode.QrImageDecoder
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.message.BrowserMessages

/** b1.J0/K0/r4/M3: per-page success/empty cache, only the latest decode may update the open menu. */
internal class DownloadQrActions(private val activity: FragmentActivity, private val dialogs: DownloadDialogOwner) : AutoCloseable {
    private val values = mutableMapOf<Long, String>()
    private val jobs = mutableListOf<Future<*>>()
    private val main = Handler(Looper.getMainLooper())
    private var sequence = 0L
    private var closed = false
    var decoded: ((Long) -> Unit)? = null
    fun result(id: Long) = values[id]
    fun probe(task: DownloadTask) {
        if (closed || values.containsKey(task.id) || task.state != DownloadState.COMPLETE ||
            !task.mime.startsWith("image/") || task.total > 204800) return
        val ticket = ++sequence
        val app = activity.applicationContext
        val reference = WeakReference(this)
        val dispatcher = main
        jobs += BrowserExecutors.network.submit {
            val bitmap = app.contentResolver.openInputStream(Uri.parse(task.target))?.use(BitmapFactory::decodeStream)
            val value = try { bitmap?.let(QrImageDecoder::decode).orEmpty() } finally { bitmap?.takeUnless { it.isRecycled }?.recycle() }
            dispatcher.post {
                reference.get()?.takeUnless { it.closed }?.let { owner ->
                    owner.values[task.id] = value
                    if (value.isNotEmpty() && ticket == owner.sequence) owner.decoded?.invoke(task.id)
                }
            }
        }
    }
    fun show(task: DownloadTask) {
        val text = values[task.id]?.takeIf(String::isNotEmpty) ?: return
        val web = text.startsWith("http://", true) || text.startsWith("https://", true)
        val dialog = BrowserDialog(activity, task.name, text).positive(activity.getString(android.R.string.ok))
        dialog.neutral(activity.getString(if (web) R.string.ref_download_action_open else R.string.ref_download_action_copy)) {
            if (web) activity.startActivity(Intent(activity, BrowserActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse(text))
                .putExtra("intent", "moe.pure.browser.BROWSER"))
            else DownloadClipboard.put(activity, text).onSuccess { BrowserMessages.show(activity, R.string.ref_download_action_copied) }
            true
        }
        dialogs.show(dialog)
    }
    override fun close() { closed = true; decoded = null; jobs.forEach { it.cancel(true) }; jobs.clear(); values.clear(); main.removeCallbacksAndMessages(null) }
}
