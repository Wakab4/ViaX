package moe.via.browser.feature.site.ui.common

import moe.via.browser.feature.site.SiteConfiguration
import moe.via.browser.feature.site.SiteFeature

/** kb.d's lists deliberately allow repeated entries within the current group. */
internal class NativeExplicitGroups(configurations: List<SiteConfiguration>, feature: SiteFeature) {
    private val groups = sortedMapOf<Int, MutableList<String>>()
    init {
        configurations.filter { it.enabled }.forEach { config -> config.values[feature]?.let { value ->
            groups.getOrPut(value) { mutableListOf() }.add(NativeSiteKeys.normalize(config.domain))
        } }
        groups.values.forEach { it.sortWith(NativeSiteKeys.order) }
    }
    fun list(value: Int): List<String> = groups[value].orEmpty()
    fun value(key: String, default: Int): Int {
        val normalized = NativeSiteKeys.normalize(key)
        return groups.entries.firstOrNull { it.value.contains(normalized) }?.key ?: default
    }
    fun put(key: String, value: Int): Boolean {
        val normalized = NativeSiteKeys.normalize(key)
        if (normalized.isEmpty()) return false
        groups.forEach { (other, rows) -> if (other != value) rows.remove(normalized) }
        groups.getOrPut(value) { mutableListOf() }.apply { add(normalized); sortWith(NativeSiteKeys.order) }
        return true
    }
    fun remove(key: String): Boolean {
        val normalized = NativeSiteKeys.normalize(key)
        var removed = false
        groups.values.forEach { if (it.remove(normalized)) removed = true }
        return removed
    }
}
