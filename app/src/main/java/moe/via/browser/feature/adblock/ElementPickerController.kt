package moe.via.browser.feature.adblock

import moe.via.browser.ui.message.BrowserMessages

import android.app.Activity
import android.widget.FrameLayout
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope

class ElementPickerController(private val activity: Activity, parent: () -> FrameLayout,
    private val page: (String) -> android.webkit.WebView?, private val closeChild: () -> Unit) {
    private var tab: BrowserTab? = null
    val isActive: Boolean get() = tab != null
    private var view: android.webkit.WebView? = null
    private var prompt: BrowserDialog? = null
    private val dialogs = DialogScope()
    private var markerTabId = ""
    private val panel = ElementPickerPanel(activity, parent, object : ElementPickerPanel.Actions {
        override fun parent() = command("window.__markParent()")
        override fun child() = command("window.__markChild()")
        override fun save() = command("window.__getMarkerFilter()")
        override fun cancel() { close() }
    })
    fun show(current: BrowserTab) {
        close()
        val web = current.webView
        if (!web.settings.javaScriptEnabled) { BrowserMessages.show(activity, "仅在允许 JavaScript 运行时支持"); return }
        if (!current.url.startsWith("http://") && !current.url.startsWith("https://")) return
        tab = current; view = web; markerTabId = current.id
        panel.show()
        ElementPickerBridge.start(web)
    }
    private fun command(code: String) { view?.let { ElementPickerBridge.command(it, code) } }
    fun update(selector: String) { if (isActive) panel.update(selector) }
    fun confirm(host: String, selector: String) {
        val dialog = BrowserDialog(activity, "标记广告", "确定将其标记为广告？($selector)").negative().positive("确定") {
            BrowserExecutors.persistence.execute {
                runCatching { CustomBlockRules(activity.applicationContext).addElement(host, selector) }
                    .onFailure { moe.via.browser.core.logging.NativeLog.warn("ElementPicker", "Save element rule", it) }
            }
            closeChild()
            page(markerTabId)?.let { ElementPickerBridge.hide(it, selector) }
            true
        }
        prompt = dialog
        dialogs.show(dialog) { if (prompt === dialog) prompt = null }
    }
    fun onSelected(current: BrowserTab) { if (tab != null && tab !== current) close() }
    fun navigation(source: android.webkit.WebView) { if (view === source) close() }
    fun rebind() { panel.rebind(); prompt?.updateGeometry() }
    fun close(): Boolean {
        val showing = tab != null
        prompt?.dismiss(); prompt = null
        view?.let(ElementPickerBridge::stop); panel.close(); view = null; tab = null
        return showing
    }
    fun dispose() { close(); dialogs.dispose() }
}
