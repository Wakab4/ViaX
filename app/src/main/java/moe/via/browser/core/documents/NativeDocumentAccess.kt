package moe.via.browser.core.documents

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import moe.via.browser.core.logging.NativeLog

/** b1.i/j/k preserve file URIs, separate metadata queries and their original empty results. */
internal object NativeDocumentAccess {
    fun name(context: Context, uri: Uri): String? {
        if (uri.scheme.equals("file", true)) return uri.lastPathSegment
        var name: String? = null
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) name = it.getString(0)
            }
        } catch (error: Exception) { NativeLog.warn(null, error) }
        return name ?: uri.lastPathSegment
    }

    fun size(context: Context, uri: Uri): Long {
        if (uri.scheme.equals("file", true)) return uri.path?.let { File(it).length() } ?: 0
        var size = -1L
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use {
                if (it.moveToFirst()) size = it.getLong(0)
            }
        } catch (error: Exception) { NativeLog.warn(null, error) }
        return size
    }

    fun input(context: Context, uri: Uri): InputStream? = try {
        if (uri.scheme.equals("file", true)) FileInputStream(uri.path)
        else context.applicationContext.contentResolver.openInputStream(uri)
    } catch (error: Exception) { NativeLog.warn(null, error); null }
}
