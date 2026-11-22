package moe.via.browser.feature.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.via.browser.R
import moe.via.browser.feature.about.ui.AboutListPage
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.ui.theme.ThemedActivity

/** hb.j4 displays the 21 project rows directly, with no extra section or separate Activity. */
class LicensesFragment : SettingsRecordPage() {
    private var content: AboutListPage? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        return AboutListPage(activity, getString(R.string.ref_about_page_licenses), appliesWindowInsets, true, ::closePage).also { page ->
            content = page
            page.rows(AboutCatalog.referenceProjects.mapIndexed { index, project ->
                SettingsItem.link(index, project.title, project.license) { AboutLinks.open(activity, project.url) }
            })
        }.root
    }
    override fun onDestroyView() { content?.close(); content = null; super.onDestroyView() }
}
