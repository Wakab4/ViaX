package moe.pure.browser.feature.catalog.document

import android.content.Context
import moe.pure.browser.feature.catalog.CatalogDirtyState
import moe.pure.browser.feature.internalpages.GeneratedPageFiles
import moe.pure.browser.feature.internalpages.render.GeneratedListCss
import moe.pure.browser.feature.internalpages.render.GeneratedPageMarkup

/** u9.c.f/e generates six static links; it never reads bookmarks, history or another user table. */
class CatalogDocument(context: Context) {
    private val app = context.applicationContext

    fun generate(options: CatalogDocumentOptions): String {
        if (CatalogDirtyState.isDirty() || !GeneratedPageFiles.exists(app, NAME)) {
            val html = buildString {
                append(GeneratedPageMarkup.head(options.title))
                append("<style>").append(GeneratedListCss.render(options.style)).append("</style>")
                append("<body><div class='frosted-glass' id='gesture-indicator'></div><div id=\"content\">")
                LINKS.forEachIndexed { index, url -> append(GeneratedPageMarkup.row(url, options.labels[index], "tag")) }
                append("</div></body></html>")
            }
            // Native ignores Q's Boolean: both a successful write and a caught IO failure clear bit 64.
            GeneratedPageFiles.writeUtf8(GeneratedPageFiles.file(app, NAME), html)
            CatalogDirtyState.clear()
        }
        return url(app)
    }

    companion object {
        private const val NAME = "catalog.html"
        private val LINKS = listOf("v://bookmarks", "v://history", "v://log", "v://offline", "v://home", "v://about")
        fun url(context: Context): String = GeneratedPageFiles.url(context, NAME)
        fun isPage(context: Context, url: String?): Boolean = GeneratedPageFiles.isPage(context, url, NAME)
    }
}
