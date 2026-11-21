package moe.pure.browser.feature.home.web

import android.content.Context
import android.net.Uri
import java.io.File

/** Homepage identity uses the application's exact private document paths. */
internal object HomePageLocation {
    fun document(context: Context) = File(context.filesDir, "homepage2.html")
    fun stylesheet(context: Context) = File(context.filesDir, "homepage.css")
    fun url(context: Context): String = Uri.fromFile(document(context)).toString()

    fun alias(value: String?) = value.equals("about:home", true) || value.equals("about:links", true)

    fun generated(context: Context, uri: Uri): Boolean = local(uri) &&
        (uri.path == document(context).absolutePath || uri.path == File(context.filesDir, "homepage.html").absolutePath)

    fun isStylesheet(context: Context, uri: Uri) = local(uri) && uri.path == stylesheet(context).absolutePath
    fun local(uri: Uri) = uri.scheme == "file" && uri.authority.isNullOrEmpty()

    fun isHome(context: Context, value: String?): Boolean = alias(value) || value != null &&
        runCatching { generated(context, Uri.parse(value)) }.getOrDefault(false)

    fun normalize(context: Context, value: String): String {
        val prefix = if (value.startsWith("view-source:", true)) "view-source:" else ""
        val address = if (prefix.isEmpty()) value else value.substring(prefix.length)
        if (alias(address)) return prefix + url(context)
        if (!isHome(context, address)) return value
        val source = Uri.parse(address)
        return prefix + Uri.fromFile(document(context)).buildUpon()
            .encodedQuery(source.encodedQuery).encodedFragment(source.encodedFragment).build().toString()
    }
}
