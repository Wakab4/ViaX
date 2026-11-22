package moe.via.browser.core.documents

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import moe.via.browser.core.files.NativeDirectories
import moe.via.browser.core.logging.NativeLog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.regex.Pattern

/** z8.b and c1.A/b/e copy the actual picker result to a fixed content filename. */
internal object NativeContentFiles {
    private val documentParts = Pattern.compile(":")
    fun copy(context: Context, uri: Uri?, prefix: String, defaultSuffix: String): String? {
        val directory = NativeDirectories.appFiles(context, "content") ?: return null
        if (uri == null || prefix.isEmpty()) return null
        val source = sourcePath(context, uri)
        if (source.isEmpty()) return null
        val dot = source.lastIndexOf('.')
        val suffix = if (dot >= 0) source.substring(dot) else defaultSuffix
        if (!directory.exists() && !directory.mkdirs()) return null
        if (directory.isFile && !directory.delete()) return null
        val target = File(directory, prefix + suffix)
        return copyTo(context, uri, target)
    }

    fun copyTo(context: Context, uri: Uri?, target: File?): String? {
        if (uri == null || uri.authority == null || target == null) return null
        var result: String? = null
        try {
            context.contentResolver.openInputStream(uri).use { input -> result = stream(input, target)?.path }
        } catch (error: IOException) { NativeLog.error(null, error) }
        return result
    }

    private fun sourcePath(context: Context, uri: Uri): String {
        val raw = uri.toString()
        if (raw.startsWith("file://")) return raw.substring(raw.indexOf("file://") + 7)
        val document = DocumentsContract.isDocumentUri(context, uri)
        var result: String? = ""
        try {
            val source = if (document) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else uri
            val args = if (document) arrayOf(documentParts.split(DocumentsContract.getDocumentId(uri))[1]) else null
            context.contentResolver.query(source, arrayOf("_data"), if (document) "_id=?" else null, args, null)?.use { cursor ->
                if (document) {
                    val column = cursor.getColumnIndexOrThrow("_data")
                    if (cursor.moveToFirst()) result = cursor.getString(column)
                } else {
                    cursor.moveToFirst()
                    result = cursor.getString(cursor.getColumnIndex("_data"))
                }
            }
        } catch (error: Exception) { if (document) result = ""; error.printStackTrace() }
        return result ?: ""
    }

    private fun stream(input: InputStream?, target: File): File? {
        if (input == null || !NativeDirectories.ensure(target.parentFile) || target.exists() && !target.delete()) return null
        var result: File? = null
        try {
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(8192)
                while (true) {
                    val size = input.read(buffer)
                    if (size == -1) break
                    output.write(buffer, 0, size)
                }
                output.flush()
                result = target
            }
        } catch (error: IOException) { NativeLog.error(null, error) }
        return result
    }
}
