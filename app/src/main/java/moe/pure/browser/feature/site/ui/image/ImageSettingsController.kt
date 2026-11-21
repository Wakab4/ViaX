package moe.pure.browser.feature.site.ui.image

import android.app.Activity
import moe.pure.browser.core.logging.NativeLog as Log
import android.widget.ImageView
import moe.pure.browser.R
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.feature.library.ui.recycler.LibraryRecyclerView
import moe.pure.browser.feature.site.SiteDecision
import moe.pure.browser.feature.site.SiteFeature
import moe.pure.browser.feature.site.SiteSettingsRepository
import moe.pure.browser.feature.site.ui.redirect.RedirectSettingsDomain
import moe.pure.browser.ui.components.SkinIconBinder
import moe.pure.browser.feature.site.ui.common.NativeSiteSettingsOwner

/** jb.n1/o1 page owner, separate from the per-site image override picker. */
class ImageSettingsController(private val activity: Activity, private val repository: SiteSettingsRepository,
    private val settings: BrowserSettings, private val back: ImageView? = null) : NativeSiteSettingsOwner {
    private val state = ImageSettingsState(repository.all())
    private val dialogs = ImageSettingsDialogs(activity)
    private val adapter = ImageSettingsAdapter(activity, ::clicked)
    private val originalBack = back?.drawable
    private val skin = SkinIconBinder(activity)
    private var closed = false
    override val view = LibraryRecyclerView(activity).apply { clipToPadding = false; adapter = this@ImageSettingsController.adapter }
    override val title get() = activity.getString(R.string.ref_image_page_title)

    init { back?.let { skin.image(it, R.drawable.ref_ic_back) } }

    private fun enabled() = settings.preferences.imagesEnabled
    private fun wifiOnly() = settings.preferences.wifiImages
    override fun render() {
        if (closed) return
        val enabled = enabled(); val wifiOnly = wifiOnly()
        adapter.submit(buildList {
            add(ImageSettingRow.Global(enabled, wifiOnly)); add(ImageSettingRow.Add)
            if (state.blocked.isNotEmpty()) {
                add(ImageSettingRow.Section(activity.getString(R.string.ref_image_page_blocked)))
                state.blocked.forEach { add(ImageSettingRow.Site(it)) }
            }
            if (state.allowed.isNotEmpty()) {
                add(ImageSettingRow.Section(activity.getString(if (enabled || wifiOnly)
                    R.string.ref_image_page_allowed else R.string.ref_image_page_exception)))
                state.allowed.forEach { add(ImageSettingRow.Site(it)) }
            }
        })
    }

    private fun clicked(row: ImageSettingRow) {
        if (closed) return
        when (row) {
            is ImageSettingRow.Global -> dialogs.mode(enabled(), wifiOnly()) { index ->
                repository.setGlobal(SiteFeature.IMAGES, when (index) { 1 -> SiteDecision.DENY
                    2 -> SiteDecision.WIFI; else -> SiteDecision.ALLOW })
                render()
            }
            ImageSettingRow.Add -> dialogs.add(::enabled, ::save)
            is ImageSettingRow.Site -> dialogs.edit(row.key, state.isAllowed(row.key, enabled()), { save(row.key, it) }) {
                if (state.remove(row.key)) { persist { repository.removeImageKey(row.key) }; render() }
            }
            is ImageSettingRow.Section -> Unit
        }
    }

    private fun save(key: String, allow: Boolean) {
        val normalized = RedirectSettingsDomain.normalize(key)
        if (!closed && state.put(normalized, allow)) { persist { repository.setImageKey(normalized, allow) }; render() }
    }
    private fun persist(write: () -> Unit) {
        try { write() } catch (failure: Exception) { Log.warn("ImageSettings", "Unable to persist site image settings", failure) }
    }
    override fun back(): Boolean = dialogs.back()
    override fun close() {
        if (closed) return
        closed = true; dialogs.close(); view.adapter = null; view.dispose(); adapter.close(); skin.close()
        back?.setImageDrawable(originalBack)
    }
}
