package moe.pure.browser.feature.passwords

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.webkit.HttpAuthHandler
import android.webkit.WebView
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.passwords.auth.DeviceAuthentication
import moe.pure.browser.feature.passwords.data.PasswordIgnoredSites
import moe.pure.browser.feature.passwords.data.PasswordPreferences
import moe.pure.browser.feature.passwords.data.PasswordRepository
import moe.pure.browser.feature.passwords.model.PasswordCredential
import moe.pure.browser.feature.passwords.model.PasswordEntry
import moe.pure.browser.feature.passwords.model.PasswordSites
import moe.pure.browser.feature.passwords.ui.HttpAuthenticationDialog
import moe.pure.browser.feature.passwords.ui.PasswordCandidateBar
import moe.pure.browser.feature.passwords.ui.PasswordPicker
import moe.pure.browser.feature.passwords.ui.PasswordSettingsEntry
import moe.pure.browser.feature.passwords.web.PasswordPageScripts
import moe.pure.browser.model.BrowserTab
import moe.pure.browser.ui.components.BrowserMessage
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.ThemedActivity
import java.util.UUID
import java.util.WeakHashMap

/** Coordinates captured forms, device confirmation and the currently selected page. No credentials enter saved state. */
class PasswordController(private val activity: ThemedActivity, private val actions: Actions) {
    interface Actions {
        fun current(): BrowserTab?
        fun toolbar(view: android.view.View?)
    }
    private data class Capture(val url: String, val name: String, val username: String, val password: String) {
        override fun toString() = "Captured password"
    }
    private class HttpRequest(val tab: BrowserTab, val view: WebView, val handler: HttpAuthHandler, val host: String) {
        var dialog: HttpAuthenticationDialog? = null
    }
    private val repository = PasswordRepository(activity)
    private val preferences = PasswordPreferences(activity)
    private val ignored = PasswordIgnoredSites(activity)
    private val authentication = DeviceAuthentication(activity)
    private val picker = PasswordPicker(activity)
    private val main = Handler(Looper.getMainLooper())
    private val messages = BrowserMessage(activity)
    private val documents = WeakHashMap<BrowserTab, Long>()
    private var candidateRequest = 0L
    private var captureRequest = 0L
    private var disposed = false
    private var visible = false
    private var detail: PasswordCredential? = null
    private var http: HttpRequest? = null
    private var saveDialog: BrowserDialog? = null
    private val barDelegate = lazy { PasswordCandidateBar(activity, object : PasswordCandidateBar.Actions {
        override fun candidate(id: String) = fill(id)
        override fun picker() = choose()
        override fun manage() = PasswordSettingsEntry.open(activity)
        override fun copyUsername() { detail?.let { copy(it.entry.username, false) } }
        override fun copyPassword() { detail?.let { copy(it.password, true) } }
        override fun closeDetail() { clearDetail(); focused(actions.current(), true) }
    }) }
    private val bar by barDelegate

    fun navigation(tab: BrowserTab) {
        documents[tab] = (documents[tab] ?: 0) + 1
        if (tab === actions.current()) hide()
        http?.takeIf { it.tab === tab }?.let { cancelHttp(it) }
    }
    fun selected() { hide(); http?.let(::cancelHttp) }

    fun focused(tab: BrowserTab?, show: Boolean) {
        if (disposed || tab == null || tab !== actions.current()) return
        if (!show || tab.home) { hide(); return }
        val request = ++candidateRequest
        val url = tab.url
        background({ repository.count() to repository.candidates(url) }) { (count, entries) ->
            if (request != candidateRequest || tab !== actions.current() || tab.url != url) return@background
            if (count == 0) { hide(); return@background }
            clearDetail(); bar.render(entries); visible = true; actions.toolbar(bar.view)
        }
    }

    /** Accepted form events retain their original page identity while the login navigates onward. */
    fun captured(url: String, username: String, password: String) {
        if (disposed || !preferences.promptToSave || url.isEmpty() || username.isEmpty() || password.isEmpty()) return
        val name = PasswordSites.authority(url)
        if (name.isEmpty()) return
        val capture = Capture(url, name, username, password)
        val request = ++captureRequest
        background({
            if (ignored.contains(name)) false to null else true to repository.find(name, username)?.let(repository::decrypt)
        }) { (allowed, previous) ->
            if (request != captureRequest || !preferences.promptToSave || !allowed) return@background
            if (previous != null && previous.password == password) return@background
            saveDialog?.dismiss()
            if (previous == null) promptSave(capture, request) else promptUpdate(capture, previous.entry, request)
        }
    }

