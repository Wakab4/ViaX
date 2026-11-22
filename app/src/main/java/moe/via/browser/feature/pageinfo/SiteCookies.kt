package moe.via.browser.feature.pageinfo

import android.net.Uri
import android.webkit.CookieManager
import androidx.webkit.CookieManagerCompat
import androidx.webkit.WebViewFeature
import java.util.concurrent.atomic.AtomicInteger

object SiteCookies {
    internal data class Entry(
        val name: String,
        val domain: String,
        val path: String,
        val partitioned: Boolean,
        val expiry: String,
    )

    internal val supportsDetails: Boolean
        get() = WebViewFeature.isFeatureSupported(WebViewFeature.GET_COOKIE_INFO)

    internal fun names(url: String): List<String> = CookieManager.getInstance().getCookie(url).orEmpty()
        .split(';').filter { '=' in it }.map { it.substringBefore('=').trim() }

    internal fun entries(url: String): List<Entry> {
        val host = Uri.parse(url).host.orEmpty()
        return CookieManagerCompat.getCookieInfo(CookieManager.getInstance(), url).mapNotNull { raw ->
            val parts = raw.split(';')
            if ('=' !in parts.first()) return@mapNotNull null
            val attributes = parts.drop(1).map(String::trim)
            val name = parts.first().substringBefore('=').trim()
            Entry(
                name,
                attributes.firstOrNull { it.substringBefore('=').equals("domain", true) }
                    ?.substringAfter('=').orEmpty().ifEmpty { host },
                attributes.firstOrNull { it.substringBefore('=').equals("path", true) }
                    ?.substringAfter('=').orEmpty().ifEmpty { "/" },
                attributes.any { it.equals("partitioned", true) },
                expiry(name, attributes),
            )
        }
    }

    internal fun delete(url: String, entries: List<Entry>, completed: () -> Unit) {
        expire(url, entries.map(Entry::expiry), completed)
    }

    fun clear(url: String, completed: () -> Unit) {
        val manager = CookieManager.getInstance()
        val expired = if (supportsDetails) entries(url).map(Entry::expiry)
        else fallback(url, manager.getCookie(url).orEmpty())
        expire(url, expired, completed)
    }

    private fun expiry(name: String, attributes: List<String>): String {
        val preserved = attributes.filterNot {
            val key = it.substringBefore('=')
            key.equals("expires", true) || key.equals("max-age", true) ||
                // WebView reports a non-dotted domain for host-only cookies.
                (key.equals("domain", true) && !it.substringAfter('=').trim().startsWith('.'))
        }
        return (listOf("$name=", "Max-Age=0") + preserved).joinToString("; ")
    }

    private fun expire(url: String, expired: List<String>, completed: () -> Unit) {
        if (expired.isEmpty()) { completed(); return }
        val manager = CookieManager.getInstance()
        val remaining = AtomicInteger(expired.size)
        expired.forEach { header ->
            manager.setCookie(url, header) {
                if (remaining.decrementAndGet() == 0) {
                    manager.flush()
                    completed()
                }
            }
        }
    }
    private fun fallback(url: String, raw: String): List<String> {
        val uri = Uri.parse(url)
        val host = uri.host ?: return emptyList()
        val paths = mutableSetOf("/")
        var path = uri.path.orEmpty()
        while (path.contains('/')) { paths.add(path.ifEmpty { "/" }); path = path.substringBeforeLast('/') }
        val domains = mutableListOf("", host, ".$host")
        var domain = host.substringAfter('.', "")
        while (domain.contains('.')) { domains += ".$domain"; domain = domain.substringAfter('.') }
        val secure = if (uri.scheme == "https") "; Secure" else ""
        return raw.split(';').map { it.substringBefore('=').trim() }.filter(String::isNotEmpty).flatMap { name ->
            paths.flatMap { cookiePath -> domains.map { cookieDomain ->
                "$name=; Max-Age=0; Path=$cookiePath$secure" + if (cookieDomain.isEmpty()) "" else "; Domain=$cookieDomain"
            } }
        }
    }
}
