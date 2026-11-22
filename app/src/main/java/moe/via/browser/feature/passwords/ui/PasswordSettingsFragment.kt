package moe.via.browser.feature.passwords.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.via.browser.R
import moe.via.browser.feature.passwords.data.PasswordPreferences
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.ui.SettingsRowAdapter
import moe.via.browser.ui.pages.RecordPageScope
import moe.via.browser.ui.theme.ThemedActivity

class PasswordSettingsFragment : PasswordRecordPage() {
    private val preferences by lazy { PasswordPreferences(requireContext()) }
    private val documents = PasswordCsvDocuments(this, { repository }, { authentication }, ::message)
    private var content: PasswordListPage? = null
    private var adapter: SettingsRowAdapter? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        return PasswordListPage(activity, getString(R.string.ref_password_csv_settings_title), appliesWindowInsets, ::closePage).also {
            content = it; adapter = SettingsRowAdapter(activity, ::render); it.list.adapter = adapter
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); documents.bindView(); render() }
    private fun render() {
        adapter?.submit(listOf(
            SettingsItem.toggle(1, getString(R.string.ref_password_csv_save_prompt), preferences.promptToSave) { preferences.promptToSave = it },
            SettingsItem.link(2, getString(R.string.ref_password_csv_saved)) { RecordPageScope.of(this)?.push(PasswordListFragment::class.java, null) },
            SettingsItem.link(5, getString(R.string.ref_password_csv_exceptions)) { RecordPageScope.of(this)?.push(PasswordExceptionsFragment::class.java, null) },
            SettingsItem.link(3, getString(R.string.ref_password_csv_import), getString(R.string.ref_password_csv_import_description), onClick = documents::importPasswords),
            SettingsItem.link(4, getString(R.string.ref_password_csv_export), getString(R.string.ref_password_csv_export_description), onClick = documents::export)
        ))
    }
    override fun onDestroyView() { documents.releaseView(); content?.close(); content = null; adapter = null; super.onDestroyView() }
    override fun onDestroy() { documents.dispose(); super.onDestroy() }
}
