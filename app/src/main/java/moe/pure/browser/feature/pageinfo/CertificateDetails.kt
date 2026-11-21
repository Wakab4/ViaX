package moe.pure.browser.feature.pageinfo

import android.net.http.SslCertificate
import android.os.Build
import android.text.Html
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.DateFormat

object CertificateDetails {
    fun text(certificate: SslCertificate?): CharSequence {
        if (certificate == null) return "该网站无证书。"
        val date = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT)
        fun value(text: String?) = Html.escapeHtml(text?.takeIf(String::isNotBlank) ?: " - ")
        val to = certificate.issuedTo
        val by = certificate.issuedBy
        val html = buildString {
            append("<b>颁发对象</b><br><b>公用名 (CN)</b><br>${value(to.cName)}<br>")
            append("<b>组织 (O)</b><br>${value(to.oName)}<br><b>组织单位 (OU)</b><br>${value(to.uName)}<br><br>")
            append("<b>颁发者</b><br><b>公用名 (CN)</b><br>${value(by.cName)}<br><b>组织 (O)</b><br>${value(by.oName)}<br><br>")
            append("<b>有效期</b><br><b>颁发日期</b><br>${value(date.format(certificate.validNotBeforeDate))}<br>")
            append("<b>截止日期</b><br>${value(date.format(certificate.validNotAfterDate))}<br>")
            val x509 = x509(certificate)
            if (x509 != null) {
                append("<br><b>SHA-256 指纹</b><br><b>证书</b><br>${fingerprint(x509.encoded)}<br>")
                append("<b>公钥</b><br>${fingerprint(x509.publicKey.encoded)}<br>")
            }
        }
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    }
    private fun x509(certificate: SslCertificate): X509Certificate? = runCatching {
        if (Build.VERSION.SDK_INT >= 29) certificate.x509Certificate
        else SslCertificate.saveState(certificate).getByteArray("x509-certificate")?.let {
            CertificateFactory.getInstance("X.509").generateCertificate(it.inputStream()) as X509Certificate
        }
    }.getOrNull()
    private fun fingerprint(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString(":") { "%02X".format(it.toInt() and 255) }
}
