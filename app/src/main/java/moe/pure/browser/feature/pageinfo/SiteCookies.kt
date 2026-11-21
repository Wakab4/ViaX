package moe.pure.browser.feature.pageinfo

import android.net.Uri
import android.webkit.CookieManager
import androidx.webkit.CookieManagerCompat
import androidx.webkit.WebViewFeature
import java.util.concurrent.atomic.AtomicInteger

object SiteCookies {
    fun clear(url: String, completed: () -> Unit) {
        val manager = CookieManager.getInstance()
        val expired = if (WebViewFeature.isFeatureSupported(WebViewFeature.GET_COOKIE_INFO)) {
            CookieManagerCompat.getCookieInfo(manager, url).map { cookie ->
                val parts = cookie.split(';')
                val attributes = parts.drop(1).filterNot {
                    val name = it.trim().substringBefore('=')
                    name.equals("expires", true) || name.equals("max-age", true) ||
                        // Chromium reports host-only cookies with a non-dotted domain. Re-applying
                        // that attribute creates a domain cookie instead of expiring the original.
                        (name.equals("domain", true) && !it.substringAfter('=').trim().startsWith('.'))
                }
                parts.first().substringBefore('=').trim() + "=; Max-Age=0; " + attributes.joinToString(";")
            }
        } else fallback(url, manager.getCookie(url).orEmpty())
        if (expired.isEmpty()) { completed(); return }
        val remaining = AtomicInteger(expired.size)
        expired.forEach { manager.setCookie(url, it) { if (remaining.decrementAndGet() == 0) { manager.flush(); completed() } } }
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
