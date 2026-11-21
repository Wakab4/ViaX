package moe.pure.browser.feature.adblock

import android.net.Uri
import moe.pure.browser.browser.TabRuntimeState
import moe.pure.browser.model.BrowserTab
import java.util.concurrent.ConcurrentHashMap

/** Continue-loading grants only this tab's main documents, for the reference's host scope. */
class AdblockNavigationState {
    private val allowed = ConcurrentHashMap.newKeySet<String>()
    fun allow(url: String) { host(url).takeIf(String::isNotEmpty)?.let(allowed::add) }
    fun allowed(url: String) = host(url) in allowed
    companion object {
        private val KEY = TabRuntimeState.Key<AdblockNavigationState>()
        fun of(tab: BrowserTab) = tab.runtime.get(KEY, ::AdblockNavigationState)
        fun host(url: String): String = if ("://" !in url) "" else url.substringAfter("://").substringBefore('/').substringBefore(':')
        fun isContinue(uri: Uri): Boolean = uri.toString().startsWith("v://blocker/jump?url=", true)
        fun target(uri: Uri): String? = runCatching { java.net.URLDecoder.decode(uri.toString().substring(21), "UTF-8") }.getOrNull()?.takeIf { value ->
            val parsed = Uri.parse(value)
            parsed.scheme?.lowercase() in setOf("http", "https") && !parsed.host.isNullOrEmpty()
        }
    }
}
