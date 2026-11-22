package moe.via.browser.feature.ai

import android.content.Context
import moe.via.browser.browser.navigation.AddressResolver
import moe.via.browser.feature.ai.model.AiPageAttachment
import moe.via.browser.browser.session.BrowserTab
import org.json.JSONTokener

/** The fixed APK extractor is shared conceptually with Reader, but never enters reading mode. */
class AiPageExtractor(context: Context) {
    private val assets = context.applicationContext.assets
    private val code by lazy {
        assets.open("reader/readability.js").bufferedReader().use { it.readText() } + "\n" +
            assets.open("ai/extract-page.js").bufferedReader().use { it.readText() }
    }

    fun extract(tab: BrowserTab, completed: (AiPageAttachment?) -> Unit) {
        val view = tab.liveView
        if (view == null || !view.settings.javaScriptEnabled || !AddressResolver.isWebUrl(tab.url)) { completed(null); return }
        val url = tab.url
        val title = tab.title
        view.evaluateJavascript(code) { raw ->
            val text = runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull().orEmpty()
            completed(AiPageAttachment(url, title, text))
        }
    }
}
