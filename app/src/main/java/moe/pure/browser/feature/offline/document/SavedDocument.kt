package moe.pure.browser.feature.offline.document

import android.content.Context
import moe.pure.browser.feature.internalpages.GeneratedPageFiles
import moe.pure.browser.feature.internalpages.render.GeneratedListCss
import moe.pure.browser.feature.internalpages.render.GeneratedPageMarkup
import moe.pure.browser.feature.internalpages.render.GeneratedPageStyle
import java.io.BufferedWriter
import java.io.FileWriter

data class SavedDocumentOptions(val title: String, val empty: String, val hint: String, val style: GeneratedPageStyle)

/** u9.c.i always rebuilds save.html; this page has no dirty-bit or metadata cache gate. */
class SavedDocument(context: Context) {
    private val app = context.applicationContext

    fun generate(options: SavedDocumentOptions): String {
        try {
            FileWriter(GeneratedPageFiles.file(app, NAME)).use { output ->
                BufferedWriter(output).use { writer ->
                    writer.write(GeneratedPageMarkup.head(options.title))
                    writer.write("<style>${GeneratedListCss.render(options.style)}</style>")
                    writer.write("<body><div class='frosted-glass' id='gesture-indicator'></div><div id=\"content\">")
                    val entries = SavedArchiveFiles.read(app)
                    writer.write(GeneratedPageMarkup.hint(if (entries.isEmpty()) options.empty else options.hint))
                    for (entry in entries) writer.write(GeneratedPageMarkup.row(entry.url, entry.title, "clock"))
                    writer.write("</div></body></html>")
                }
            }
        } catch (_: Exception) { }
        return url(app)
    }

    companion object {
        private const val NAME = "save.html"
        fun url(context: Context): String = GeneratedPageFiles.url(context, NAME)
        fun isPage(context: Context, url: String?): Boolean = GeneratedPageFiles.isPage(context, url, NAME)
    }
}
