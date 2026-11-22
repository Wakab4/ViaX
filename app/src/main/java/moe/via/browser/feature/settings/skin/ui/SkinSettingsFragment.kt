package moe.via.browser.feature.settings.skin.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.via.browser.R
import moe.via.browser.storage.preferences.BrowserPreferences
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.ui.components.NativeToolbarTap
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.documents.NativeDocumentPicker
import moe.via.browser.ui.theme.ThemedActivity

class SkinSettingsFragment : SettingsRecordPage() {
    private val picker = NativeDocumentPicker(this)
    private var controller: SkinSettingsController? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val preferences = BrowserPreferences(activity.getSharedPreferences("browser", android.content.Context.MODE_PRIVATE))
        val content = SkinSettingsController(activity, preferences).also { controller = it }
        val page = PageScaffold(activity, getString(R.string.ref_skin_title), ::closePage, appliesWindowInsets)
        page.addIconAction(R.drawable.ref_ic_add, getString(R.string.ref_skin_import)) {
            picker.open(arrayOf("application/zip")) { uri -> if (uri != null) controller?.import(uri) }
        }
        NativeToolbarTap.bind(page.toolbar) { content.view.smoothScrollToPosition(0) }
        page.showContent(content.view, bottomInsets = content.view)
        return page.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); controller!!.load() }
    override fun onDestroyView() { controller?.dispose(); controller = null; super.onDestroyView() }
}
