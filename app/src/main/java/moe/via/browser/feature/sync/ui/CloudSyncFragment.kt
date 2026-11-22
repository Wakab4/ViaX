package moe.via.browser.feature.sync.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import moe.via.browser.R
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.ui.SettingsRowAdapter
import moe.via.browser.feature.sync.model.CloudBookmarkTooLargeException
import moe.via.browser.feature.sync.model.CloudLoginResult
import moe.via.browser.feature.sync.model.SyncCall
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.theme.ThemedActivity

/** hb.u keeps the cloud page and its actions independent of the WebDAV child. */
class CloudSyncFragment : SyncChildFragment() {
    private var adapter: SettingsRowAdapter? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = SettingsRowAdapter(requireActivity()) {}; list!!.itemAnimator = null; list!!.adapter = adapter; render()
    }
    private fun render() {
        val account = repository.cloudAccount()
        adapter?.submit(buildList {
            if (account.loggedIn) {
                add(SettingsItem.link(2, account.username) {
                    dialogs.show(BrowserDialog(requireActivity(), account.username, getString(R.string.ref_sync_page_logout))
                        .negative(getString(android.R.string.cancel)).positive(getString(android.R.string.ok)) { repository.logoutCloud(); render(); true })
                })
                add(SettingsItem.link(4, getString(R.string.ref_sync_page_download)) {
                    confirm(R.string.ref_sync_page_download, R.string.ref_sync_page_download_confirm, ::download)
                })
                add(SettingsItem.link(5, getString(R.string.ref_sync_page_upload)) {
                    confirm(R.string.ref_sync_page_upload, R.string.ref_sync_page_upload_confirm) { upload() }
                })
                add(SettingsItem.link(6, getString(R.string.ref_sync_page_delete), getString(R.string.ref_sync_page_delete_detail), onClick = ::delete))
            } else add(SettingsItem.link(1, getString(R.string.ref_sync_page_login), onClick = ::loginDialog))
            add(SettingsItem.link(3, getString(R.string.ref_sync_page_server), resources.getStringArray(R.array.ref_sync_page_servers)[account.serverIndex]) {
                dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_sync_page_server), getString(R.string.ref_sync_page_server_message))
                    .singleChoice(resources.getStringArray(R.array.ref_sync_page_servers).toList(), account.serverIndex) { index ->
                        if (index != account.serverIndex) { repository.selectCloudServer(index); render() }
                    })
            })
        })
    }
    private fun loginDialog() {
        val account = repository.cloudAccount()
        val window = CloudLoginDialog(requireActivity() as ThemedActivity, account.serverIndex, account.username) { user, password ->
            waiting<Result<CloudLoginResult>>({ done -> manager.loginCloud(user, password, done) }) { result ->
                result.onSuccess { if (it.registered) message(R.string.ref_sync_page_registered) }
                    .onFailure { message(if (it.message == "密码错误。") R.string.ref_sync_page_password_wrong else R.string.ref_sync_page_server_error) }
                render()
            }
        }
        dialogs.show(window.dialog)
    }
    private fun upload(oversize: Boolean = false) {
        waiting<Result<Unit>>({ done -> manager.uploadCloud(allowOversizeBookmarks = oversize, complete = done) }) { result ->
            result.onSuccess { notice(R.string.ref_sync_page_notice, R.string.ref_sync_page_upload_success) }
                .onFailure { error ->
                    if (error is CloudBookmarkTooLargeException) confirm(R.string.ref_sync_page_upload, R.string.ref_sync_page_oversize) { upload(true) }
                    else notice(R.string.ref_sync_page_notice, R.string.ref_sync_page_server_error)
                }
        }
    }
    private fun download() {
        waiting<Result<Int>>({ done -> manager.downloadCloud(done) }) { result ->
            notice(R.string.ref_sync_page_download, if (result.isSuccess) R.string.ref_sync_page_download_success else R.string.ref_sync_page_server_error)
        }
    }
    private fun delete() {
        val field = SyncFields.field(requireActivity(), getString(R.string.ref_sync_page_password), secret = true)
        val body = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; SyncFields.append(this, field) }
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_sync_page_delete), getString(R.string.ref_sync_page_delete_message)).custom(body)
            .neutral(getString(android.R.string.cancel)).positive(getString(R.string.ref_sync_page_delete_confirm)) {
                val password = field.text.toString()
                if (password.isEmpty()) message(R.string.ref_sync_page_password_required) else {
                    waiting<Result<Unit>>({ done -> manager.deleteCloudAccount(password, done) }) { result ->
                        result.onSuccess { notice(R.string.ref_sync_page_delete, R.string.ref_sync_page_delete_success) }
                            .onFailure { message(if (it.message == "密码错误。") R.string.ref_sync_page_password_wrong else R.string.ref_sync_page_server_error) }
                        render()
                    }
                }
                true
            })
    }
    private fun notice(title: Int, text: Int) {
        dialogs.show(BrowserDialog(requireActivity(), getString(title), getString(text)).positive(getString(android.R.string.ok)))
    }
    private fun <T> waiting(start: ((T) -> Unit) -> SyncCall, completed: (T) -> Unit) {
        val wait = dialogs.show(CloudWaitDialog.create(requireActivity()))
        request(start) { value -> wait.dismiss(); completed(value) }
    }
    override fun onDestroyView() { adapter = null; super.onDestroyView() }
}
