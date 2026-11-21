package moe.pure.browser.feature.download

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/** Read grants for legacy file destinations; clients cannot write or enumerate downloads. */
class DownloadFileProvider : ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri) = task(uri)?.mime
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("只允许读取下载文件")
        val entry = task(uri) ?: throw FileNotFoundException()
        val target = Uri.parse(entry.target)
        if (target.scheme != "file") throw FileNotFoundException()
        return ParcelFileDescriptor.open(File(target.path ?: throw FileNotFoundException()), ParcelFileDescriptor.MODE_READ_ONLY)
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val entry = task(uri) ?: return null
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(columns).apply { addRow(columns.map { when (it) { OpenableColumns.DISPLAY_NAME -> entry.name; OpenableColumns.SIZE -> entry.received; else -> null } }) }
    }
    private fun task(uri: Uri): DownloadTask? {
        if (uri.pathSegments.size != 2 || uri.pathSegments[0] != "download") return null
        val id = uri.lastPathSegment?.toLongOrNull() ?: return null
        return DownloadRepository(context ?: return null).use { it.get(id) }
    }
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
}
