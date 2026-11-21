package moe.pure.browser.feature.backup.ui

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.util.concurrent.ConcurrentHashMap

/** App-owned transfer metadata survives the window that launched the document/permission request. */
internal class BackupDocumentState(context: Context) {
    private val app = context.applicationContext
    var pendingName: String? = null
    val pendingDownloads = ConcurrentHashMap.newKeySet<Uri>()
    fun finish(uri: Uri, success: Boolean) {
        if (!pendingDownloads.remove(uri) || Build.VERSION.SDK_INT < 29) return
        runCatching {
            if (success) app.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            else app.contentResolver.delete(uri, null, null)
        }
    }
    fun discard() { pendingDownloads.toList().forEach { finish(it, false) }; pendingName = null }
}
