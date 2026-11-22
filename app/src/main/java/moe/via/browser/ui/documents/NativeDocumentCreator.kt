package moe.via.browser.ui.documents

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import moe.via.browser.R
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.ui.components.BrowserMessage
import java.io.File

/** p8.d keeps one temporary callback; a missing document activity uses public Download. */
internal class NativeDocumentCreator(private val owner: Fragment, mime: String) {
    private var callback: ((Uri?, String?) -> Unit)? = null
    private var pending: (() -> Unit)? = null
    private val permission = if (Build.VERSION.SDK_INT <= 28) owner.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { pending?.invoke(); pending = null }
        else BrowserMessage(owner.requireActivity()).show(owner.getString(R.string.ref_scan_page_permission_denied), owner.getString(android.R.string.ok)) {
            owner.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${owner.requireContext().packageName}")))
        }
    } else null
    private val launcher = owner.registerForActivityResult(ActivityResultContracts.CreateDocument(mime)) { uri -> deliver(uri, null) }
    fun create(name: String, result: (Uri?, String?) -> Unit) {
        callback = result
        try { launcher.launch(name) } catch (error: ActivityNotFoundException) {
            NativeLog.warn(null, error)
            val action = { destination(name)?.let { deliver(it.first, it.second) }; Unit }
            pending = action
            val request = permission
            if (request == null) action() else request.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    private fun deliver(uri: Uri?, path: String?) { callback?.let { it(uri, path); callback = null } }
    @Suppress("DEPRECATION") private fun destination(name: String): Pair<Uri, String>? {
        val context = owner.requireContext()
        if (name.isEmpty()) { NativeLog.warn(null, "Can not create a file with null or empty path"); return null }
        val dot = name.lastIndexOf('.')
        val base = if (dot < 0) name else name.substring(0, dot)
        val suffix = if (dot < 0) "" else name.substring(dot)
        if (Build.VERSION.SDK_INT < 29) {
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!directory.exists() && !directory.mkdirs()) return null
            var file = File(directory, name); var index = 1
            while (file.exists()) file = File(directory, "$base (${index++})$suffix")
            return FileProvider.getUriForFile(context, "${context.packageName}.provider", file) to file.absolutePath
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val names = HashSet<String>()
        context.contentResolver.query(collection, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), "relative_path = ?", arrayOf(Environment.DIRECTORY_DOWNLOADS + "/"), null)?.use { cursor ->
            while (cursor.moveToNext()) names.add(cursor.getString(0))
        }
        var selected = name; var index = 1
        while (selected in names) selected = "$base (${index++})$suffix"
        val type = moe.via.browser.core.mime.NativeMimeTypes.mime(name.substringAfterLast('.', ""), "application/octet-stream") ?: "application/octet-stream"
        val uri = try { context.contentResolver.insert(collection, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, selected); put(MediaStore.MediaColumns.MIME_TYPE, type); put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }) } catch (error: Exception) { NativeLog.warn(null, error); null }
        return uri?.let { it to "/sdcard/${Environment.DIRECTORY_DOWNLOADS}/$selected" }
    }
}
