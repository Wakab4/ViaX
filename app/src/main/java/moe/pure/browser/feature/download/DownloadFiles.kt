package moe.pure.browser.feature.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

class DownloadFiles(private val context: Context) {
    data class Created(val uri: Uri, val name: String)
    fun create(name: String, mime: String): Created {
        val safe = safeName(name)
        val directory = context.getSharedPreferences("browser", 0).getString("download_directory", "").orEmpty()
        val uri = if (directory.startsWith("content://")) {
            val tree = Uri.parse(directory)
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            DocumentsContract.createDocument(context.contentResolver, parent, mime, safe) ?: error("无法创建下载文件")
        } else if (Build.VERSION.SDK_INT >= 29) {
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, safe); put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }) ?: error("无法创建下载文件")
        } else {
            @Suppress("DEPRECATION")
            val folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).apply { check(mkdirs() || isDirectory) }
            var file = File(folder, safe); var index = 1
            while (file.exists()) { file = File(folder, safe.substringBeforeLast('.', safe) + " (${index++})" + safe.substringAfterLast('.', "").let { if (it.isBlank()) "" else ".$it" }) }
            check(file.createNewFile()); Uri.fromFile(file)
        }
        return Created(uri, displayName(uri) ?: safe)
    }
    fun sink(target: String): DownloadSink {
        val uri = Uri.parse(target)
        return if (uri.scheme == "file") FileDownloadSink(File(uri.path ?: error("下载路径为空"))) else ContentDownloadSink(context, uri)
    }
    fun publish(task: DownloadTask) {
        val uri = Uri.parse(task.target)
        if (Build.VERSION.SDK_INT >= 29 && uri.authority == MediaStore.AUTHORITY) {
            context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } else if (uri.scheme == "file") android.media.MediaScannerConnection.scanFile(context, arrayOf(uri.path), arrayOf(task.mime), null)
    }
    fun prepare(task: DownloadTask) {
        val uri = Uri.parse(task.target)
        if (Build.VERSION.SDK_INT >= 29 && uri.authority == MediaStore.AUTHORITY) {
            context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 1) }, null, null)
        }
    }
    fun delete(task: DownloadTask) {
        val uri = Uri.parse(task.target)
        if (uri.scheme == "file") { val file = File(uri.path ?: return); check(!file.exists() || file.delete()) { "无法删除下载文件" } }
        else if (DocumentsContract.isDocumentUri(context, uri)) {
            try { check(DocumentsContract.deleteDocument(context.contentResolver, uri)) { "无法删除下载文件" } }
            catch (_: java.io.FileNotFoundException) { }
        } else context.contentResolver.delete(uri, null, null)
    }
    fun rename(task: DownloadTask, name: String): String {
        val uri = Uri.parse(task.target); val safe = safeName(name)
        if (uri.scheme == "file") {
            val file = File(uri.path ?: error("下载路径为空")); val renamed = File(file.parentFile, safe)
            check(!renamed.exists() && file.renameTo(renamed)) { "重命名失败，文件可能已存在" }
            return Uri.fromFile(renamed).toString()
        }
        if (DocumentsContract.isDocumentUri(context, uri)) return (DocumentsContract.renameDocument(context.contentResolver, uri, safe) ?: error("重命名失败")).toString()
        check(context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, safe) }, null, null) > 0)
        return task.target
    }
    fun shareUri(task: DownloadTask): Uri = if (Uri.parse(task.target).scheme == "file")
        Uri.parse("content://${context.packageName}.downloads/download/${task.id}") else Uri.parse(task.target)
    fun exists(task: DownloadTask): Boolean = runCatching {
        val uri = Uri.parse(task.target)
        if (uri.scheme == "file") File(uri.path ?: return false).isFile else context.contentResolver.openFileDescriptor(uri, "r")?.use { true } == true
    }.getOrDefault(false)
    fun name(target: String): String? = displayName(Uri.parse(target))
    private fun displayName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching { context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } }.getOrNull()
    }
    companion object {
        fun safeName(value: String) = value.replace(Regex("[\\\\/\\u0000-\\u001f]"), "_").trim().trim('.').take(220).ifBlank { "download" }
    }
}
