package moe.pure.browser.feature.reader

import android.content.Context

/** i6.c0.g/i/j: entry separates style/content; exit reports immediately after dispatch. */
internal class ReaderPageProtocol(context: Context) {
    private val scripts = ReaderScripts(context)
    fun probe(page: ReaderPage, completed: (Int) -> Unit) {
        if (!page.javascriptEnabled || !ReaderProbeRules.network(page.url)) { completed(0); return }
        page.evaluate(scripts.probeState(page.url.orEmpty())) { value, _ -> completed(value?.toIntOrNull() ?: 0) }
    }
    fun enter(page: ReaderPage, completed: (Int) -> Unit) {
        if (!page.javascriptEnabled) { completed(1); return }
        if (!ReaderProbeRules.network(page.url)) { completed(4); return }
        page.evaluate(scripts.baseStyle())
        page.evaluate(scripts.enter()) { _, error -> completed(if (error.isNullOrEmpty()) 0 else 2) }
    }
    fun exit(page: ReaderPage, completed: (Int) -> Unit) {
        if (!page.javascriptEnabled) { completed(1); return }
        page.evaluate(scripts.exit())
        completed(0)
    }
}
