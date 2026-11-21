package moe.pure.browser.feature.site.ui.common

import android.app.Activity
import moe.pure.browser.core.logging.NativeLog as Log
import android.widget.ImageView
import moe.pure.browser.R
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.feature.library.ui.recycler.LibraryRecyclerView
import moe.pure.browser.feature.site.SiteFeature
import moe.pure.browser.feature.site.SiteSettingsRepository
import moe.pure.browser.feature.useragent.UserAgentRepository
import moe.pure.browser.ui.components.SkinIconBinder

internal abstract class NativeSitePage(protected val activity: Activity, protected val repository: SiteSettingsRepository,
    protected val settings: BrowserSettings, protected val agents: UserAgentRepository,
    protected val feature: SiteFeature, private val backImage: ImageView?) : NativeSiteSettingsOwner {
    protected val texts = NativeSiteTexts(activity, settings, agents)
    protected val dialogs = NativeSiteEntryDialogs(activity)
    protected val adapter = NativeSiteAdapter(activity, ::clicked)
    private val skin = SkinIconBinder(activity)
    private val originalBack = backImage?.drawable
    protected var closed = false
        private set
    override val title get() = texts.title(feature)
    override val view = LibraryRecyclerView(activity).apply { clipToPadding = false; adapter = this@NativeSitePage.adapter }
    init { backImage?.let { skin.image(it, R.drawable.ref_ic_back) } }
    protected fun submit(rows: List<NativeSiteRow>) {
        if (closed) return
        adapter.submit(rows)
    }
    protected fun global() = repository.global(feature, settings)
    protected fun persist(write: () -> Unit) {
        try { write() } catch (failure: Exception) { Log.warn("SiteSettings", "Unable to persist site setting", failure) }
    }
    protected abstract fun clicked(row: NativeSiteRow)
    override fun back() = dialogs.back()
    override fun close() {
        if (closed) return
        view.clearFocus()
        closed = true; dialogs.close(); view.adapter = null; view.dispose(); adapter.close(); skin.close()
        backImage?.setImageDrawable(originalBack)
    }
}
