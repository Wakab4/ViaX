package moe.via.browser.feature.site.ui.useragent

import android.app.Activity
import android.widget.ImageView
import moe.via.browser.R
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.settings.SettingsEntry
import moe.via.browser.feature.settings.SettingsRoute
import moe.via.browser.feature.site.SiteFeature
import moe.via.browser.feature.site.data.SiteSettingsRepository
import moe.via.browser.feature.site.ui.common.NativeSiteKeys
import moe.via.browser.feature.site.ui.common.NativeSitePage
import moe.via.browser.feature.site.ui.common.NativeSiteRow
import moe.via.browser.feature.site.ui.common.NativeSiteTexts
import moe.via.browser.feature.useragent.data.UserAgent
import moe.via.browser.feature.useragent.data.UserAgentRepository
import java.util.Locale

internal class UserAgentSiteSettingsController(activity: Activity, repository: SiteSettingsRepository,
    settings: BrowserSettings, agents: UserAgentRepository, back: ImageView?) :
    NativeSitePage(activity, repository, settings, agents, SiteFeature.USER_AGENT, back) {
    private data class Entry(val id: Int, val label: String, val rememberedCustom: String?)
    private val entries = sortedMapOf<String, Entry>(NativeSiteKeys.order)
    init {
        val choices = agents.all()
        repository.all().filter { it.enabled }.forEach { config ->
            config.values[feature]?.takeIf { it != -1000 }?.let { id ->
                val agent = choices.firstOrNull { it.id == id }
                if (id == -999 || agent != null) entries[NativeSiteKeys.normalize(config.domain)] =
                    Entry(id, if (id == -999) config.customUserAgent.orEmpty() else NativeSiteTexts.userAgentLabel(activity, agent!!), config.customUserAgent)
            }
        }
    }
    override fun render() {
        if (closed) return
        submit(buildList {
            add(NativeSiteRow.Link(1, title, agents.all().firstOrNull { it.id == settings.userAgentId }?.let { NativeSiteTexts.userAgentLabel(activity, it) }))
            add(NativeSiteRow.Add(2, texts.add))
            if (entries.isNotEmpty()) {
                add(NativeSiteRow.Section(0, activity.getString(R.string.ref_site_exception)))
                entries.forEach { (key, entry) -> add(NativeSiteRow.Link(key.hashCode(), key, entry.label)) }
            }
        })
    }
    override fun clicked(row: NativeSiteRow) {
        if (closed) return
        when (row) {
            is NativeSiteRow.Add -> dialogs.domain(activity.getString(R.string.ref_site_user_agent_add_hint), normalized = false) { key, _ -> edit(key) }
            is NativeSiteRow.Link -> if (row.id == 1) SettingsEntry.open(activity, SettingsRoute.USER_AGENT) else edit(row.title.toString())
            else -> Unit
        }
    }
    private fun edit(key: String) {
        if (key.isEmpty()) return
        // w5.r lowercases the lookup but does not trim until the selected value is persisted.
        val previous = entries[key.lowercase(Locale.ROOT)]
        val choices = agents.all() + UserAgent(-999, texts.custom, null)
        dialogs.userAgent(NativeSiteKeys.display(key), choices, previous?.id ?: settings.userAgentId,
            previous?.rememberedCustom, { choice ->
                val normalized = NativeSiteKeys.normalize(key)
                if (!closed && normalized.isNotEmpty()) {
                    val remembered = entries[normalized]?.rememberedCustom
                    entries[normalized] = Entry(choice.id, choice.name,
                        if (choice.id == -999) choice.value else remembered)
                    persist { repository.writeFeatureKey(normalized, feature, choice.id, choice.value) }; render()
                }
            }, {
                val normalized = NativeSiteKeys.normalize(key)
                if (entries.remove(normalized) != null) {
                    // Native e0(-1000) keeps Q/custom text; the typed repository preserves that field.
                    persist { repository.writeFeatureKey(normalized, feature, null) }; render()
                }
            })
    }
}
