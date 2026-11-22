package moe.via.browser.feature.useragent

import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.useragent.data.UserAgent

/** w7.u3/t3: selected IDs and raw fallback strings are distinct Native preferences. */
class UserAgentPreferences(private val settings: BrowserSettings) {
    fun global(agent: UserAgent) {
        settings.userAgentId = agent.id
        if (needsFallback(agent.id)) settings.values.edit().putString("uastring", agent.value.orEmpty()
            .replace(Regex("[\\t\\r\\n]"), " ")).apply()
        policyChanged()
    }
    fun desktop(agent: UserAgent) {
        settings.preferences.desktopUserAgentId = agent.id
        if (needsFallback(agent.id)) settings.values.edit().putString("duastring", agent.value).apply()
        if (settings.preferences.desktop) policyChanged()
    }
    fun reduction(enabled: Boolean) { settings.preferences.simplifyUserAgent = enabled; policyChanged() }
    fun policyChanged() { settings.values.edit().putLong("ua.revision", System.nanoTime()).apply() }
    private fun needsFallback(id: Int) = id > 0 || id <= -999
}
