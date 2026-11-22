package moe.via.browser.feature.site.ui.font

import android.app.Activity
import android.widget.ImageView
import moe.via.browser.R
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.site.SiteFeature
import moe.via.browser.feature.site.data.SiteSettingsRepository
import moe.via.browser.feature.site.ui.common.NativeSiteKeys
import moe.via.browser.feature.site.ui.common.NativeSitePage
import moe.via.browser.feature.site.ui.common.NativeSiteRow
import moe.via.browser.feature.useragent.data.UserAgentRepository
import java.util.Locale

internal class FontSiteSettingsController(activity: Activity, repository: SiteSettingsRepository, settings: BrowserSettings,
    agents: UserAgentRepository, back: ImageView?) : NativeSitePage(activity, repository, settings, agents, SiteFeature.FONT_SIZE, back) {
    private val overrides = sortedMapOf<String, Int>(NativeSiteKeys.order).apply {
        repository.all().filter { it.enabled }.forEach { config -> config.values[feature]?.takeIf { it > 0 }?.let {
            put(NativeSiteKeys.normalize(config.domain), it)
        } }
    }
    init {
        adapter.onSliderValue = { _, value -> if (!closed) { repository.setGlobal(feature, value); render() } }
    }
    override fun render() {
        if (closed) return
        submit(buildList {
            add(NativeSiteRow.Slider(1, title, settings.textZoom))
            add(NativeSiteRow.Toggle(3, activity.getString(R.string.ref_site_force_zoom), settings.preferences.forceZoom))
            add(NativeSiteRow.Add(2, texts.add))
            if (overrides.isNotEmpty()) {
                add(NativeSiteRow.Section(0, activity.getString(R.string.ref_site_exception)))
                overrides.forEach { (key, size) -> add(NativeSiteRow.Link(key.hashCode(), key, String.format(Locale.ROOT, "%d%%", size))) }
            }
        })
    }
    override fun clicked(row: NativeSiteRow) {
        if (closed) return
        when (row) {
            is NativeSiteRow.Toggle -> { settings.preferences.forceZoom = !row.checked; render() }
            is NativeSiteRow.Add -> dialogs.domain(activity.getString(R.string.ref_site_font_add_hint), normalized = false) { key, _ ->
                edit(key, settings.textZoom, false)
            }
            is NativeSiteRow.Link -> overrides[row.title.toString()]?.let { edit(row.title.toString(), it, true) }
            else -> Unit
        }
    }
    private fun edit(key: String, size: Int, existing: Boolean) {
        dialogs.font(null, size, settings.textZoom, { value ->
            val normalized = NativeSiteKeys.normalize(key)
            if (!closed && normalized.isNotEmpty()) {
                overrides[normalized] = value; persist { repository.writeFeatureKey(normalized, feature, value) }; render()
            }
        }, onDelete = if (existing) ({
            overrides.remove(key); persist { repository.writeFeatureKey(key, feature, null) }; render()
        }) else null)
    }
}
