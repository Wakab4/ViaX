package moe.via.browser.feature.backup.ui

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

/** System document flows first, then Native's public Downloads fallback when no creator is available. */
internal class BackupDocuments(
    private val activity: Activity,
    private val state: BackupDocumentState,
    private val exportTo: (Uri, String?) -> Unit,
    private val importFrom: (Uri) -> Unit,
    private val cancelled: () -> Unit,
    private val error: () -> Unit,
    private val launchDocument: (Intent, Int) -> Unit,
    private val askPermissions: (Array<String>, Int) -> Unit
) {
    fun export(name: String) {
        state.pendingName = name
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/zip")
            .putExtra(Intent.EXTRA_TITLE, name)
        try { launchDocument(intent, EXPORT) }
        catch (_: Exception) { fallback() }
    }
    fun chooseImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "application/zip"))
            .putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.providers.downloads.documents/document/downloads"))
        try { launchDocument(intent, IMPORT) }
        catch (_: Exception) { error() }
    }
    fun onActivityResult(request: Int, result: Int, data: Intent?): Boolean {
        if (request != EXPORT && request != IMPORT) return false
        val uri = data?.data
        if (result == Activity.RESULT_OK && uri != null) { if (request == EXPORT) exportTo(uri, null) else importFrom(uri) }
        else cancelled()
        return true
    }
    private fun fallback() {
        if (Build.VERSION.SDK_INT <= 28 && activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            askPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_PERMISSION); return
        }
        val name = state.pendingName ?: return
        if (Build.VERSION.SDK_INT >= 29) {
            // The row is created only because the user explicitly requested this export.
            runCatching {
                val uri = activity.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name); put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS); put(MediaStore.Downloads.IS_PENDING, 1)
                }) ?: throw IllegalStateException("Unable to create export")
                state.pendingDownloads += uri; exportTo(uri, "${Environment.DIRECTORY_DOWNLOADS}/$name")
            }.onFailure { error() }
        } else runCatching {
            @Suppress("DEPRECATION")
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).canonicalFile
            check(directory.isDirectory || directory.mkdirs())
            var file = File(directory, name).canonicalFile; var index = 1
            while (file.exists()) { check(index <= 1000); file = File(directory, "${name.removeSuffix(".zip")} (${index++}).zip").canonicalFile }
            check(file.parentFile == directory)
            exportTo(Uri.fromFile(file), file.path)
        }.onFailure { error() }
    }
    fun onRequestPermissionsResult(request: Int, results: IntArray): Boolean {
        if (request != WRITE_PERMISSION) return false
        if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) fallback() else error()
        return true
    }
    companion object { private const val EXPORT = 6410; private const val IMPORT = 6411; private const val WRITE_PERMISSION = 6412 }
}
