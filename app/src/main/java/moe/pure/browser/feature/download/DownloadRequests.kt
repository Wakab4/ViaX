package moe.pure.browser.feature.download

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import moe.pure.browser.ui.message.BrowserMessages
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.download.actions.DownloadDialogOwner
import moe.pure.browser.feature.download.permissions.DownloadPermissionFragment
import moe.pure.browser.feature.download.permissions.DownloadPermissionNotice
import moe.pure.browser.feature.download.workflow.DownloadContinuation
import moe.pure.browser.feature.download.ui.ManualDownloadPrompt
import moe.pure.browser.feature.download.directory.DownloadDirectoryFragment

/** Confirmation and destination recovery are independent of the transfer queue and list actions. */
class DownloadRequests(private val activity: FragmentActivity, private val fragments: FragmentManager = activity.supportFragmentManager,
    private val visible: () -> Boolean = { true }) {
    val managers = DownloadManagers(activity)
    private val queue get() = DownloadCoordinator.get(activity)
    private val dialogs = DownloadDialogOwner(activity)
    private var prompt: WebDownloadPrompt? = null
    private var manualPrompt: ManualDownloadPrompt? = null
    private var disposed = false
    private var restoredWeb: DownloadDraft? = null
    private var restoredManual: DownloadDraft? = null
    private var permissionFragment: DownloadPermissionFragment? = null
    private val deniedDelegate = lazy { DownloadPermissionNotice(activity) }
    private val denied by deniedDelegate
    private val permissionHost = object : DownloadPermissionFragment.Host {
        override fun ready() = canPresent()
        override fun storageResolved(work: DownloadContinuation, allowed: Boolean) {
            if (disposed) return
            val request = work as? DownloadContinuation.Draft ?: return
            if (allowed) { if (request.approved) start(request.value) else confirm(request.value) }
            else { discard(request.value.payload); denied.show() }
        }
    }
    private var savedForRestore = false
    private val preparing = mutableSetOf<DownloadControl>()
    private val handingOff = mutableSetOf<String>()
    private val payloads by lazy { DownloadPayloads.get(activity.applicationContext) }
    private val directory = DownloadDirectoryDialog(activity, DownloadDirectoryFragment.REQUESTS,
        changed = { work -> (work as? DownloadContinuation.Draft)?.let { if (it.approved) start(it.value) else confirm(it.value) } },
        cancelled = { work -> discard(work.payload()) }, fragments = fragments, visible = ::canPresent)
    fun confirm(draft: DownloadDraft) {
        if (!canPresent()) { discard(draft.payload); return }
        if (!draft.manual && managers.sendToSelected(activity, draft.url)) return
        if (!draft.manual) notifyWebRequest()
        if (draft.payload == null && draft.url.startsWith("data:", true) && managers.selected != "system") {
            prepareData(draft); return
        }
        if (!draft.manual && !directory.directories.hasAccess()) { recover(draft, false); return }
        if (!draft.manual && needsStoragePermission()) { permissions().requestStorage(DownloadContinuation.Draft(draft, false)); return }
        if (!draft.manual) {
            val dialog = WebDownloadPrompt(activity, draft, ::start)
            prompt = dialog
            dialogs.show(dialog) {
                if (prompt === dialog) prompt = null
                if (!dialog.approvedDownload && (dialog.cancelledByUser || !savedForRestore)) discard(draft.payload)
            }
            return
        }
        val manual = ManualDownloadPrompt(activity, draft, ::start)
        manualPrompt = manual
        dialogs.show(manual.dialog) { if (manualPrompt === manual) manualPrompt = null }
    }
    private fun start(draft: DownloadDraft) {
        if (!canPresent()) { discard(draft.payload); return }
        if (!directory.directories.hasAccess()) { recover(draft, true); return }
        if (needsStoragePermission()) { permissions().requestStorage(DownloadContinuation.Draft(draft, true)); return }
        draft.payload?.let(handingOff::add)
        val headers = buildMap {
            draft.userAgent?.takeIf(String::isNotBlank)?.let { put("User-Agent", it) }
            draft.referer?.takeIf(String::isNotBlank)?.let { put("Referer", it) }
            draft.authorization?.takeIf(String::isNotBlank)?.let { put("Authorization", it) }
        }
        if (!draft.manual && managers.selected == "system") {
            runCatching { managers.enqueueSystem(draft.url, draft.name!!, draft.mime!!, headers) }
                .onSuccess { toast("已开始下载") }.onFailure { toast("下载失败") }
            draft.payload?.let(handingOff::remove); discard(draft.payload)
        } else {
            val owner = WeakReference(this)
            val transfers = queue
            val store = payloads
            val token = draft.payload
            transfers.enqueue(draft.url, draft.name!!, draft.mime!!, headers, token, draft.length) { result ->
                val current = owner.get()
                token?.let { current?.handingOff?.remove(it) }
                result.onSuccess { id ->
                    transfers.requestStart(id)
                    if (current?.canReport() == true) current.toast("已开始下载")
                }.onFailure { error ->
                    token?.let { payload -> BrowserExecutors.persistence.execute { store.removeUnused(payload) } }
                    if (current?.canReport() == true) current.toast("下载失败：${error.message}")
                }
            }
        }
    }
    private fun needsStoragePermission() = Build.VERSION.SDK_INT <= 28 && directory.directories.tree == null &&
        activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
    private fun permissions(): DownloadPermissionFragment = permissionFragment ?: DownloadPermissionFragment.obtain(fragments).also {
        permissionFragment = it; it.bind(permissionHost)
    }
    private fun canReport() = !disposed && !activity.isFinishing && !activity.isDestroyed
    private fun canPresent() = canReport() && !fragments.isDestroyed && !fragments.isStateSaved && visible()
    fun notifyWebRequest() {
        if (canPresent() && Build.VERSION.SDK_INT >= 33 &&
            activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions().requestNotifications()
    }
    private fun bindRestoredPermissions() {
        DownloadPermissionFragment.existing(fragments)?.let { permissionFragment = it; it.bind(permissionHost) }
    }
    private fun recover(draft: DownloadDraft, approved: Boolean) {
        directory.permissionRevoked(DownloadContinuation.Draft(draft, approved))
    }
    private fun prepareData(draft: DownloadDraft) {
        val control = DownloadControl(); preparing.add(control)
        val store = payloads
        val owner = WeakReference(this)
        val main = Handler(Looper.getMainLooper())
        BrowserExecutors.network.execute {
            val result = runCatching { control.check(); store.fromDataUri(draft.url, control) }
            main.post {
                val current = owner.get()
                current?.preparing?.remove(control)
                result.onSuccess { payload ->
                    if (current?.canReport() != true || control.requestedState != null)
                        BrowserExecutors.persistence.execute { store.removeUnused(payload.token) }
                    else {
                        val ready = draft.copy(url = "data:${payload.mime};base64,", mime = payload.mime, length = payload.size, payload = payload.token)
                        if (current.canPresent()) current.confirm(ready) else current.defer(ready)
                    }
                }.onFailure { if (current?.canReport() == true && control.requestedState == null) current.toast("页面下载失败：${it.message}") }
            }
        }
    }
    private fun discard(token: String?) {
        if (token != null && token !in handingOff) { val store = payloads; BrowserExecutors.persistence.execute { store.removeUnused(token) } }
    }
    private fun defer(draft: DownloadDraft) {
        val previous = if (draft.manual) restoredManual else restoredWeb
        previous?.payload?.takeIf { it != draft.payload }?.let(::discard)
        if (draft.manual) restoredManual = draft else restoredWeb = draft
    }
    private fun presentRestored() {
        if (!canPresent()) return
        val web = restoredWeb; restoredWeb = null; web?.let(::confirm)
        val manual = restoredManual; restoredManual = null; manual?.let(::confirm)
    }
    fun save() = Bundle().apply {
        savedForRestore = true
        (prompt?.takeUnless { it.approvedDownload }?.snapshot() ?: restoredWeb)?.let { putBundle("prompt", it.save()) }
        (manualPrompt?.takeUnless { it.approvedDownload }?.snapshot() ?: restoredManual)?.let { putBundle("manual_prompt", it.save()) }
    }
    fun restore(bundle: Bundle?) {
        bundle?.getBundle("prompt")?.let(DownloadDraft::restore)?.let(::defer)
        bundle?.getBundle("manual_prompt")?.let(DownloadDraft::restore)?.let(::defer)
        activity.window.decorView.post { presentRestored() }
        bindRestoredPermissions()
        directory.onResume()
    }
    fun onResume() { savedForRestore = false; bindRestoredPermissions(); directory.onResume(); presentRestored() }
    fun dispose() {
        disposed = true; permissionFragment?.release(permissionHost); permissionFragment = null
        if (deniedDelegate.isInitialized()) denied.close()
        preparing.forEach(DownloadControl::cancel); preparing.clear()
        if (!savedForRestore) { restoredWeb?.payload?.let(::discard); restoredManual?.payload?.let(::discard) }
        restoredWeb = null; restoredManual = null
        dialogs.close(); directory.dispose()
    }
    private fun toast(text: String) = BrowserMessages.show(activity, text)
}
