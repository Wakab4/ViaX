package moe.pure.browser.feature.settings.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.R
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.feature.settings.navigation.SettingsNavigation
import moe.pure.browser.feature.useragent.UserAgentRepository
import moe.pure.browser.feature.useragent.ui.GlobalUserAgentController
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.components.NativeToolbarTap
import moe.pure.browser.ui.pages.RecordPageFragment
import moe.pure.browser.ui.theme.ThemedActivity

class UserAgentSettingsFragment : SettingsRecordPage() {
    private var repository: UserAgentRepository? = null
    private var controller: GlobalUserAgentController? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val data = UserAgentRepository(activity).also { repository = it }
        val list = GlobalUserAgentController(activity, data, BrowserSettings(activity)) { SettingsNavigation.editUserAgent(this, it) }
            .also { controller = it }
        val page = PageScaffold(activity, getString(R.string.ref_ua_title), ::closePage, appliesWindowInsets)
        page.addIconAction(R.drawable.ref_ic_add, getString(R.string.ref_ua_new)) { SettingsNavigation.editUserAgent(this, 0) }
        NativeToolbarTap.bind(page.toolbar) { list.view.smoothScrollToPosition(0) }
        page.showContent(list.view, bottomInsets = list.view)
        return page.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentFragmentManager.setFragmentResultListener(SettingsNavigation.USER_AGENT_EDITED, viewLifecycleOwner) { _, result ->
            controller?.edited(result.getInt(SettingsNavigation.USER_AGENT_ID))
        }
        controller!!.render()
    }
    override fun consumeBack() = controller?.back() == true
    override fun onDestroyView() {
        controller?.close(); controller = null
        repository?.close(); repository = null
        super.onDestroyView()
    }
}
