package moe.via.browser.feature.download.open

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import moe.via.browser.R
import moe.via.browser.core.mime.NativeMimeTypes
import java.io.File

/** w1.g/b: worker-only existence/MIME/package resolution, preserving all returned activities. */
internal object FileOpenQuery {
    @Suppress("DEPRECATION")
    fun load(context: Context, request: FileOpenRequest, previewLabel: String): List<FileOpenChoice> {
        val raw = uri(request.path)
        val exists = if (raw.scheme == "file") File(raw.path.orEmpty()).isFile
            else if (raw.scheme == "content") runCatching { context.contentResolver.openFileDescriptor(raw, "r")?.use { true } == true }.getOrDefault(false)
            else false
        if (!exists) return emptyList()
        val name = name(context, raw)
        val mime = request.mime?.takeIf { it.isNotEmpty() } ?: mime(context, raw, name)
        val result = mutableListOf<FileOpenChoice>()
        if (mime == "application/pdf") result += FileOpenChoice(previewLabel, context.getDrawable(R.drawable.ref_pdf_preview), 2, null)
        val base = Intent(if (mime == "application/vnd.android.package-archive") Intent.ACTION_INSTALL_PACKAGE else Intent.ACTION_VIEW)
            .setDataAndType(uri(request.uri), mime).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val packages = context.packageManager
        val apps = packages.queryIntentActivities(base, 0).sortedWith { first, second ->
            (first.loadLabel(packages) ?: "").toString().compareTo((second.loadLabel(packages) ?: "").toString(), ignoreCase = true)
        }
        apps.forEach { info ->
            info.activityInfo?.let { activity ->
                result += FileOpenChoice(info.loadLabel(packages) ?: "", info.loadIcon(packages), 1,
                    Intent(base).setComponent(ComponentName(activity.packageName, activity.name)))
            }
        }
        return result
    }
    fun uri(path: String): Uri = Uri.parse(path).let { if (it.scheme == "file" || it.scheme == "content") it else Uri.fromFile(File(path)) }
    private fun name(context: Context, uri: Uri): String? = if (uri.scheme == "file") uri.lastPathSegment else runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull()
    private fun mime(context: Context, uri: Uri, name: String?): String {
        val dot = name?.lastIndexOf('.') ?: -1
        val extension = if (name != null && dot > 0 && dot < name.lastIndex) NativeMimeTypes.mime(name.substring(dot + 1), null) else null
        return extension ?: (if (uri.scheme == "content") context.contentResolver.getType(uri) else null) ?: "application/octet-stream"
    }
}
