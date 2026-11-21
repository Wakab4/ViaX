package moe.pure.browser.feature.download

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/** Clipboard service failures (including oversized inline links) must not close the browser. */
internal object DownloadClipboard {
    fun put(context: Context, text: String): Result<Unit> = runCatching {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, text))
        Unit
    }
}
