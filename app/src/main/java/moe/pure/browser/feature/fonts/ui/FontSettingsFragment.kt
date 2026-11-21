package moe.pure.browser.feature.fonts.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.FontPreviewCache
import moe.pure.browser.feature.settings.data.BrowserPreferences
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.components.NativeToolbarTap
import moe.pure.browser.ui.theme.ThemedActivity
import moe.pure.browser.feature.settings.pages.SettingsRecordPage
import moe.pure.browser.ui.documents.NativeDocumentPicker

/** hb.j2 owns picker registration and previews; each view owns its list/import subscriptions. */
class FontSettingsFragment : SettingsRecordPage() {
    private val previews = FontPreviewCache()
    private val picker = NativeDocumentPicker(this)
    private var controller: FontSettingsController? = null

    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val preferences = BrowserPreferences(activity.getSharedPreferences("browser", android.content.Context.MODE_PRIVATE))
        val content = FontSettingsController(activity, preferences, previews).also { controller = it }
        content.view.id = R.id.font_settings_list
        val page = PageScaffold(activity, getString(R.string.ref_font_title), ::closePage, appliesWindowInsets)
        page.addIconAction(R.drawable.ref_ic_add, getString(R.string.ref_font_new), ::chooseFile)
        NativeToolbarTap.bind(page.toolbar) { content.view.smoothScrollToPosition(0) }
        page.showContent(content.view, bottomInsets = content.view)
        return page.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller!!.load()
    }
    fun chooseFile() = picker.open(arrayOf("font/ttf", "font/otf", "font/woff", "font/woff2")) {
        uri -> if (uri != null) controller?.import(uri)
    }
    override fun onDestroyView() {
        controller?.dispose()
        controller = null
        super.onDestroyView()
    }
}
