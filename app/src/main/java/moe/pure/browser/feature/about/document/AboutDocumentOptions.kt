package moe.pure.browser.feature.about.document

import moe.pure.browser.feature.internalpages.render.GeneratedPageStyle

/** The window resolves locale, channel and public document URLs before handing plain values to IO. */
data class AboutDocumentOptions(
    val title: String,
    val gleanTitle: String,
    val quote: String,
    val contactTitle: String,
    val websiteLabel: String,
    val translateLabel: String,
    val termsLabel: String,
    val privacyLabel: String,
    val termsUrl: String,
    val privacyUrl: String,
    val showWebsite: Boolean,
    val showQqGroup: Boolean,
    val style: GeneratedPageStyle
)
