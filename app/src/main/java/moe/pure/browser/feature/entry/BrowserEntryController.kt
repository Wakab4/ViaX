package moe.pure.browser.feature.entry

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.search.data.SearchEngineRepository
import moe.pure.browser.feature.search.resolve.SearchUrlResolver
import java.io.InputStream
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/** External Intent parsing and worker-owned content document preparation. */
class BrowserEntryController(context: Context) {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val importer = EntryDocumentImporter(app)
    private val jobs = mutableSetOf<Preparation>()
    private var disposed = false

    /** No document stream is opened here. Existing shared-text/query/process-text entry stays in SearchController. */
    fun parse(intent: Intent?): BrowserEntryRequest? = runCatching {
        if (disposed || intent == null) return null
        val action = intent.action
        val internal = internalActionUrl(action)
        if (internal == null && usesSearch(intent)) return null
        val raw = internal ?: if (intent.hasExtra("OPEN")) intent.getStringExtra("OPEN")
            else if (intent.hasExtra(Intent.EXTRA_PROCESS_TEXT) && translationAction(action)) {
                "v://translator/translate?text=" + URLEncoder.encode(intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty(), "UTF-8").replace("+", "%20")
            } else intent.dataString
        if (raw.isNullOrEmpty()) return null
        val uri = Uri.parse(raw)
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        // The APK declares inline:, but has no inline HTML loader; its URLUtil gate rejects it.
        if (scheme == "inline" || scheme == "data") return null
        val mime = intent.type?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
        BrowserEntryRequest(raw, mime, action, raw.startsWith("javascript:", true))
    }.getOrNull()

    /** Each request is independent. cancelPending/dispose close streams and suppress callbacks. */
    fun prepare(request: BrowserEntryRequest, completed: (Result<BrowserEntry>) -> Unit): BrowserEntryPreparation {
        val job = Preparation(request, completed)
        if (disposed) { job.cancel(); return job }
        jobs.add(job)
        BrowserExecutors.network.execute { job.run() }
        return job
    }

    fun cancelPending() { jobs.toList().forEach { it.cancel() }; jobs.clear() }
    fun dispose() { disposed = true; cancelPending() }

    private fun resolve(request: BrowserEntryRequest, cancelled: () -> Boolean, source: (InputStream?) -> Unit): BrowserEntry {
        val uri = Uri.parse(request.url)
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme == "file" || scheme == "content") return importer.prepare(request, cancelled, source)
        if (scheme in setOf("v", "via", "folder", "history")) return BrowserEntry(request.url, request.mimeType, request.url, routeOnly = true)
        if (scheme == "http" || scheme == "https") return BrowserEntry(SearchUrlResolver.normalize(request.url), request.mimeType,
            request.url, reuseExisting = true)
        if (scheme == "about" || scheme == "ftp" || request.url.startsWith("view-source:", true)) {
            return BrowserEntry(request.url, request.mimeType, request.url)
        }
        val template = SearchEngineRepository(app).use { it.selected().mobileTemplate }
        val resolved = SearchUrlResolver.resolve(request.url, template, request.forceSearch) ?: error("无效的打开地址")
        return BrowserEntry(resolved.url, request.mimeType, request.url,
            routeOnly = !resolved.isSearch && !SearchUrlResolver.isHttp(resolved.url), reuseExisting = SearchUrlResolver.isHttp(resolved.url))
    }

    private inner class Preparation(private val request: BrowserEntryRequest, private val completed: (Result<BrowserEntry>) -> Unit) : BrowserEntryPreparation {
        private val cancelled = AtomicBoolean()
        @Volatile private var input: InputStream? = null
        fun run() {
            val result = runCatching { check(!cancelled.get()); resolve(request, cancelled::get) { stream ->
                input = stream
                if (cancelled.get()) runCatching { stream?.close() }
            } }
            main.post {
                jobs.remove(this)
                if (!disposed && !cancelled.get()) completed(result)
            }
        }
        override fun cancel() { cancelled.set(true); runCatching { input?.close() }; input = null }
    }

    companion object {
        /** Shared with Root's search split so OPEN beats query, but SEND text and Native actions win. */
        fun usesSearch(intent: Intent): Boolean = runCatching {
            when {
                internalActionUrl(intent.action) != null -> false
                intent.action.equals(Intent.ACTION_SEND, true) && !intent.getCharSequenceExtra(Intent.EXTRA_TEXT).isNullOrEmpty() -> true
                intent.hasExtra("OPEN") -> false
                intent.hasExtra("query") -> true
                else -> intent.hasExtra(Intent.EXTRA_PROCESS_TEXT) && !translationAction(intent.action)
            }
        }.getOrDefault(false)

        fun accepts(intent: Intent): Boolean = runCatching {
            !usesSearch(intent) && (internalActionUrl(intent.action) != null || intent.hasExtra("OPEN") ||
                intent.hasExtra(Intent.EXTRA_PROCESS_TEXT) && translationAction(intent.action) || intent.dataString != null)
        }.getOrDefault(false)

        private fun internalActionUrl(action: String?): String? {
            val suffix = listOf("mark.via.", "moe.pure.browser.").firstOrNull { action?.startsWith(it, true) == true }
                ?.let { action!!.substring(it.length).uppercase(Locale.ROOT) } ?: return null
            return when (suffix) {
                "BOOKMARK" -> "v://bookmarks"
                "HISTORY" -> "v://history"
                "SCAN" -> "v://scanner"
                "DOWNLOADER" -> "v://downloader"
                "READ_ALOUD" -> "v://readaloud"
                "SEARCH" -> "v://search"
                else -> null
            }
        }

        private fun translationAction(action: String?) = action.equals("mark.via.gp.TRANSLATE", true) ||
            action.equals("moe.pure.browser.TRANSLATE", true)

    }
}
