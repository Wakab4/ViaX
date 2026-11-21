package moe.pure.browser.feature.catalog.document

import moe.pure.browser.feature.internalpages.render.GeneratedPageStyle

/** Native fixed order: bookmarks, history, network log, saved pages, homepage, about. */
data class CatalogDocumentOptions(val title: String, val labels: List<String>, val style: GeneratedPageStyle) {
    init { require(labels.size == 6) { "The catalog has six fixed labels" } }
}
