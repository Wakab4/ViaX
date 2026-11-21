package moe.pure.browser.feature.network

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.util.concurrent.ConcurrentHashMap

/** u9.c.h writes each snapshot to one of two actual owned files, without a dirty cache gate. */
object NetworkLogPages {
    private val sources = ConcurrentHashMap<String, String>()
    fun isPage(context: Context, url: String): Boolean = fileName(context, url) != null
    fun sourceId(context: Context, url: String) = fileName(context, url)?.let { sources[it] }
    fun url(context: Context, resources: Boolean): String =
        "file://${File(context.filesDir, if (resources) "res.html" else "log.html").path}"

    /** Synchronous worker entry; the caller has already captured window text and request values. */
    fun open(context: Context, sourceId: String, requests: List<PageRequest>, resources: Boolean,
        options: NetworkLogDocumentOptions): String {
        val name = if (resources) "res.html" else "log.html"
        sources[name] = sourceId
        val file = File(context.filesDir, name)
        try {
            FileWriter(file).use { output ->
                BufferedWriter(output).use { writer ->
                    NetworkLogDocument.writeTo(writer, requests, resources, options)
                }
            }
        } catch (_: Exception) { }
        return "file://${file.path}"
    }

    @Suppress("UNUSED_PARAMETER")
    fun response(context: Context, uri: Uri, night: Boolean): WebResourceResponse? {
        val name = fileName(context, uri.toString()) ?: return null
        return try {
            WebResourceResponse("text/html", "UTF-8", FileInputStream(File(context.filesDir, name)))
        } catch (_: Exception) { null }
    }

    private fun fileName(context: Context, url: String): String? {
        val uri = Uri.parse(url.removePrefix("view-source:"))
        if (uri.scheme != "file" || !uri.authority.isNullOrEmpty()) return null
        return runCatching {
            val file = File(uri.path ?: return null).canonicalFile
            if (file.parentFile == context.filesDir.canonicalFile && file.name in setOf("log.html", "res.html")) file.name else null
        }.getOrNull()
    }
}
