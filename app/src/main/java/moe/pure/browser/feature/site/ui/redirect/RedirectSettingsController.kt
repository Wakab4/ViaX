package moe.pure.browser.feature.site.ui.redirect

import android.app.Activity
import moe.pure.browser.core.logging.NativeLog as Log
import android.widget.ImageView
import moe.pure.browser.R
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.feature.library.ui.recycler.LibraryRecyclerView
import moe.pure.browser.feature.site.SiteDecision
import moe.pure.browser.feature.site.SiteFeature
import moe.pure.browser.feature.site.SiteSettingsRepository
import moe.pure.browser.ui.components.SkinIconBinder
import moe.pure.browser.feature.site.ui.common.NativeSiteSettingsOwner

/** q3-specific page owner. The hosting Activity owns the shared repository and navigation. */
class RedirectSettingsController(private val activity: Activity, private val repository: SiteSettingsRepository,
    private val settings: BrowserSettings, private val back: ImageView? = null) : NativeSiteSettingsOwner {
    private val state = RedirectSettingsState(global(), repository.all())
    private val dialogs = RedirectSettingsDialogs(activity)
    private val adapter = RedirectSettingsAdapter(activity, ::clicked)
    private var closed = false
    private val originalBack = back?.drawable
    private val skin = SkinIconBinder(activity)
    override val view = LibraryRecyclerView(activity).apply { clipToPadding = false; this.adapter = this@RedirectSettingsController.adapter }
    override val title get() = activity.getString(R.string.ref_redirect_page_title)

    init { back?.let { skin.image(it, R.drawable.ref_ic_back) } }

    private fun global() = repository.global(SiteFeature.REDIRECTS, settings) == SiteDecision.ALLOW

    override fun render() {
        if (closed) return
        state.globalAllowed = global()
        adapter.submit(buildList {
            add(RedirectSettingRow.Global(state.globalAllowed)); add(RedirectSettingRow.Add)
            if (state.asking.isNotEmpty()) {
                add(RedirectSettingRow.Section(activity.getString(R.string.ref_redirect_page_ask_section)))
                state.asking.forEach { add(RedirectSettingRow.Site(it)) }
            }
            if (state.allowed.isNotEmpty()) {
                add(RedirectSettingRow.Section(activity.getString(if (state.globalAllowed)
                    R.string.ref_redirect_page_allowed else R.string.ref_redirect_page_exception)))
                state.allowed.forEach { add(RedirectSettingRow.Site(it)) }
            }
        })
    }

    private fun clicked(row: RedirectSettingRow) {
        if (closed) return
        when (row) {
            is RedirectSettingRow.Global -> {
                repository.setGlobal(SiteFeature.REDIRECTS, if (row.allowed) SiteDecision.ASK else SiteDecision.ALLOW)
                render()
            }
            RedirectSettingRow.Add -> dialogs.add(global(), ::save)
            is RedirectSettingRow.Site -> dialogs.edit(row.key, state.isAllowed(row.key), { save(row.key, it) }) {
                if (state.remove(row.key)) { persist { repository.removeRedirectKey(row.key) }; render() }
            }
            is RedirectSettingRow.Section -> Unit
        }
    }

    private fun save(key: String, allowed: Boolean) {
        if (!closed && state.put(key, allowed)) { persist { repository.setRedirectKey(key, allowed) }; render() }
    }
    private fun persist(write: () -> Unit) {
        try { write() } catch (failure: Exception) { Log.warn("RedirectSettings", "Unable to persist site redirection settings", failure) }
    }
    override fun back(): Boolean = dialogs.back()
    override fun close() {
        if (closed) return
        closed = true; dialogs.close(); view.adapter = null; view.dispose(); adapter.close(); skin.close()
        back?.setImageDrawable(originalBack)
    }
}
