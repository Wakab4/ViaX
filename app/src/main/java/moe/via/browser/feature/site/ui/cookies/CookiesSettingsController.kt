package moe.via.browser.feature.site.ui.cookies

import android.app.Activity
import android.widget.ImageView
import moe.via.browser.R
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.library.ui.recycler.LibraryRecyclerView
import moe.via.browser.feature.site.SiteDecision
import moe.via.browser.feature.site.SiteFeature
import moe.via.browser.feature.site.data.SiteSettingsRepository
import moe.via.browser.ui.components.SkinIconBinder
import moe.via.browser.feature.site.ui.common.NativeSiteSettingsOwner

/** The Activity owns preferences/repository; this owner only holds the Native cookie page views. */
class CookiesSettingsController(private val activity: Activity, private val repository: SiteSettingsRepository,
    private val settings: BrowserSettings, private val back: ImageView? = null) : NativeSiteSettingsOwner {
    private val adapter = CookieSettingsAdapter(activity, ::clicked)
    private val originalBack = back?.drawable
    private val skin = SkinIconBinder(activity)
    private var closed = false
    override val view = LibraryRecyclerView(activity).apply { clipToPadding = false; adapter = this@CookiesSettingsController.adapter }
    override val title get() = activity.getString(R.string.ref_cookie_page_title)

    init { back?.let { skin.image(it, R.drawable.ref_ic_back) } }

    override fun render() {
        if (closed) return
        val allowed = repository.global(SiteFeature.COOKIES, settings) == SiteDecision.ALLOW
        val thirdPartyBlocked = repository.global(SiteFeature.THIRD_PARTY_COOKIES, settings) != SiteDecision.ALLOW
        adapter.submit(listOf(CookieSettingRow(1, allowed), CookieSettingRow(2, thirdPartyBlocked, !allowed)))
    }

    private fun clicked(row: CookieSettingRow) {
        if (closed || row.disabled) return
        if (row.id == 1) repository.setGlobal(SiteFeature.COOKIES, if (row.checked) SiteDecision.DENY else SiteDecision.ALLOW)
        else {
            // The checkbox is "block", whereas the stored flag means "accept third-party".
            if (repository.global(SiteFeature.COOKIES, settings) != SiteDecision.ALLOW) return
            repository.setGlobal(SiteFeature.THIRD_PARTY_COOKIES, if (row.checked) SiteDecision.ALLOW else SiteDecision.DENY)
        }
        render()
    }

    override fun back(): Boolean = false
    override fun close() {
        if (closed) return
        closed = true; view.adapter = null; view.dispose(); skin.close(); back?.setImageDrawable(originalBack)
    }
}
