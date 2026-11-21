package moe.pure.browser.feature.adblock

import android.content.Context
import android.webkit.WebResourceRequest
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.feature.adblock.data.NativeCustomFilterSource
import moe.pure.browser.feature.adblock.data.NativeFilterRuleSources
import moe.pure.browser.feature.adblock.rules.CustomRuleFiles
import moe.pure.browser.feature.adblock.rules.FilterRuleSet
import moe.pure.browser.feature.site.SiteDecision
import moe.pure.browser.feature.site.SiteFeature
import moe.pure.browser.feature.site.SiteSettingsRepository

/** x4.a gates matching while rebuilding the original mutable indexes. */
class AdblockEngine private constructor(private val context: Context) {
    private val settings = BrowserSettings(context)
    private val sites = SiteSettingsRepository(context)
    private val custom = NativeCustomFilterSource(CustomRuleFiles.file(context))
    private val rules = FilterRuleSet(android.webkit.WebView.getCurrentWebViewPackage()?.versionName?.substringBefore('.')?.toIntOrNull() ?: 0)
    private var builtin = settings.preferences.builtinFilters
    @Volatile private var busy = false

    fun reload() {
        busy = true
        rules.begin()
        rules.load(custom.load())
        if (builtin) rules.load(NativeFilterRuleSources.builtin(context))
        FilterSubscriptions(context).use { subscriptions ->
            subscriptions.texts().forEach { rules.load(NativeFilterRuleSources.file(it)) }
        }
        rules.finish()
        busy = false
    }
    fun useBuiltin(enabled: Boolean) {
        if (builtin == enabled) return
        builtin = enabled
        reload()
    }
    internal fun customText() = custom.text()
    internal fun containsCustom(raw: String) = custom.contains(raw)
    internal fun replaceCustom(raw: String): Boolean = custom.replace(raw).also { if (it) reload() }
    internal fun addCustom(raw: String): Boolean {
        if (busy) return false
        val rule = custom.add(raw) ?: return false
        rules.add(rule)
        return true
    }
    internal fun removeCustom(raw: String): Boolean {
        if (busy) return false
        val rule = custom.remove(raw) ?: return false
        rules.remove(rule)
        return true
    }
    fun enabled(url: String) = sites.resolve(url, SiteFeature.ADBLOCK, settings) == SiteDecision.ALLOW
    fun blocked(request: WebResourceRequest, page: String): Boolean {
        if (request.url.scheme == "file" || page.startsWith("file://")) return false
        if (busy || !enabled(page) || request.url.toString().isEmpty()) return false
        val rule = rules.match(request.url.toString(), page, FilterRequestTypes.of(request, page))
        val blocked = rule != null && !rule.exception
        if (blocked) BrowserExecutors.persistence.execute { AdblockStatistics.record(settings.values, request.url.toString()) }
        return blocked
    }
    fun css(url: String): String = if (busy) "" else rules.css(url)
    fun documentRule(url: String): String {
        if (busy || url.isEmpty()) return ""
        return rules.match(url, null, FilterRequestTypes.forUrl(url, null))?.text().orEmpty()
    }
    companion object {
        @Volatile private var instance: AdblockEngine? = null
        fun get(context: Context) = instance ?: synchronized(this) {
            instance ?: AdblockEngine(context.applicationContext).also { instance = it }
        }
        fun start(context: Context) { val engine = get(context); BrowserExecutors.io.execute { engine.reload() } }
        fun changed(context: Context) = get(context).reload()
    }
}
