package moe.pure.browser.feature.network

import android.content.Context
import moe.pure.browser.R
import moe.pure.browser.feature.internalpages.render.GeneratedPageStyle

/** All window-localized values are resolved before a worker starts generating the file. */
data class NetworkLogDocumentOptions(
    val style: GeneratedPageStyle,
    val title: String,
    val resourcesHint: String,
    val logHint: String,
    val empty: String,
    val resourcesEmpty: String,
    val resourcesNote: String
)

object NetworkLogDocumentTexts {
    fun options(window: Context, resources: Boolean, style: GeneratedPageStyle, rulesPath: String) =
        NetworkLogDocumentOptions(
            style = style,
            title = window.getString(if (resources) R.string.ref_network_page_resource_title else R.string.ref_network_page_log_title),
            resourcesHint = window.getString(R.string.ref_network_page_resources_hint),
            logHint = window.getString(R.string.ref_network_page_log_hint, rulesPath),
            empty = window.getString(R.string.ref_network_page_empty),
            resourcesEmpty = window.getString(R.string.ref_network_page_resources_empty),
            resourcesNote = window.getString(R.string.ref_network_page_resources_note)
        )
}
