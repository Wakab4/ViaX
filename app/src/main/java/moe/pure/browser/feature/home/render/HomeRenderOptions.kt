package moe.pure.browser.feature.home.render

import java.io.File

/** The real home and the touch-blocked native preview use the same original document. */
data class HomeRenderOptions(
    val pageTitle: String,
    val bookmarkTitle: String,
    val searchTitle: String
)

/** htmlUrl is the source file URL; cssUrl is the URL embedded in its stylesheet link. */
data class HomeDocumentFiles(val htmlFile: File, val cssFile: File, val htmlUrl: String, val cssUrl: String)
