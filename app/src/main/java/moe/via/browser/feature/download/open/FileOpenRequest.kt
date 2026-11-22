package moe.via.browser.feature.download.open

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle

/** Raw preview path and the grantable URI used for external applications are separate values. */
internal data class FileOpenRequest(val path: String, val uri: String, val mime: String?) {
    fun save() = Bundle().apply { putString("file_path", path); putString("file_uri", uri); putString("mime_type", mime) }
    companion object {
        fun read(bundle: Bundle) = bundle.getString("file_path")?.let {
            FileOpenRequest(it, bundle.getString("file_uri") ?: it, bundle.getString("mime_type"))
        }
    }
}

internal data class FileOpenChoice(val label: CharSequence, val icon: Drawable?, val kind: Int, val intent: Intent?) {
    // q1.a performs all arithmetic as a signed Int before widening to Long.
    val id: Long = (((label.hashCode() * 31) + kind) * 31 + (intent?.component?.flattenToShortString()?.hashCode() ?: 0)).toLong()
}
