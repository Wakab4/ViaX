package moe.pure.browser.feature.about.document

import android.content.Context
import moe.pure.browser.R
import moe.pure.browser.feature.internalpages.render.GeneratedPageStyle
import java.util.Locale

object AboutDocumentTexts {
    fun options(context: Context, style: GeneratedPageStyle, termsUrl: String, privacyUrl: String,
        channel: String = "viayoo"): AboutDocumentOptions = AboutDocumentOptions(
        context.getString(R.string.ref_about_document_title),
        context.getString(R.string.ref_about_document_glean),
        if (Locale.getDefault().country.equals("CN", ignoreCase = true)) "莫忧世事兼身事，须著人间比梦间。" else "Less is more.",
        context.getString(R.string.ref_about_document_contact),
        context.getString(R.string.ref_about_document_website),
        context.getString(R.string.ref_about_document_translate),
        context.getString(R.string.ref_about_document_terms),
        context.getString(R.string.ref_about_document_privacy),
        termsUrl, privacyUrl,
        channel != "huawei" && channel != "vivo",
        context.resources.configuration.locales[0].country == "CN",
        style
    )
}
