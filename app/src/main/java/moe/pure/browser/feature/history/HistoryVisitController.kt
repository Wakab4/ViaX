package moe.pure.browser.feature.history

import android.content.Context
import moe.pure.browser.core.NativePageTitles

/** e8.n0.w → s6.g0 → ua.W1/t9.e.d, independent of whether the source is visible. */
class HistoryVisitController(context: Context, private val isPrivate: (String) -> Boolean,
    private val onNonHomeTitle: () -> Unit) {
    private val app = context.applicationContext
    private var disposed = false

    fun titleChanged(title: String?, rawUrl: String?) {
        if (disposed) return
        if (!generatedHome(rawUrl)) onNonHomeTitle()
        if (isPrivate(rawUrl.orEmpty())) return
        val decorated = NativePageTitles.decorate(title, rawUrl)
        if (rawUrl.isNullOrEmpty() || decorated.isNullOrEmpty() || rawUrl.startsWith("file://") ||
            rawUrl.startsWith("view-source:") || rawUrl.startsWith("about:")) return
        HistoryVisitWriter.submit(app, rawUrl, decorated)
        HistoryDirtyState.markDirty()
    }

    fun dispose() { disposed = true }

    /** u9.d.k/d uses the raw private-file prefix, rather than URI canonicalization or start-page choice. */
    private fun generatedHome(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val prefix = "file://${app.filesDir.path}/"
        if (!url.startsWith(prefix)) return false
        var name = url.substring(prefix.length)
        val query = name.indexOf('?')
        if (query > 0) name = name.substring(0, query)
        val fragment = name.indexOf('#')
        if (fragment > 0) name = name.substring(0, fragment)
        return name == "homepage.html" || name == "homepage2.html"
    }
}
