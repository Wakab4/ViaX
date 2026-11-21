package moe.pure.browser.feature.download

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract

class DownloadDirectories(private val context: Context,
    private val preferences: SharedPreferences = context.getSharedPreferences("browser", Context.MODE_PRIVATE)) {
    val tree: Uri? get() = preferences.getString("download_directory", "").orEmpty().takeIf { it.startsWith("content://") }?.let(Uri::parse)
    val isDefault get() = preferences.getString("download_directory", "").orEmpty().let { it.isEmpty() || it == Environment.DIRECTORY_DOWNLOADS }
    fun useDefault() { preferences.edit().remove("download_directory").apply() }
    fun select(uri: Uri) {
        val access = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, access)
        preferences.edit().putString("download_directory", uri.toString()).apply()
    }
    fun hasAccess(): Boolean = tree?.let(::hasAccess) ?: true
    fun hasAccess(uri: Uri): Boolean {
        if (uri.scheme != "content" || !uri.pathSegments.contains("tree")) return true
        val treeId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() ?: return false
        return context.contentResolver.persistedUriPermissions.any { grant ->
            grant.isReadPermission && grant.isWritePermission && grant.uri.authority == uri.authority &&
                runCatching { DocumentsContract.getTreeDocumentId(grant.uri) == treeId }.getOrDefault(false)
        }
    }
    fun summary(): String {
        val uri = tree ?: return "/sdcard/" + Environment.DIRECTORY_DOWNLOADS
        if (uri.authority == "com.android.externalstorage.documents") {
            val id = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrDefault("")
            if (id.startsWith("primary:")) return "/sdcard/" + id.removePrefix("primary:")
        }
        return runCatching {
            val document = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
            context.contentResolver.query(document, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull() ?: uri.toString()
    }
}
