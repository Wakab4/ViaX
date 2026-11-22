package moe.via.browser.feature.startpage.document

import android.content.Context
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState
import moe.via.browser.feature.internalpages.GeneratedPageFiles
import moe.via.browser.feature.internalpages.render.GeneratedPageMarkup

/** u9.c.c: blank generation consumes the same bit 256 also used by the reader resume effect. */
class BlankDocument(context: Context) {
    private val app = context.applicationContext

    fun generate(title: String): String {
        if (GeneratedPageDirtyState.isDirty(GeneratedPageDirtyState.PAGE_EFFECTS) ||
            !GeneratedPageFiles.exists(app, NAME)) {
            val html = GeneratedPageMarkup.head(title) +
                "<style>body{background:transparent;}</style><body></body></html>"
            GeneratedPageFiles.writeUtf8(GeneratedPageFiles.file(app, NAME), html)
            // Native ignores Q's result and clears this shared bit even after a caught write failure.
            GeneratedPageDirtyState.clear(GeneratedPageDirtyState.PAGE_EFFECTS)
        }
        return url(app)
    }

    companion object {
        private const val NAME = "blank.html"
        fun url(context: Context): String = GeneratedPageFiles.url(context, NAME)
        fun isPage(context: Context, url: String?): Boolean = GeneratedPageFiles.isPage(context, url, NAME)
    }
}
