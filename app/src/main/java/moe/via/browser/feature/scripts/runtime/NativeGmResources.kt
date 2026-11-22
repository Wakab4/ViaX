package moe.via.browser.feature.scripts.runtime

import android.util.Base64
import android.webkit.MimeTypeMap

/** p5.a.D/E use the executing script's resource mapping and the same cached/inline byte reader. */
internal class NativeGmResources(private val engine: UserScriptEngine) {
    fun read(identifier: String, name: String, asUrl: Boolean): String {
        val url = engine.execution(identifier)?.resources?.get(name) ?: return "undefined"
        val resource = engine.repository.resource(url) ?: return "undefined"
        if (resource.isEmpty()) return ""
        if (!asUrl) return resource.toString(Charsets.UTF_8)
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        val mime = if (extension.isNullOrEmpty()) null else MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return "data:${mime ?: "application/octet-stream"};base64," + Base64.encodeToString(resource, Base64.NO_WRAP)
    }
}
