package moe.via.browser.feature.site.ui.binary

import android.app.Activity
import android.widget.ImageView
import moe.via.browser.R
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.site.SiteDecision
import moe.via.browser.feature.site.data.SiteSettingsRepository
import moe.via.browser.feature.site.ui.common.NativeExplicitGroups
import moe.via.browser.feature.site.ui.common.NativeSiteKeys
import moe.via.browser.feature.site.ui.common.NativeSitePage
import moe.via.browser.feature.site.ui.common.NativeSiteRow
import moe.via.browser.feature.useragent.data.UserAgentRepository

internal class BinarySiteSettingsController(activity: Activity, repository: SiteSettingsRepository, settings: BrowserSettings,
    agents: UserAgentRepository, private val descriptor: BinarySiteDescriptor, back: ImageView?) :
    NativeSitePage(activity, repository, settings, agents, descriptor.feature, back) {
    private val groups = NativeExplicitGroups(repository.all(), feature)
    override fun render() {
        if (closed) return
        val enabled = global() == SiteDecision.ALLOW
        val negative = if (descriptor.onOff) texts.off else texts.blocked
        val positive = if (descriptor.onOff) texts.on else texts.allowed
        submit(buildList {
            add(NativeSiteRow.Toggle(1, title, enabled, detail = descriptor.enabledDetail?.let {
                activity.getString(if (enabled) it else R.string.ref_site_blocked)
            }))
            add(NativeSiteRow.Add(2, texts.add))
            if (groups.list(SiteDecision.DENY).isNotEmpty()) {
                add(NativeSiteRow.Section(0, negative)); groups.list(SiteDecision.DENY).forEach { add(NativeSiteRow.Link(it.hashCode(), it)) }
            }
            if (groups.list(SiteDecision.ALLOW).isNotEmpty()) {
                add(NativeSiteRow.Section(1, if (enabled) positive else activity.getString(R.string.ref_site_exception)))
                groups.list(SiteDecision.ALLOW).forEach { add(NativeSiteRow.Link(it.hashCode(), it)) }
            }
        })
    }
    override fun clicked(row: NativeSiteRow) {
        if (closed) return
        when (row) {
            is NativeSiteRow.Toggle -> { repository.setGlobal(feature, if (row.checked) SiteDecision.DENY else SiteDecision.ALLOW); render() }
            is NativeSiteRow.Add -> {
                val previous = global() == SiteDecision.ALLOW
                dialogs.domain(activity.getString(if (previous) descriptor.addDisable else descriptor.addEnable), normalized = false) { key, _ ->
                    save(key, if (previous) SiteDecision.DENY else SiteDecision.ALLOW)
                }
            }
            is NativeSiteRow.Link -> {
                val key = row.title.toString()
                dialogs.choices(NativeSiteKeys.display(key), if (descriptor.onOff) listOf(texts.on, texts.off) else listOf(texts.allow, texts.block),
                    if (groups.value(key, global()) == SiteDecision.ALLOW) 0 else 1,
                    { save(key, if (it == 0) SiteDecision.ALLOW else SiteDecision.DENY) }, {
                        if (groups.remove(key)) { persist { repository.writeFeatureKey(key, feature, null) }; render() }
                    })
            }
            else -> Unit
        }
    }
    private fun save(key: String, value: Int) {
        if (!closed && groups.put(key, value)) { persist { repository.writeFeatureKey(key, feature, value) }; render() }
    }
}
