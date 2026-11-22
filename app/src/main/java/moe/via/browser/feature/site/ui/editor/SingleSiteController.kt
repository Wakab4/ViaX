package moe.via.browser.feature.site.ui.editor

import android.app.Activity
import moe.via.browser.R
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.library.ui.recycler.LibraryRecyclerView
import moe.via.browser.feature.site.SiteDecision
import moe.via.browser.feature.site.SiteFeature
import moe.via.browser.feature.site.data.SiteSettingsRepository
import moe.via.browser.feature.site.data.SiteCommitResult
import moe.via.browser.feature.site.ui.common.NativeSiteAdapter
import moe.via.browser.feature.site.ui.common.NativeSiteEntryDialogs
import moe.via.browser.feature.site.ui.common.NativeSiteRow
import moe.via.browser.feature.site.ui.common.NativeSiteSettingsOwner
import moe.via.browser.feature.site.ui.common.NativeSiteTexts
import moe.via.browser.feature.useragent.data.UserAgent
import moe.via.browser.feature.useragent.data.UserAgentRepository
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.tasks.NativeViewJobs

/** u4's view edits a copy; the actual Fragment commits it in onDestroy. */
class SingleSiteController(private val activity: Activity, private val repository: SiteSettingsRepository,
    private val settings: BrowserSettings, private val agents: UserAgentRepository,
    key: String, launchFlags: Int, private val leave: () -> Unit) : NativeSiteSettingsOwner {
    private val draft = repository.editDraft(key, launchFlags)
    private val texts = NativeSiteTexts(activity, settings, agents)
    private val dialogs = NativeSiteEntryDialogs(activity)
    private val scope = DialogScope()
    private var resetDialog: BrowserDialog? = null
    private val adapter = NativeSiteAdapter(activity, ::clicked)
    private var closed = false
    private val jobs = NativeViewJobs()
    override val title: CharSequence get() = activity.getString(R.string.ref_site_editor_title)
    override val view = LibraryRecyclerView(activity).apply { adapter = this@SingleSiteController.adapter }

    override fun render() {
        if (closed) return
        jobs.launch({ buildList {
            val disabled = !draft.config.enabled
            add(NativeSiteRow.Toggle(1, activity.getString(R.string.ref_site_editor_enable, draft.key), !disabled))
            sections().forEach { (section, entries) ->
                add(NativeSiteRow.Section(-section, activity.getString(section)))
                entries.forEach { (id, feature) -> add(NativeSiteRow.Link(id, texts.title(feature), summary(feature), disabled)) }
            }
        } }) { adapter.submit(it) }
    }

    private fun global(feature: SiteFeature): Int = if (feature == SiteFeature.IMAGES && settings.preferences.wifiImages)
        SiteDecision.WIFI else repository.global(feature, settings)

    private fun summary(feature: SiteFeature): String? {
        val explicit = draft.config.feature(feature)
        val value = explicit ?: global(feature)
        val label = texts.summary(feature, value, draft.config.customUserAgent)
        return if (feature == SiteFeature.USER_AGENT && label.isEmpty()) null
            else if (explicit == null) texts.defaultLabel(label) else label
    }

    private fun clicked(row: NativeSiteRow) {
        if (closed) return
        if (row.id == 1) { draft.config = draft.config.withEnabled(!draft.config.enabled); render(); return }
        if (!draft.config.enabled) return
        val feature = FEATURES[row.id] ?: return
        when (feature) {
            SiteFeature.FONT_SIZE -> {
                if (draft.launchFlags and 2 != 0) { draft.resultFlags = draft.resultFlags or 2; leave(); return }
                dialogs.font(null, draft.config.feature(feature) ?: settings.textZoom, settings.textZoom,
                    { draft.set(feature, it); render() },
                    onReset = if (draft.config.feature(feature) != null) ({ draft.set(feature, null); render() }) else null)
            }
            SiteFeature.USER_AGENT -> {
                val inherited = texts.defaultLabel(texts.summary(feature, settings.userAgentId))
                val options = listOf(UserAgent(-1000, inherited, null)) + agents.all() + UserAgent(-999, texts.custom, draft.config.customUserAgent)
                // u4 only changes the selected preset; its custom UA text remains in the draft.
                dialogs.userAgent(texts.title(feature), options, draft.config.userAgentChoice, draft.config.customUserAgent,
                    { selected -> draft.setUserAgent(selected.id, if (selected.id == -999) selected.value else draft.config.customUserAgent); render() },
                    positive = false)
            }
            else -> choose(feature)
        }
    }

    private fun choose(feature: SiteFeature) {
        val booleans = feature in BOOLEAN_CHOICES
        val permissions = feature in PERMISSIONS
        val values = if (permissions) listOf(null, SiteDecision.ALLOW, SiteDecision.DENY, SiteDecision.ASK)
            else if (feature == SiteFeature.REDIRECTS) listOf(null, SiteDecision.ALLOW, SiteDecision.ASK)
            else listOf(null, SiteDecision.ALLOW, SiteDecision.DENY)
        val inherited = texts.defaultLabel(texts.summary(feature, global(feature)))
        val labels = buildList<CharSequence> {
            add(inherited)
            when {
                booleans -> { add(texts.on); add(texts.off) }
                feature == SiteFeature.REDIRECTS -> { add(texts.allowed); add(texts.summary(feature, SiteDecision.ASK)) }
                else -> { add(texts.allow); add(texts.block); if (permissions) add(texts.ask) }
            }
        }
        dialogs.choices(texts.title(feature), labels, values.indexOf(draft.config.feature(feature)).coerceAtLeast(0), {
            draft.set(feature, values[it]); render()
        })
    }

    fun reset() {
        if (closed) return
        val dialog = BrowserDialog(activity, activity.getString(R.string.ref_site_editor_reset_title),
            activity.getString(R.string.ref_site_editor_reset_message, draft.key))
            .positive(activity.getString(R.string.ref_site_reset)) { draft.reset(); render(); true }
            .negative(activity.getString(android.R.string.cancel))
        resetDialog?.dismiss(); resetDialog = dialog
        scope.show(dialog) { if (resetDialog === dialog) resetDialog = null }
    }

    fun commit(): SiteCommitResult = repository.commit(draft)
    override fun back(): Boolean {
        resetDialog?.let { it.dismiss(); return true }
        return dialogs.back()
    }
    override fun close() {
        if (closed) return
        closed = true; jobs.close()
        dialogs.close(); scope.dispose(); view.adapter = null; view.dispose(); adapter.close()
    }

    private fun sections() = listOf(
        R.string.ref_site_section_content to listOf(14 to SiteFeature.FONT_SIZE, 2 to SiteFeature.USER_AGENT,
            3 to SiteFeature.DESKTOP, 4 to SiteFeature.IMAGES, 6 to SiteFeature.JAVASCRIPT),
        R.string.ref_site_section_basic to listOf(5 to SiteFeature.ADBLOCK, 8 to SiteFeature.INCOGNITO),
        R.string.ref_site_section_permissions to listOf(11 to SiteFeature.MICROPHONE, 12 to SiteFeature.CAMERA,
            7 to SiteFeature.CLIPBOARD, 9 to SiteFeature.OPEN_APPS, 10 to SiteFeature.REDIRECTS, 13 to SiteFeature.LOCATION),
        R.string.ref_site_section_advanced to listOf(15 to SiteFeature.BACK_CACHE))

    companion object {
        private val FEATURES = mapOf(14 to SiteFeature.FONT_SIZE, 2 to SiteFeature.USER_AGENT, 3 to SiteFeature.DESKTOP,
            4 to SiteFeature.IMAGES, 6 to SiteFeature.JAVASCRIPT, 5 to SiteFeature.ADBLOCK, 8 to SiteFeature.INCOGNITO,
            11 to SiteFeature.MICROPHONE, 12 to SiteFeature.CAMERA, 7 to SiteFeature.CLIPBOARD, 9 to SiteFeature.OPEN_APPS,
            10 to SiteFeature.REDIRECTS, 13 to SiteFeature.LOCATION, 15 to SiteFeature.BACK_CACHE)
        private val BOOLEAN_CHOICES = setOf(SiteFeature.DESKTOP, SiteFeature.ADBLOCK, SiteFeature.INCOGNITO, SiteFeature.BACK_CACHE)
        private val PERMISSIONS = setOf(SiteFeature.CLIPBOARD, SiteFeature.OPEN_APPS, SiteFeature.CAMERA, SiteFeature.MICROPHONE, SiteFeature.LOCATION)
    }
}
