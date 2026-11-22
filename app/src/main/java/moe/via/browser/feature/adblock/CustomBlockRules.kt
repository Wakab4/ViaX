package moe.via.browser.feature.adblock

import android.content.Context
import android.net.Uri
import java.util.Locale
import moe.via.browser.feature.adblock.rules.FilterUrls

/** Rules created by the log's block-domain / block-link actions, in Via's custom.txt format. */
class CustomBlockRules(context: Context) {
    private val app = context.applicationContext
    private val engine get() = AdblockEngine.get(app)
    fun text() = engine.customText()
    fun replace(text: String) = engine.replaceCustom(text)
    fun contains(url: String, domain: Boolean) = rule(url, domain)?.let(engine::containsCustom) ?: false
    fun addElement(url: String, selector: String): Boolean {
        val host = FilterUrls.host(url)?.takeIf(String::isNotEmpty) ?: return false
        return engine.addCustom("$host##$selector")
    }
    fun set(url: String, domain: Boolean, blocked: Boolean) {
        val value = rule(url, domain) ?: return
        if (blocked) engine.addCustom(value) else engine.removeCustom(value)
    }
    private fun rule(url: String, domain: Boolean): String? = if (domain) Uri.parse(url).host?.lowercase(Locale.ROOT)?.let { "||$it^" } else "|$url"
}
