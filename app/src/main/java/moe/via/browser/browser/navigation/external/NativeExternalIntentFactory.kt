package moe.via.browser.browser.navigation.external

import android.content.Intent
import android.net.MailTo
import android.net.Uri
import java.net.URISyntaxException

/** Native s6.J9 and z8.f1.c/g; this only constructs a destination, never queries or launches it. */
internal object NativeExternalIntentFactory {
    fun ignored(url: String): Boolean = url.startsWith("baidubox://") ||
        url.startsWith("baiduboxapp://") || url.startsWith("baiduboxlite://")

    fun create(url: String): Intent? = when {
        url.startsWith("mailto:") -> email(url)
        url.startsWith("tel:") || url.startsWith("sms:") -> Intent(Intent.ACTION_VIEW, Uri.parse(url))
        url.startsWith("magnet:") || url.startsWith("intent://") -> parsed(url)
        url.startsWith("tg:") -> parsed(if (url.startsWith("tg://")) url else "tg://" + url.substring(3))
        url.contains("://") -> if (webOrFile(url) || url.startsWith("ftp://") || url.startsWith("view-source:")) null else parsed(url)
        url.indexOf(':') <= 0 || url.startsWith("data:") || url.startsWith("javascript:") || url.startsWith("about:") -> null
        else -> parsed(url)
    }

    private fun webOrFile(url: String): Boolean = url.startsWith("http://", true) ||
        url.startsWith("https://", true) || url.startsWith("file://", true)

    private fun parsed(url: String): Intent? = try {
        Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
    } catch (_: URISyntaxException) { null }

    private fun email(url: String): Intent? = runCatching {
        val mail = MailTo.parse(url)
        Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", mail.to.orEmpty(), null)).apply {
            mail.to?.takeIf(String::isNotEmpty)?.let { putExtra(Intent.EXTRA_EMAIL, it.split(Regex("\\s*,\\s*")).toTypedArray()) }
            mail.body?.takeIf(String::isNotEmpty)?.let { putExtra(Intent.EXTRA_TEXT, it) }
            mail.subject?.takeIf(String::isNotEmpty)?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            mail.cc?.takeIf(String::isNotEmpty)?.let { putExtra(Intent.EXTRA_CC, it.split(Regex("\\s*,\\s*")).toTypedArray()) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }.getOrNull()
}
