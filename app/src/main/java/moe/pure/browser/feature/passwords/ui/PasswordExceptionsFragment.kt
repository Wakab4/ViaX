package moe.pure.browser.feature.passwords.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.R
import moe.pure.browser.feature.passwords.data.PasswordIgnoredSites
import moe.pure.browser.feature.passwords.model.PasswordSites
import moe.pure.browser.feature.settings.SettingsItem
import moe.pure.browser.feature.settings.ui.SettingsRowAdapter
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.ThemedActivity

class PasswordExceptionsFragment : PasswordRecordPage() {
    private val ignored by lazy { PasswordIgnoredSites(requireContext().applicationContext) }
    private var content: PasswordListPage? = null
    private var adapter: SettingsRowAdapter? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        return PasswordListPage(activity, getString(R.string.ref_password_csv_exceptions), appliesWindowInsets, ::closePage).also {
            content = it; adapter = SettingsRowAdapter(activity) {}; it.list.adapter = adapter
            it.page.addIconAction(R.drawable.ref_ic_add, getString(R.string.ref_search_page_new)) { edit(null) }
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); render() }
    private fun render() {
        val sites = ignored.all().sortedWith(PasswordSites.siteOrder)
        adapter?.submit(sites.map { site -> SettingsItem.link(site.hashCode(), site) { edit(site) } })
        content?.empty(sites.isEmpty())
    }
    private fun edit(old: String?) {
        val dialog = BrowserDialog(requireActivity(), getString(if (old == null) R.string.ref_search_page_new else R.string.ref_search_page_edit))
        val field = dialog.field(old.orEmpty(), "www.example.com")
        dialog.positive(getString(android.R.string.ok)) {
            val site = PasswordSites.normalizeEditor(field.text.toString())
            if (site.isEmpty()) return@positive false
            if (site != old) { if (old != null) ignored.remove("https://$old"); ignored.add("https://$site"); render() }
            true
        }.negative()
        if (old != null) dialog.neutral(getString(R.string.ref_search_page_delete)) { ignored.remove("https://$old"); render(); true }
        dialogs.show(dialog)
    }
    override fun onDestroyView() { content?.close(); content = null; adapter = null; super.onDestroyView() }
}
