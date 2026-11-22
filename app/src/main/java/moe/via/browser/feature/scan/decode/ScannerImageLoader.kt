package moe.via.browser.feature.scan.decode

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import moe.via.browser.ui.tasks.NativeViewJobs

/** fb.q uses a view-bound Maybe over the complete selected bitmap, with WARN errors. */
internal class ScannerImageLoader(context: Context) : AutoCloseable {
    private val app = context.applicationContext
    private val jobs = NativeViewJobs()
    fun decode(uri: Uri, callback: (String?) -> Unit) {
        jobs.launch({
            val bitmap = BitmapFactory.decodeStream(app.contentResolver.openInputStream(uri))
            if (bitmap == null) false to null else {
                val result = QrImageDecoder.decode(bitmap)
                if (!bitmap.isRecycled) bitmap.recycle()
                true to result
            }
        }) { (present, text) -> if (present) callback(text) }
    }
    override fun close() = jobs.close()
}
