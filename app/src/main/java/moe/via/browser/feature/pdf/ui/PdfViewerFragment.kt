package moe.via.browser.feature.pdf.ui

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import moe.via.browser.R
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.pages.RecordPageFragment
import moe.via.browser.storage.preferences.BrowserNightMode
import moe.via.browser.ui.theme.palette

/** za.g content belongs to a view lifecycle inside the shared record-page stack. */
class PdfViewerFragment : RecordPageFragment() {
    private var scaffold: PageScaffold? = null
    private var canvas: PdfCanvasView? = null
    private var counter: PdfPageCounter? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity()
        val page = PageScaffold(activity, getString(R.string.ref_pdf_preview_title), ::closePage, applyInsets = appliesWindowInsets)
        scaffold = page
        val path = arguments?.getString("pdfPath")
        if (path.isNullOrEmpty()) return page.root
        val uri = Uri.parse(path)
        val name = arguments?.getString("pdfName") ?: if (uri.scheme == "file") uri.lastPathSegment else runCatching {
            activity.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
        }.getOrNull()
        page.titleView.text = name
        val document = PdfCanvasView(activity).also { canvas = it }
        val indicator = PdfPageCounter(activity, document).also { counter = it }
        document.pageChanged = indicator::page
        document.setOnTouchListener { _, event -> indicator.touch(event); false }
        page.showContent(FrameLayout(activity).apply {
            addView(document, FrameLayout.LayoutParams(-1, -1)); addView(indicator.view)
        })
        themeChanged(); document.open(uri)
        return page.root
    }
    internal override fun themeChanged() {
        val context = context ?: return
        scaffold?.let { page ->
            page.root.setBackgroundColor(context.palette.canvas); page.titleView.setTextColor(context.palette.text)
            (page.toolbar.getChildAt(0) as? ImageView)?.imageTintList = ColorStateList.valueOf(context.palette.icon)
        }
        counter?.refreshTheme()
        canvas?.foreground = if (BrowserNightMode.global(context)) ColorDrawable(0x80000000.toInt()) else null
    }
    override fun onDestroyView() {
        canvas?.let { it.setOnTouchListener(null); it.close() }; canvas = null
        counter?.close(); counter = null; scaffold = null
        super.onDestroyView()
    }
}
