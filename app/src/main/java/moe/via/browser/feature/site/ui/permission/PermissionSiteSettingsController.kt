package moe.via.browser.feature.site.ui.permission

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

internal class PermissionSiteSettingsController(activity: Activity, repository: SiteSettingsRepository,
    settings: BrowserSettings, agents: UserAgentRepository, private val descriptor: PermissionSiteDescriptor,
    back: ImageView?) : NativeSitePage(activity, repository, settings, agents, descriptor.feature, back) {
    private val groups = NativeExplicitGroups(repository.all(), feature)
    override fun render() {
        if (closed) return
        val global = global()
        val detail = activity.getString(when (global) {
            SiteDecision.ASK -> descriptor.askDetail
            SiteDecision.DENY -> descriptor.blockedDetail
            else -> descriptor.allowedDetail.takeIf { it != 0 } ?: descriptor.blockedDetail
        })
        submit(buildList {
            if (descriptor.checkbox) add(NativeSiteRow.Toggle(1, title, global == SiteDecision.ASK, detail = detail))
            else add(NativeSiteRow.Link(1, title, detail))
            add(NativeSiteRow.Add(2, texts.add))
            if (groups.list(SiteDecision.DENY).isNotEmpty()) {
                add(NativeSiteRow.Section(0, texts.blocked))
                groups.list(SiteDecision.DENY).forEach { add(NativeSiteRow.Link(it.hashCode(), it)) }
            }
            if (groups.list(SiteDecision.ASK).isNotEmpty() || groups.list(SiteDecision.ALLOW).isNotEmpty()) {
                add(NativeSiteRow.Section(1, if (global != SiteDecision.DENY) texts.allowed else activity.getString(R.string.ref_site_exception)))
                groups.list(SiteDecision.ASK).forEach { add(NativeSiteRow.Link(it.hashCode(), it, texts.ask)) }
                groups.list(SiteDecision.ALLOW).forEach { add(NativeSiteRow.Link(it.hashCode(), it)) }
            }
        })
    }
    override fun clicked(row: NativeSiteRow) {
        if (closed) return
        when (row) {
            is NativeSiteRow.Toggle -> { repository.setGlobal(feature, if (row.checked) SiteDecision.DENY else SiteDecision.ASK); render() }
            is NativeSiteRow.Add -> dialogs.domain(activity.getString(if (global() == SiteDecision.DENY)
                descriptor.addAllow else descriptor.addBlock), normalized = true, askOption = global() == SiteDecision.DENY) { key, ask ->
                save(key, if (ask) SiteDecision.ASK else if (global() == SiteDecision.DENY) SiteDecision.ALLOW else SiteDecision.DENY)
            }
            is NativeSiteRow.Link -> {
                if (row.id == 1) {
                    dialogs.choices(title, listOf(texts.allow, texts.block, texts.ask), global() - 1,
                        { repository.setGlobal(feature, it + 1); render() }, positive = false)
                } else {
                    val key = row.title.toString()
                    dialogs.choices(NativeSiteKeys.display(key), listOf(texts.allow, texts.block, texts.ask),
                        groups.value(key, global()) - 1, { save(key, it + 1) }, {
                            if (groups.remove(key)) {
                                // DEX: camera/microphone page deletion calls Clipboard O(0), not its own setter.
                                persist { repository.removePermissionPageKey(key, feature) }; render()
                            }
                        })
                }
            }
            else -> Unit
        }
    }
    private fun save(key: String, value: Int) {
        val normalized = NativeSiteKeys.normalize(key)
        if (!closed && groups.put(normalized, value)) { persist { repository.writeFeatureKey(normalized, feature, value) }; render() }
    }
}
