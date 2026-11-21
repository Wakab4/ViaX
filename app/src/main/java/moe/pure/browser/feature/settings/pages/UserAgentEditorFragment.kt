package moe.pure.browser.feature.settings.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.R
import moe.pure.browser.feature.settings.navigation.SettingsNavigation
import moe.pure.browser.feature.useragent.ui.UserAgentEditorController
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.pages.RecordPageFragment
import moe.pure.browser.ui.theme.ThemedActivity

class UserAgentEditorFragment : SettingsRecordPage() {
    private var editor: UserAgentEditorController? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val id = requireArguments().getInt(SettingsNavigation.USER_AGENT_ID)
        val controller = UserAgentEditorController(activity, id, savedInstanceState?.getBundle("editor")) { updated ->
            if (updated != null && updated > 0) parentFragmentManager.setFragmentResult(SettingsNavigation.USER_AGENT_EDITED,
                Bundle().apply { putInt(SettingsNavigation.USER_AGENT_ID, updated) })
            if (!parentFragmentManager.isStateSaved) {
                if (parentFragmentManager.backStackEntryCount == 0) closePage() else parentFragmentManager.popBackStack()
            }
        }.also { editor = it }
        val page = PageScaffold(activity, controller.title, ::closePage, appliesWindowInsets)
        page.addTextAction(getString(R.string.ref_ua_save), controller::save)
        page.showContent(controller.view)
        return page.root
    }
    override fun consumeBack() = editor?.back() == true
    override fun onResume() { super.onResume(); editor?.resume() }
    override fun onPause() { editor?.pause(); super.onPause() }
    override fun onSaveInstanceState(outState: Bundle) {
        editor?.saveState()?.let { outState.putBundle("editor", it) }
        super.onSaveInstanceState(outState)
    }
    override fun onDestroyView() { editor?.close(retain = requireActivity().isChangingConfigurations); editor = null; super.onDestroyView() }
}
