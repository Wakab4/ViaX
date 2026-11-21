package moe.pure.browser.feature.site.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.feature.settings.pages.SettingsRecordPage
import moe.pure.browser.feature.site.SiteSettingsRepository
import moe.pure.browser.feature.site.ui.common.NativeSiteSettingsOwner
import moe.pure.browser.feature.useragent.UserAgentRepository
import moe.pure.browser.ui.components.NativeToolbarTap
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.theme.ThemedActivity

/** Each original site page owns one view and its controllers; the FragmentManager owns navigation. */
abstract class SiteRecordPage : SettingsRecordPage() {
    private val repositoryDelegate = lazy { SiteSettingsRepository(requireContext().applicationContext) }
    protected val repository by repositoryDelegate
    private val agentsDelegate = lazy { UserAgentRepository(requireContext().applicationContext) }
    protected val agents by agentsDelegate
    protected val settings by lazy { BrowserSettings(requireContext()) }
    protected var page: PageScaffold? = null
    protected var content: NativeSiteSettingsOwner? = null
    protected abstract fun createContent(activity: ThemedActivity, back: ImageView): NativeSiteSettingsOwner
    protected open fun configure(page: PageScaffold) = Unit
    final override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        return PageScaffold(activity, "", ::closePage, appliesWindowInsets).also { scaffold ->
            page = scaffold
            val owner = createContent(activity, scaffold.toolbar.getChildAt(0) as ImageView)
            content = owner; scaffold.titleView.text = owner.title
            scaffold.showContent(owner.view, bottomInsets = owner.list)
            configure(scaffold)
            NativeToolbarTap.bind(scaffold.toolbar) { owner.list.smoothScrollToPosition(0) }
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); content?.render() }
    internal override fun consumeBack() = content?.back() == true
    override fun onDestroyView() { content?.close(); content = null; page = null; super.onDestroyView() }
    override fun onDestroy() {
        if (repositoryDelegate.isInitialized()) repository.close()
        if (agentsDelegate.isInitialized()) agents.close()
        super.onDestroy()
    }
}
