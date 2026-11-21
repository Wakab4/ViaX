package moe.pure.browser.web.upload

import android.content.Context
import android.content.Intent
import android.webkit.WebChromeClient
import moe.pure.browser.R

/** Preserve the provider's action, mode and capture behavior; only replace its Native MIME selection. */
internal object WebUploadIntent {
    fun chooser(context: Context, params: WebChromeClient.FileChooserParams): Intent {
        val picker = params.createIntent()
        val types = linkedSetOf<String>()
        params.acceptTypes?.forEach { value ->
            val candidate = value?.trim()?.takeIf(String::isNotEmpty) ?: return@forEach
            val mime = if (candidate.startsWith('.')) UploadMimeTypes.forExtension(candidate.substring(1)) else candidate
            if (mime != null) types.add(mime)
        }
        picker.type = if (types.size == 1) types.first() else "*/*"
        if (types.size > 1) picker.putExtra(Intent.EXTRA_MIME_TYPES, types.toTypedArray())
        val title = params.title?.takeIf { it.isNotEmpty() } ?: context.getString(R.string.ref_web_upload_title)
        return Intent.createChooser(picker, title)
    }
}
