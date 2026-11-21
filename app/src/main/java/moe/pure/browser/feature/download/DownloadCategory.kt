package moe.pure.browser.feature.download

import android.content.Context
import android.webkit.MimeTypeMap
import moe.pure.browser.R
import java.util.Locale

enum class DownloadCategory(val titleRes: Int, val icon: Int) {
    ALL(R.string.ref_download_ui_all, R.drawable.ref_ic_file_other),
    DOCUMENT(R.string.ref_download_ui_document, R.drawable.ref_ic_file_document),
    ARCHIVE(R.string.ref_download_ui_archive, R.drawable.ref_ic_file_archive),
    APK(R.string.ref_download_ui_apk, R.drawable.ref_ic_file_apk),
    IMAGE(R.string.ref_download_ui_image, R.drawable.ref_ic_file_image),
    VIDEO(R.string.ref_download_ui_video, R.drawable.ref_ic_file_video),
    AUDIO(R.string.ref_download_ui_audio, R.drawable.ref_ic_file_audio),
    OTHER(R.string.ref_download_ui_other, R.drawable.ref_ic_file_other);

    fun title(context: Context): String = context.getString(titleRes)
    companion object {
        private val documents = setOf("mht", "docx", "doc", "xml", "xul", "xls", "xlsx", "json", "jsonld", "epub", "js", "pdf", "ppt", "pptx", "ts", "azw", "rtf", "odp", "ods", "odt", "sh", "php", "py")
        private val archives = setOf("zip", "rar", "gz", "7z", "tar", "gtar", "bz", "bz2", "xz", "lzma", "z", "arj", "cab", "lzh", "iso", "jar", "ace", "tgz")
        fun of(task: DownloadTask): DownloadCategory {
            // b1.e4 only recognizes a suffix when the period is after the first character.
            val dot = task.name.lastIndexOf('.')
            val extension = if (dot > 0) task.name.substring(dot + 1).lowercase(Locale.ROOT) else ""
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).orEmpty()
            return when {
                mime.startsWith("audio/") -> AUDIO
                mime.startsWith("video/") -> VIDEO
                mime.startsWith("image/") -> IMAGE
                extension in setOf("apk", "xapk") -> APK
                mime.startsWith("text/") || extension in documents -> DOCUMENT
                extension in archives -> ARCHIVE
                else -> OTHER
            }
        }

        /** The row delegate classifies the recorded MIME, independently of the category filter. */
        fun iconForMime(mime: String): Int = when {
            mime.startsWith("audio/") -> AUDIO.icon
            mime.startsWith("video/") -> VIDEO.icon
            mime.startsWith("image/") -> IMAGE.icon
            mime == "application/vnd.android.package-archive" -> APK.icon
            mime.startsWith("text/") || mime.contains("document") || mime == "application/pdf" -> DOCUMENT.icon
            mime.contains("zip") || mime.contains("compress") -> ARCHIVE.icon
            else -> OTHER.icon
        }
    }
}
