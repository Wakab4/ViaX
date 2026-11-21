package moe.pure.browser.feature.translation

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import moe.pure.browser.core.AddressResolver
import moe.pure.browser.feature.translation.ui.TranslationPanel
import moe.pure.browser.model.BrowserTab
import moe.pure.browser.ui.chrome.ChromeLayoutPolicy
import moe.pure.browser.ui.chrome.ChromeMode
import moe.pure.browser.ui.components.BrowserMessage
import moe.pure.browser.ui.components.ContentOverlay
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.ThemedActivity

/** Menu, page injection and the text panel share no persistent page or speech state. */
class TranslationController(private val activity: ThemedActivity, private val parent: () -> FrameLayout,
    private val current: () -> BrowserTab?, private val open: (String) -> Unit) {
    private data class Provider(val id: Int, val title: String)
    private val messages = BrowserMessage(activity)
    private val preferences = TranslationPreferences(activity)
    private var panel: TranslationPanel? = null
    private var overlay: ContentOverlay? = null
    private var rebinding = false
    private var disposed = false

    fun translatePage(tab: BrowserTab) {
        if (!network(tab.url)) return
        val google = moe.pure.browser.feature.search.data.SearchPreferences(activity).engineId == -1
        inject(tab, edge = preferences.useEdgeByDefault(if (google) -1 else 0))
    }

    fun showProviders(pageInjection: Boolean = true) {
        val tab = current() ?: return
        if (!network(tab.url)) return
        val choices = buildList {
            if (china() && pageInjection) add(Provider(6, "微软翻译"))
            if (china()) add(Provider(1, "百度翻译"))
            if (pageInjection) add(Provider(4, "谷歌页内翻译"))
            add(Provider(3, "谷歌翻译")); add(Provider(5, "谷歌文本翻译"))
        }
        activity.dialogScope.show(BrowserDialog(activity, "翻译").items(choices.map { it.title }) { index ->
            if (disposed || current() !== tab) return@items
            when (choices[index].id) {
                6 -> inject(tab, true)
                4 -> inject(tab, false)
                1 -> TranslationUrls.baidu(tab.url, locale())?.let(open)
                3 -> TranslationUrls.google(tab.url, locale())?.let(open)
                5 -> showText()
            }
        })
    }

    private fun inject(tab: BrowserTab, edge: Boolean) {
        val view = tab.liveView ?: return
        PageTranslationBridge.inject(view, { token ->
            if (edge) PageTranslationScripts.edge(locale(), token) else PageTranslationScripts.google(locale(), token)
        }) {
            if (!disposed && current() === tab && tab.liveView === view) showProviders(false)
        }
        messages.show("稍等片刻…")
    }

    fun showText(text: String? = null) {
        if (disposed) return
        panel?.let { it.setInput(text.orEmpty(), translate = true); return }
        val created = TranslationPanel(activity) { close() }
        panel = created
        attach(created)
        if (!text.isNullOrEmpty()) created.setInput(text)
    }

    private fun attach(created: TranslationPanel) {
        val target = parent()
        val spec = ChromeLayoutPolicy.resolve(activity)
        val top = spec.topAnchored
        val shortSide = if (target.width > 0 && target.height > 0) minOf(target.width, target.height)
            else minOf(activity.resources.displayMetrics.widthPixels, activity.resources.displayMetrics.heightPixels)
        val width = if (spec.tablet && spec.mode != ChromeMode.TRADITIONAL)
            minOf(shortSide - activity.dp(96), activity.dp(384)).coerceAtLeast(1) else shortSide
        overlay = ContentOverlay(activity, target) {
            if (!rebinding) { created.dispose(); if (panel === created) panel = null; overlay = null }
        }.apply { show(created.view, width, top, end = true) }
    }

    fun close(): Boolean {
        panel?.hideKeyboard()
        return overlay?.close() == true
    }

    fun rebind() {
        val current = panel ?: return
        rebinding = true
        (current.view.parent as? ViewGroup)?.removeView(current.view)
        overlay?.close()
        rebinding = false
        attach(current)
        current.updateGeometry()
        messages.close()
    }

    fun consume(address: String): Boolean {
        if (!isInternal(address)) return false
        val text = runCatching { Uri.parse(address).getQueryParameter("text") }.getOrNull()
        showText(text)
        return true
    }

    private fun network(url: String): Boolean {
        if (AddressResolver.isWebUrl(url)) return true
        messages.show("仅支持 \"http\" 或 \"https\" 开头的网址")
        return false
    }

    fun dispose() { disposed = true; close(); messages.close() }
    private fun china() = preferences.showRegionalProviders
    private fun locale() = preferences.deviceLocaleTag

    companion object {
        private const val PREFIX = "v://translator/translate?text="
        fun isInternal(url: String) = url.startsWith(PREFIX)
    }
}
