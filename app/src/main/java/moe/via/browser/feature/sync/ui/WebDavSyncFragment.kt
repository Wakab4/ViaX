package moe.via.browser.feature.sync.ui

import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import moe.via.browser.R
import moe.via.browser.feature.settings.SettingRow
import moe.via.browser.feature.sync.model.SyncData
import moe.via.browser.feature.sync.model.SyncMode
import moe.via.browser.feature.sync.model.SyncReport
import moe.via.browser.ui.dialog.BrowserDialog

class WebDavSyncFragment : SyncChildFragment() {
    private var adapter: WebDavRows? = null
    private var lastManual = 0L
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = WebDavRows(requireActivity()); list!!.adapter = adapter; render()
    }
    private fun render() {
        val profile = repository.webDav()
        val configured = profile?.configured == true
        val title = if (!configured || profile!!.username.isEmpty()) getString(R.string.ref_sync_page_webdav) else {
            val user = profile.username
            getString(R.string.ref_sync_page_webdav_user, if (user.length > 3) user.take(3) + "*".repeat(user.length - 3) else user)
        }
        val detail = when {
            !configured -> getString(R.string.ref_sync_page_no_configuration)
            profile!!.lastSuccessAt == 0L -> getString(R.string.ref_sync_page_never)
            else -> getString(R.string.ref_sync_page_synced_time, relative(profile.lastSuccessAt))
        }
        val flags = profile?.flags ?: 0
        val data = DATA_ORDER.filter { flags and it.bit != 0 }.joinToString(getString(R.string.ref_sync_page_separator), transform = ::dataName)
        adapter?.submit(WebDavRows.Account(title, detail, configured, ::configuration, { sync(SyncMode.MERGE) }, ::manual),
            getString(R.string.ref_sync_page_automatic),
            SettingRow.Toggle(getString(R.string.ref_sync_page_automatic), profile?.autoSync ?: false, enabled = configured) { enabled ->
                repository.setWebDavFlags(if (enabled) flags or 1 else flags and 1.inv()); render()
            },
            SettingRow.Link(getString(R.string.ref_sync_page_data), data.ifEmpty { getString(R.string.ref_sync_page_none) }, configured) { chooseData(SyncMode.MERGE) },
            SettingRow.Link(getString(R.string.ref_sync_page_manual), enabled = configured, onClick = ::manual))
    }
    private fun relative(timestamp: Long): CharSequence {
        val now = System.currentTimeMillis()
        return if (now - timestamp < 60000L) getString(R.string.ref_script_page_just_now)
            else DateUtils.getRelativeTimeSpanString(timestamp, now, 60000, 65552)
    }
    private fun configuration() {
        val window = WebDavConfigurationDialog(requireActivity(), repository.webDav(), { fields ->
            local({ repository.saveWebDav(fields.address, fields.username, fields.password.takeIf(String::isNotEmpty), fields.digest, fields.path) }) { render() }
        }, { fields ->
            local({ repository.removeWebDav(fields.address.trim { it <= ' ' }, fields.username.trim { it <= ' ' }, fields.digest, fields.path.trim { it <= ' ' }) }) { render() }
        })
        dialogs.show(window)
    }
    private fun chooseData(after: SyncMode) {
        val profile = repository.webDav() ?: return
        val selected = DATA_ORDER.indices.filterTo(linkedSetOf()) { profile.flags and DATA_ORDER[it].bit != 0 }
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_sync_page_data)).multipleChoice(DATA_ORDER.map(::dataName), selected)
            .negative(getString(android.R.string.cancel)).positive(getString(android.R.string.ok)) { dialog ->
                val mask = dialog.selectedIndices.fold(0) { value, index -> value or DATA_ORDER[index].bit }
                repository.setWebDavFlags(profile.flags and 112.inv() or mask); render()
                if (mask > 0) sync(after)
                true
            })
    }
    private fun manual() {
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_sync_page_manual)).items(listOf(
            R.string.ref_sync_page_push, R.string.ref_sync_page_pull, R.string.ref_sync_page_merge).map(::getString)) { index ->
            when (index) {
                0 -> confirm(R.string.ref_sync_page_warning, R.string.ref_sync_page_push_confirm) { sync(SyncMode.PUSH_LOCAL) }
                1 -> confirm(R.string.ref_sync_page_warning, R.string.ref_sync_page_pull_confirm) { sync(SyncMode.PULL_REMOTE) }
                2 -> sync(SyncMode.MERGE)
            }
        })
    }
    private fun sync(mode: SyncMode) {
        val profile = repository.webDav() ?: return
        val now = System.currentTimeMillis()
        if (now - lastManual < 1000) return
        lastManual = now
        if (profile.data.isEmpty()) { chooseData(mode); return }
        request<SyncReport>({ done -> manager.sync(mode = mode, force = true, complete = done) }) { report ->
            if (!report.cancelled) {
                if (report.success) { message(R.string.ref_sync_page_complete); render() }
                else {
                    val errors = report.error?.let(::listOf) ?: report.errors.map { it.error.orEmpty() }
                    dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_sync_page_failed), errors.joinToString("\n").trim { it <= ' ' })
                        .positive(getString(android.R.string.ok)))
                }
            }
        }
    }
    private fun dataName(value: SyncData) = getString(when (value) {
        SyncData.FAVORITES -> R.string.ref_sync_page_favorites; SyncData.BOOKMARKS -> R.string.ref_sync_page_bookmarks; SyncData.SETTINGS -> R.string.ref_sync_page_settings
    })
    override fun onDestroyView() { adapter = null; super.onDestroyView() }
    companion object { private val DATA_ORDER = listOf(SyncData.FAVORITES, SyncData.BOOKMARKS, SyncData.SETTINGS) }
}
