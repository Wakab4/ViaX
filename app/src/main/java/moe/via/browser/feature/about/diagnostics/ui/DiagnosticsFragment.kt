package moe.via.browser.feature.about.diagnostics.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.ui.theme.ThemedActivity

class DiagnosticsFragment : SettingsRecordPage() {
    private var controller: DiagnosticsPageController? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DiagnosticsPageController(requireActivity() as ThemedActivity, appliesWindowInsets, ::closePage)
            .also { controller = it }.view
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); controller!!.load() }
    override fun onDestroyView() { controller?.close(); controller = null; super.onDestroyView() }
}
