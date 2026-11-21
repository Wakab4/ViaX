package moe.pure.browser.feature.passwords.storage

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File

/** p8.d / z8.b1: public Download is used only when the system CREATE_DOCUMENT activity is absent. */
internal class PasswordCsvDownloads(context: Context) {
    data class Created(val uri: Uri, val displayPath: String)
    private val context = context.applicationContext

    /** The caller runs this and the CSV writer on the serialized persistence worker. */
    fun create(name: String): Created {
        require(name.isNotEmpty() && name.none { it == '/' || it == '\\' || it == '\u0000' })
        return if (Build.VERSION.SDK_INT >= 29) createMediaStore(name) else createLegacy(name)
    }

    @TargetApi(29)
    private fun createMediaStore(name: String): Created {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val selected = uniqueMediaStoreName(collection, name)
        val uri = context.contentResolver.insert(collection, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, selected)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }) ?: error("Unable to create CSV destination")
        val actualName = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment.orEmpty()
        // c1.q deliberately displays /sdcard on scoped-storage Android, rather than a guessed _data path.
        return Created(uri, "/sdcard/${Environment.DIRECTORY_DOWNLOADS}/$actualName")
    }

    @TargetApi(29)
    private fun uniqueMediaStoreName(collection: Uri, name: String): String {
        val dot = name.lastIndexOf('.')
        val base = name.substring(0, dot)
        val extension = name.substring(dot + 1)
        val escaped = base.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val existing = HashSet<String>()
        runCatching {
            context.contentResolver.query(collection, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                "relative_path = ? AND _display_name LIKE ? ESCAPE '\\'",
                arrayOf("${Environment.DIRECTORY_DOWNLOADS}/", "$escaped%"), null)?.use { cursor ->
                while (cursor.moveToNext()) cursor.getString(0)?.let(existing::add)
            }
        }
        var candidate = name
        var index = 1
        while (candidate in existing) candidate = "$base (${index++}).$extension"
        return candidate
    }

    @Suppress("DEPRECATION")
    private fun createLegacy(name: String): Created {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).canonicalFile
        check(directory.isDirectory || directory.mkdirs())
        val dot = name.lastIndexOf('.')
        val base = if (dot < 0) name else name.substring(0, dot)
        val suffix = if (dot < 0) "" else name.substring(dot)
        var file = File(directory, name).canonicalFile
        var index = 1
        while (file.exists()) file = File(directory, "$base (${index++})$suffix").canonicalFile
        check(file.parentFile == directory)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val path = "${Environment.getExternalStorageDirectory().absolutePath}/${Environment.DIRECTORY_DOWNLOADS}/${file.name}"
        return Created(uri, path)
    }

    companion object { const val MIME = "text/csv" }
}
