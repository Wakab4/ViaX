package moe.pure.browser.feature.history.document

import android.content.Context
import moe.pure.browser.core.logging.NativeLog as Log
import moe.pure.browser.data.BrowserStore
import moe.pure.browser.feature.history.HistoryDirtyState
import moe.pure.browser.feature.history.data.HistoryRecord
import moe.pure.browser.feature.history.data.HistoryRows
import moe.pure.browser.feature.internalpages.GeneratedPageFiles
import moe.pure.browser.feature.internalpages.render.GeneratedListCss
import moe.pure.browser.feature.internalpages.render.GeneratedPageMarkup
import moe.pure.browser.feature.internalpages.render.GeneratedPageStyle
import java.io.BufferedWriter
import java.io.FileWriter

/** u9.c.g: synchronous worker generation; the WebView receives the actual owned file URL. */
class HistoryDocument(context: Context) {
    private val app = context.applicationContext

    fun generate(options: HistoryDocumentOptions): String {
        val file = GeneratedPageFiles.file(app, NAME)
        if (HistoryDirtyState.isDirty() || !file.exists()) {
            try {
                FileWriter(file).use { output ->
                    BufferedWriter(output).use { writer ->
                        writer.write(GeneratedPageMarkup.head(options.title))
                        writer.write("<style>${GeneratedListCss.render(GeneratedPageStyle(options.isRtl, options.isNight, options.isLight))}</style>")
                        writer.write("<body><div class='frosted-glass' id='gesture-indicator'></div><div id=\"content\">")
                        val records = recent()
                        if (records.isEmpty()) writer.write(GeneratedPageMarkup.hint(options.empty))
                        else if (options.globalIncognito) writer.write(GeneratedPageMarkup.hint(options.incognitoHint))
                        for (record in records) writer.write(GeneratedPageMarkup.row(record.url.orEmpty(), record.title.orEmpty(), "clock"))
                        writer.write("</div></body></html>")
                        // Native clears before BufferedWriter.close/flush, including that failure boundary.
                        HistoryDirtyState.clear()
                    }
                }
            } catch (_: Exception) { }
        }
        return "file://${file.path}"
    }

    private fun recent(): List<HistoryRecord> = try {
        BrowserStore(app).use { HistoryRows(it.readableDatabase).read(limit = "256").rows }
    } catch (error: Exception) {
        Log.warn("History", "History document query", error)
        emptyList()
    }

    companion object {
        private const val NAME = "history.html"
        fun url(context: Context): String = GeneratedPageFiles.url(context, NAME)

        /** Match the one raw owned path, with only the Native query/fragment suffix allowance. */
        fun isPage(context: Context, url: String?): Boolean = GeneratedPageFiles.isPage(context, url, NAME)
    }
}
