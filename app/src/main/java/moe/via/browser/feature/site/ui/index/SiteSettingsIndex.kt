package moe.via.browser.feature.site.ui.index

import android.app.Activity
import moe.via.browser.R
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.library.ui.recycler.LibraryRecyclerView
import moe.via.browser.feature.site.SiteDecision
import moe.via.browser.feature.site.SiteFeature
import moe.via.browser.feature.site.data.SiteSettingsRepository
import moe.via.browser.feature.site.ui.common.NativeSiteAdapter
import moe.via.browser.feature.site.ui.common.NativeSiteRow
import moe.via.browser.feature.site.ui.common.NativeSiteSettingsOwner
import moe.via.browser.feature.site.ui.common.NativeSiteTexts
import moe.via.browser.feature.useragent.data.UserAgentRepository
import moe.via.browser.ui.tasks.NativeViewJobs

/** y4 index: explicit Native order and summaries, separate from the raw-domain list. */
class SiteSettingsIndex(private val activity: Activity, private val repository: SiteSettingsRepository,
    private val settings: BrowserSettings, agents: UserAgentRepository,
    private val all: () -> Unit, private val open: (SiteFeature) -> Unit) : NativeSiteSettingsOwner {
    private val texts = NativeSiteTexts(activity, settings, agents)
    private val adapter = NativeSiteAdapter(activity) { row ->
        if (row.id == 1) all() else FEATURES[row.id]?.let(open)
    }
    override val view = LibraryRecyclerView(activity).apply { adapter = this@SiteSettingsIndex.adapter }
    override val title: CharSequence get() = activity.getString(R.string.ref_site_editor_title)
    private var closed = false
    private val jobs = NativeViewJobs()

    override fun render() {
        if (closed) return
        jobs.launch({ buildList {
            add(NativeSiteRow.Link(1, activity.getString(R.string.ref_site_all_title)))
            sections().forEach { (section, entries) ->
                add(NativeSiteRow.Section(-section, activity.getString(section)))
                entries.forEach { (id, feature) ->
                    val value = if (feature == SiteFeature.IMAGES && settings.preferences.wifiImages) SiteDecision.WIFI
                        else repository.global(feature, settings)
                    val detail = if (feature == SiteFeature.COOKIES && value == SiteDecision.ALLOW &&
                        repository.global(SiteFeature.THIRD_PARTY_COOKIES, settings) != SiteDecision.ALLOW)
                        activity.getString(R.string.ref_site_cookie_third_party_only)
                    else texts.summary(feature, value)
                    add(NativeSiteRow.Link(id, texts.title(feature), detail.takeIf { it.isNotEmpty() }))
                }
            }
        } }) { adapter.submit(it) }
    }
    override fun back() = false
    override fun close() { if (!closed) { closed = true; jobs.close(); view.adapter = null; view.dispose(); adapter.close() } }

    private fun sections() = listOf(
        R.string.ref_site_section_content to listOf(14 to SiteFeature.FONT_SIZE, 3 to SiteFeature.USER_AGENT,
            17 to SiteFeature.DESKTOP, 2 to SiteFeature.IMAGES, 8 to SiteFeature.JAVASCRIPT, 9 to SiteFeature.COOKIES, 10 to SiteFeature.POPUPS),
        R.string.ref_site_section_basic to listOf(12 to SiteFeature.ADBLOCK),
        R.string.ref_site_section_permissions to listOf(4 to SiteFeature.CLIPBOARD, 11 to SiteFeature.OPEN_APPS,
            5 to SiteFeature.LOCATION, 6 to SiteFeature.CAMERA, 7 to SiteFeature.MICROPHONE,
            13 to SiteFeature.REDIRECTS, 15 to SiteFeature.VIBRATION),
        R.string.ref_site_section_advanced to listOf(16 to SiteFeature.BACK_CACHE))
    companion object {
        private val FEATURES = mapOf(14 to SiteFeature.FONT_SIZE, 3 to SiteFeature.USER_AGENT, 17 to SiteFeature.DESKTOP,
            2 to SiteFeature.IMAGES, 8 to SiteFeature.JAVASCRIPT, 9 to SiteFeature.COOKIES, 10 to SiteFeature.POPUPS,
            12 to SiteFeature.ADBLOCK, 4 to SiteFeature.CLIPBOARD, 11 to SiteFeature.OPEN_APPS, 5 to SiteFeature.LOCATION,
            6 to SiteFeature.CAMERA, 7 to SiteFeature.MICROPHONE, 13 to SiteFeature.REDIRECTS,
            15 to SiteFeature.VIBRATION, 16 to SiteFeature.BACK_CACHE)
    }
}
