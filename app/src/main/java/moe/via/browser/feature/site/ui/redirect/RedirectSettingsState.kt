package moe.via.browser.feature.site.ui.redirect

import moe.via.browser.feature.site.SiteConfiguration
import moe.via.browser.feature.site.SiteDecision
import moe.via.browser.feature.site.SiteFeature

internal sealed interface RedirectSettingRow {
    data class Global(val allowed: Boolean) : RedirectSettingRow
    data object Add : RedirectSettingRow
    data class Section(val title: String) : RedirectSettingRow
    data class Site(val key: String) : RedirectSettingRow
}

/** r3 / kb.a retain two explicit lists independently of the current global value. */
internal class RedirectSettingsState(var globalAllowed: Boolean, configurations: List<SiteConfiguration>) {
    val asking = mutableListOf<String>()
    val allowed = mutableListOf<String>()

    init {
        configurations.filter { it.enabled }.forEach { configuration ->
            configuration.values[SiteFeature.REDIRECTS]?.let { decision ->
                (if (decision == SiteDecision.ALLOW) allowed else asking).add(RedirectSettingsDomain.normalize(configuration.domain))
            }
        }
        asking.sortWith(RedirectSettingsDomain.order); allowed.sortWith(RedirectSettingsDomain.order)
    }

    fun isAllowed(key: String): Boolean = when {
        allowed.contains(key) -> true
        asking.contains(key) -> false
        else -> globalAllowed
    }

    fun put(key: String, allow: Boolean): Boolean {
        val listed = RedirectSettingsDomain.normalize(key)
        if (listed.isEmpty()) return false
        (if (allow) asking else allowed).remove(listed)
        val target = if (allow) allowed else asking
        // Native adds again even for an already-selected radio; it does not deduplicate this group.
        target.add(listed); target.sortWith(RedirectSettingsDomain.order)
        return true
    }

    fun remove(key: String): Boolean {
        val listed = RedirectSettingsDomain.normalize(key)
        val removedAsk = asking.remove(listed)
        val removedAllow = allowed.remove(listed)
        return removedAsk || removedAllow
    }
}
