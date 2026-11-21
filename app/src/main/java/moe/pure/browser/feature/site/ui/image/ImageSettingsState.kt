package moe.pure.browser.feature.site.ui.image

import moe.pure.browser.feature.site.SiteConfiguration
import moe.pure.browser.feature.site.SiteDecision
import moe.pure.browser.feature.site.SiteFeature
import moe.pure.browser.feature.site.ui.redirect.RedirectSettingsDomain

internal sealed interface ImageSettingRow {
    data class Global(val enabled: Boolean, val wifiOnly: Boolean) : ImageSettingRow
    data object Add : ImageSettingRow
    data class Section(val title: String) : ImageSettingRow
    data class Site(val key: String) : ImageSettingRow
}

/** o1/kb.a retains explicit allow/block lists independently of both global image bits. */
internal class ImageSettingsState(configurations: List<SiteConfiguration>) {
    val blocked = mutableListOf<String>()
    val allowed = mutableListOf<String>()

    init {
        configurations.filter { it.enabled }.forEach { configuration ->
            configuration.values[SiteFeature.IMAGES]?.let { decision ->
                (if (decision == SiteDecision.ALLOW) allowed else blocked).add(RedirectSettingsDomain.normalize(configuration.domain))
            }
        }
        sort()
    }

    fun isAllowed(key: String, globalEnabled: Boolean): Boolean {
        val normalized = RedirectSettingsDomain.normalize(key)
        return when {
            allowed.contains(normalized) -> true
            blocked.contains(normalized) -> false
            else -> globalEnabled
        }
    }

    fun put(key: String, allow: Boolean): Boolean {
        val normalized = RedirectSettingsDomain.normalize(key)
        if (normalized.isEmpty()) return false
        (if (allow) blocked else allowed).remove(normalized)
        // kb.d.a appends even when the selected radio already belongs to this group.
        (if (allow) allowed else blocked).add(normalized)
        sort(); return true
    }

    fun remove(key: String): Boolean {
        val normalized = RedirectSettingsDomain.normalize(key)
        val fromBlocked = blocked.remove(normalized)
        val fromAllowed = allowed.remove(normalized)
        return fromBlocked || fromAllowed
    }

    private fun sort() { blocked.sortWith(RedirectSettingsDomain.order); allowed.sortWith(RedirectSettingsDomain.order) }
}
