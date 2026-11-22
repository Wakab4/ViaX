package moe.via.browser.feature.about.document

import android.content.Context
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState
import moe.via.browser.feature.internalpages.GeneratedPageFiles
import moe.via.browser.feature.internalpages.render.GeneratedPageMarkup

/** u9.c.b / u9.d.a: the generated leaf is separate from the native settings About Activity. */
class AboutDocument(context: Context) {
    private val app = context.applicationContext

    fun generate(options: AboutDocumentOptions): String {
        if (GeneratedPageDirtyState.isDirty(GeneratedPageDirtyState.ABOUT) || !GeneratedPageFiles.exists(app, NAME)) {
            val html = GeneratedPageMarkup.head(options.title) + "<style>" + AboutDocumentCss.render(options.style) +
                "</style><body><div class='frosted-glass' id='gesture-indicator'></div><div id=\"content\">" +
                body(options) + "</div></body></html>"
            GeneratedPageFiles.writeUtf8(GeneratedPageFiles.file(app, NAME), html)
            // c1.Q's false result does not prevent Native from clearing the About bit.
            GeneratedPageDirtyState.clear(GeneratedPageDirtyState.ABOUT)
        }
        return url(app)
    }

    private fun body(options: AboutDocumentOptions): String = buildString {
        append("<h1>").append(options.gleanTitle).append("</h1><p>")
        append(options.quote.replace("\n", "</p><p>"))
        append("</p><h1>").append(options.contactTitle).append("</h1>")
        if (options.showWebsite) append("<p><a href=\"http://viayoo.com/\">").append(options.websiteLabel).append("</a></p>")
        if (options.showQqGroup) append("<p><a href=\"http://viayoo.com/contact/qqgroup/\">官方鹅群</a></p>")
        append("<p><a href=\"https://t.me/viatg\">Telegram</a></p><p><a href=\"https://github.com/tuyafeng/Via\">")
        append(options.translateLabel).append("</a></p><p><a href=\"").append(options.termsUrl).append("\">")
        append(options.termsLabel).append("</a></p><p><a href=\"").append(options.privacyUrl).append("\">")
        append(options.privacyLabel).append("</a></p><br><br><br>")
        // GP c0.h is false; the CN-only donation section is not part of this reference build.
    }

    companion object {
        private const val NAME = "about.html"
        fun url(context: Context): String = GeneratedPageFiles.url(context, NAME)
        fun isPage(context: Context, url: String?): Boolean = GeneratedPageFiles.isPage(context, url, NAME)
    }
}
