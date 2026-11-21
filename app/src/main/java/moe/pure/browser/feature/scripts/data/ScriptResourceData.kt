package moe.pure.browser.feature.scripts.data

import android.content.Context
import moe.pure.browser.feature.scripts.model.ScriptResourceInfo

/** Native resources are bytes in files or inline URIs; MIME is derived by the GM URL caller. */
internal class ScriptResourceData(context: Context) {
    private val files = ScriptResourceFileStore(context)
    fun path(url: String): String? = if (ScriptResourceFileStore.http(url)) files.file(url)!!.absolutePath else null
    fun info(url: String): ScriptResourceInfo {
        if (!ScriptResourceFileStore.http(url)) return ScriptResourceInfo.Inline(url)
        val file = files.file(url) ?: return ScriptResourceInfo.Missing(url)
        if (!file.exists()) return ScriptResourceInfo.Missing(url)
        return ScriptResourceInfo.Cached(url, file, file.length(), file.lastModified())
    }
    fun read(url: String): ByteArray? {
        val inline = ScriptResourceText.inlineBytes(url)
        val value = if (inline != null && inline.isNotEmpty()) inline else ScriptResourceText.bytes(files.file(url))
        return value?.let { ScriptResourceIntegrity.bytes(url, it) }
    }
    fun text(url: String): String? {
        val inline = ScriptResourceText.inlineText(url)
        val value = inline ?: files.file(url)?.let { ScriptResourceText.file(it, trim = false) }
        return ScriptResourceIntegrity.text(url, value)
    }
    fun timestamp(url: String): Long? = (info(url) as? ScriptResourceInfo.Cached)?.takeIf { it.length > 0 }?.modified
    fun fetch(url: String): ScriptResourceFetch = files.fetch(url)
    fun prune(retained: Set<String>): Boolean = files.prune(retained)
}
