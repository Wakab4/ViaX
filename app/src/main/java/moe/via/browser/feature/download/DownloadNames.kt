package moe.via.browser.feature.download

import moe.via.browser.feature.download.http.NativeDownloadFilename
import moe.via.browser.core.mime.NativeMimeTypes
import java.util.Locale
import moe.via.browser.feature.download.model.DownloadDraft

object DownloadNames {
    fun guess(draft: DownloadDraft) = NativeDownloadFilename.guess(draft.url, draft.disposition, draft.mime)
    fun mime(name: String, fallback: String = "application/octet-stream"): String {
        val extension = name.substringAfterLast('.', "")
        return NativeMimeTypes.mime(extension, fallback) ?: fallback
    }
    fun size(bytes: Long): String {
        val value: Double
        val unit: String
        when {
            bytes >= 858993459.2 -> { value = bytes / (1024.0 * 1024 * 1024); unit = "GB" }
            bytes >= 838860.8 -> { value = bytes / (1024.0 * 1024); unit = "MB" }
            bytes >= 819.2 -> { value = bytes / 1024.0; unit = "KB" }
            else -> { value = bytes.toDouble(); unit = "B" }
        }
        return String.format(Locale.ROOT, "%.1f %s", value, unit)
    }
}
