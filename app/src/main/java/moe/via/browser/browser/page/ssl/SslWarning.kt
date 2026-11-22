package moe.via.browser.browser.page.ssl

import android.net.http.SslError

/** Native preference bits are deliberately independent of Android's primaryError ordinals. */
internal object SslWarning {
    private val errors = listOf(
        Triple(SslError.SSL_UNTRUSTED, 1, "证书不受信任"),
        Triple(SslError.SSL_DATE_INVALID, 2, "证书日期无效"),
        Triple(SslError.SSL_EXPIRED, 4, "证书已过期"),
        Triple(SslError.SSL_IDMISMATCH, 8, "证书域名与网站域名不匹配"),
        Triple(SslError.SSL_NOTYETVALID, 16, "证书尚未生效"),
        Triple(SslError.SSL_INVALID, 32, "证书无效")
    )

    fun mask(error: SslError): Int = errors.fold(0) { mask, (type, flag, _) -> if (error.hasError(type)) mask or flag else mask }
    fun lines(error: SslError): String = buildString {
        errors.forEach { (type, _, text) -> if (error.hasError(type)) append("- ").append(text).append('\n') }
    }

    /** GP i0.f uses its own authority slice and preserves host casing. */
    fun host(url: String?): String {
        val text = url.orEmpty()
        val scheme = text.indexOf("://")
        if (scheme < 0) return ""
        val start = scheme + 3
        val end = text.indexOf('/', start).takeIf { it >= 0 } ?: text.length
        return text.substring(start, end).substringBefore(':')
    }
}
