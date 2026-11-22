package moe.via.browser.browser.ui.menu.customize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.via.browser.R
import moe.via.browser.browser.ui.menu.MenuPreferences
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.ui.components.NativeToolbarTap
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.theme.ThemedActivity

class MenuCustomizationFragment : SettingsRecordPage() {
    private var model: MenuCustomizationModel? = null
    private var list: MenuCustomizationList? = null
    private var dialogs: DialogScope? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val value = MenuCustomizationModel(MenuPreferences(activity.getSharedPreferences("browser", android.content.Context.MODE_PRIVATE)))
        val content = MenuCustomizationList(activity, value)
        model = value; list = content; dialogs = DialogScope()
        return PageScaffold(activity, getString(R.string.ref_menu_customize), ::closePage, appliesWindowInsets).also { page ->
            page.addTextAction(getString(R.string.ref_menu_reset)) {
                dialogs?.show(BrowserDialog(activity, getString(R.string.ref_menu_reset_title), getString(R.string.ref_menu_reset_message))
                    .negative(getString(android.R.string.cancel)).positive(getString(R.string.ref_menu_reset)) { value.reset(); content.refresh(); true })
            }
            page.showContent(content.view, content.view); NativeToolbarTap.bind(page.toolbar) { content.view.smoothScrollToPosition(0) }
        }.root
    }
    override fun onHiddenChanged(hidden: Boolean) { super.onHiddenChanged(hidden); if (hidden) model?.save() }
    override fun onPause() { model?.save(); super.onPause() }
    override fun onDestroyView() { dialogs?.dispose(); dialogs = null; list?.close(); list = null; model = null; super.onDestroyView() }
}
