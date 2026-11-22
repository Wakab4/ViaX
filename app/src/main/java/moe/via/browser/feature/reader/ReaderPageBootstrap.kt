package moe.via.browser.feature.reader

import android.content.Context
import android.webkit.WebView

/** i6.c0.q prepares Readability then readerability at the DOM phase before probing current. */
internal class ReaderPageBootstrap(context: Context) {
    private val assets = context.applicationContext.assets
    private val readability by lazy { read("readability.js") }
    private val readerable by lazy { read("readerable.js") }

    fun prepare(view: WebView, prepared: () -> Unit) {
        val page = ReaderPage(view)
        if (!ReaderProbeRules.network(page.url)) return
        page.evaluate(readability)
        page.evaluate(readerable) { _, error -> if (error.isNullOrEmpty()) prepared() }
    }
    private fun read(name: String) = assets.open("reader/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }
}