    private fun promptSave(capture: Capture, request: Long) {
        val dialog = BrowserDialog(activity, "保存 ${capture.name} 上的密码吗?").widthDp(268)
            .items(listOf("保存密码", "此网站一律不提示", "取消")) { index ->
                if (request != captureRequest) return@items
                if (index == 0) persist(capture, null)
                else if (index == 1) background({ ignored.add(capture.name) }) { }
            }
        saveDialog = activity.dialogScope.show(dialog) { if (saveDialog === dialog) saveDialog = null }
    }
    private fun promptUpdate(capture: Capture, previous: PasswordEntry, request: Long) {
        val dialog = BrowserDialog(activity, "更新密码", "更新 ${capture.name} 上的密码吗?")
            .positive("确定") { if (request == captureRequest) persist(capture, previous); true }.negative()
        saveDialog = activity.dialogScope.show(dialog) { if (saveDialog === dialog) saveDialog = null }
    }
    private fun persist(capture: Capture, previous: PasswordEntry?) {
        val entry = previous?.copy(url = capture.url) ?: PasswordEntry(UUID.randomUUID().toString(), capture.name,
            capture.url, capture.username, "", 0, 0)
        background({ repository.save(entry, capture.password) }) {
            messages.show(if (previous == null) "密码已保存" else "密码已更新")
            if (visible) focused(actions.current(), true)
        }
    }

    private fun choose() {
        val tab = actions.current() ?: return
        picker.show(tab.url) { id -> if (tab === actions.current()) fill(id) }
    }
    private fun fill(id: String) {
        val tab = actions.current() ?: return
        val view = tab.liveView ?: return
        val document = documents[tab]
        val url = tab.url
        authentication.authenticate("填充密码", "解锁设备来填充密码", onSuccess = {
            if (!current(tab, view, url, document)) return@authenticate
            background({ repository.decrypt(id) }) { credential ->
                if (credential == null || !current(tab, view, url, document)) return@background
                PasswordPageScripts.fill(view, credential.entry.username, credential.password) { filled ->
                    if (current(tab, view, url, document) && !filled) {
                        detail = credential; visible = true
                        bar.showDetail(credential.entry.username, credential.password); actions.toolbar(bar.view)
                    }
                }
            }
        })
    }
    private fun current(tab: BrowserTab, view: WebView, url: String, document: Long?) =
        !disposed && actions.current() === tab && tab.liveView === view && tab.url == url && documents[tab] == document

    fun httpAuthentication(tab: BrowserTab, view: WebView, handler: HttpAuthHandler, host: String) {
        if (disposed || tab !== actions.current() || !view.isShown) { handler.cancel(); return }
        http?.let(::cancelHttp)
        val cached = httpCredentials[host]
        if (handler.useHttpAuthUsernamePassword() && cached != null) { handler.proceed(cached.first, cached.second); return }
        val request = HttpRequest(tab, view, handler, host)
        val url = "https://$host"
        val dialog = HttpAuthenticationDialog(activity, host, { username, password ->
            if (http !== request) return@HttpAuthenticationDialog
            http = null; request.dialog = null
            if (username.isEmpty() || password.isEmpty()) handler.cancel()
            else {
                httpCredentials[host] = username to password
                handler.proceed(username, password)
                captured(url, username, password)
            }
        }, { cancelHttp(request) }, { id -> selectHttp(request, id) }, {
            picker.show(url) { id -> if (http === request) selectHttp(request, id) }
        })
        request.dialog = dialog; http = request; dialog.show()
        background({ repository.count() to repository.candidates(url) }) { (count, entries) ->
            if (http === request) dialog.setCandidates(entries, libraryAvailable = count > 0)
        }
    }
    private fun selectHttp(request: HttpRequest, id: String) {
        authentication.authenticate("填充密码", "解锁设备来填充密码", onSuccess = {
            if (http !== request || actions.current() !== request.tab) return@authenticate
            background({ repository.decrypt(id) }) { credential ->
                if (http === request && actions.current() === request.tab && credential != null)
                    request.dialog?.setCredentials(credential.entry.username, credential.password)
            }
        })
    }
    private fun cancelHttp(request: HttpRequest) {
        if (http !== request) return
        http = null; request.dialog?.close(); request.dialog = null; request.handler.cancel()
    }

    private fun copy(text: String, sensitive: Boolean) {
        val clip = ClipData.newPlainText(null, text)
        if (sensitive) clip.description.extras = android.os.PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
        activity.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
        messages.show("已复制文本")
    }
    private fun clearDetail() { detail = null; if (visible) bar.clearDetail() }
    private fun hide() { candidateRequest++; clearDetail(); visible = false; actions.toolbar(null) }
    fun resume() { if (visible) focused(actions.current(), true) }
    fun backgrounded() { clearDetail() }
    fun rebind() { bar.refreshTheme(); actions.toolbar(if (visible) bar.view else null) }
    fun back(): Boolean = picker.back()
    fun result(request: Int, result: Int, data: Intent?): Boolean = authentication.onActivityResult(request, result)

    private fun <T> background(work: () -> T, done: (T) -> Unit) {
        if (disposed) return
        BrowserExecutors.persistence.execute {
            val result = runCatching(work)
            main.post { if (!disposed) result.onSuccess(done).onFailure { messages.show("操作失败") } }
        }
    }
    fun dispose() {
        disposed = true; candidateRequest++; captureRequest++
        hide(); saveDialog?.dismiss(); saveDialog = null; http?.let(::cancelHttp)
        picker.dispose(); authentication.dispose(); messages.close(); main.removeCallbacksAndMessages(null)
        if (barDelegate.isInitialized()) bar.dispose()
        BrowserExecutors.persistence.execute { repository.close(); ignored.close() }
    }

    companion object {
        private val httpCredentials = mutableMapOf<String, Pair<String, String>>()
    }
}
