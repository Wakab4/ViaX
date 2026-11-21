package moe.pure.browser.feature.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.R
import moe.pure.browser.feature.about.diagnostics.ui.DiagnosticsEntry
import moe.pure.browser.feature.about.ui.AboutHeader
import moe.pure.browser.feature.about.ui.AboutHeaderView
import moe.pure.browser.feature.about.ui.AboutListPage
import moe.pure.browser.feature.settings.SettingsItem
import moe.pure.browser.feature.settings.pages.SettingsRecordPage
import moe.pure.browser.ui.pages.RecordPageScope
import moe.pure.browser.ui.theme.ThemedActivity

/** About identity and its sole open source licenses action. */
class AboutFragment : SettingsRecordPage() {
    private var content: AboutListPage? = null
    @Suppress("DEPRECATION")
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val header = AboutHeader(getString(R.string.ref_about_page_name),
            activity.packageManager.getPackageInfo(activity.packageName, 0).versionName.orEmpty())
        return AboutListPage(activity, getString(R.string.ref_about_page_title), appliesWindowInsets, false, ::closePage).also { page ->
            content = page
            page.rows(listOf(SettingsItem.content("about_header", header) {
                AboutHeaderView(activity, header) { DiagnosticsEntry.open(activity) }
            }, SettingsItem.link(1, getString(R.string.ref_about_page_licenses)) {
                RecordPageScope.of(this@AboutFragment)?.push(LicensesFragment::class.java, null)
            }))
        }.root
    }
    override fun onDestroyView() {
        content?.close(); content = null
        super.onDestroyView()
    }
}
