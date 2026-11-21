package moe.pure.browser.feature.download

import android.os.Bundle
import android.webkit.WebView
import androidx.fragment.app.FragmentActivity
import moe.pure.browser.feature.download.ui.DownloadManagerFragment
import moe.pure.browser.ui.message.BrowserMessages
import moe.pure.browser.web.bridge.NativePageBridge
import moe.pure.browser.web.download.NativeDownloadNames
import moe.pure.browser.ui.BrowserChrome
import moe.pure.browser.ui.theme.ThemedActivity
import moe.pure.browser.feature.download.panel.DownloadPanelController
import androidx.fragment.app.FragmentManager

/** Browser request owner; built-in navigation pushes a complete Record page above the browser. */
class DownloadController(private val activity: ThemedActivity, chrome: () -> BrowserChrome?,
    panelManager: () -> FragmentManager) {
    private val requests = DownloadRequests(activity)
    private val panel = DownloadPanelController(activity, chrome, panelManager())

    fun show(forceBuiltin: Boolean = false) {
        if (!forceBuiltin && requests.managers.openSelected(activity)) return
        if (panel.eligible) { panel.show(); return }
        activity.pageStack.push(DownloadManagerFragment::class.java, null)
    }

    fun confirm(url: String = "https://", userAgent: String? = null, disposition: String? = null, mimeType: String? = null,
        referer: String? = null, length: Long = -1) =
        requests.confirm(DownloadDraft(url, userAgent, disposition, mimeType, referer, manual = url == "https://", length = length))

    fun chooseManager() = requests.managers.choose(activity)

    fun fromScript(view: WebView?, url: String, name: String, userAgent: String?) = begin(view,
        DownloadDraft(url, userAgent = userAgent, disposition = "attachment", mime = if (name.lastIndexOf('.') < 0) null else DownloadNames.mime(name),
            name = name, length = 0, overwriteName = name.isEmpty()))

    fun fromPage(view: WebView, data: String?, name: String?, referer: String?) {
        if (data != null) begin(view, DownloadDraft(data, userAgent = view.settings.userAgentString, referer = referer, name = name))
    }

    fun request(view: WebView, url: String, userAgent: String?, disposition: String?, mime: String?, length: Long, referer: String?) {
        begin(view, DownloadDraft(url, userAgent, disposition, mime, referer, length = length))
    }

    fun fromPageUrl(view: WebView?, url: String) = begin(view, DownloadDraft(url, length = 0))

    private fun begin(view: WebView?, draft: DownloadDraft) {
        if (activity.isFinishing || activity.isDestroyed || activity.supportFragmentManager.isStateSaved) return
        val blob = draft.url.startsWith("blob:") && requests.managers.selected != "system"
        val stream = draft.url.startsWith("https://wormhole.app/download-stream/")
        if (blob || stream) {
            if (requests.managers.sendToSelected(activity, draft.url)) return
            if (view == null) { BrowserMessages.show(activity, "仅可下载以 \"http\" 或 \"https\" 开头的网址"); return }
            requests.notifyWebRequest()
            BrowserMessages.show(activity, "处理 blob 数据中…")
            if (blob) NativePageBridge.blob(view, draft.url)
            else {
                NativeDownloadNames.remember(draft.url, DownloadNames.guess(draft))
                NativePageBridge.stream(view, draft.url)
            }
        } else requests.confirm(draft)
    }

    // The shared record stack owns page Back/restoration; this owner retains only browser requests.
    fun close(): Boolean = panel.dismiss()
    fun back(): Boolean = panel.back()
    fun rebind() { panel.rebind() }
    fun hostVisibilityChanged(visible: Boolean) { panel.hostVisibilityChanged(visible) }
    fun save() = Bundle().apply { putBundle("requests", requests.save()) }
    fun restore(bundle: Bundle?) {
        requests.restore(bundle?.getBundle("requests"))
    }
    fun onResume() { requests.onResume(); panel.rebind() }
    fun dispose() { panel.close(); requests.dispose() }
    companion object {
        fun hasRestored(activity: FragmentActivity) = activity is ThemedActivity && DownloadPanelController.existing(activity) != null
    }
}
