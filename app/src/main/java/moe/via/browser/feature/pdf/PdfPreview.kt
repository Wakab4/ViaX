package moe.via.browser.feature.pdf

import android.net.Uri
import android.os.Bundle
import moe.via.browser.feature.pdf.ui.PdfViewerFragment
import moe.via.browser.ui.theme.ThemedActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import moe.via.browser.ui.pages.RecordPageScope

/** za.g's URI and display name contract; the preview is always an internal application page. */
internal object PdfPreview {
    fun open(activity: FragmentActivity, uri: Uri, name: String?, source: Fragment? = null): Boolean {
        val arguments = Bundle().apply { putString("pdfPath", uri.toString()); putString("pdfName", name) }
        val host = activity as ThemedActivity
        val stack = if (source != null) RecordPageScope.of(source) else host.currentRecordStack
        return stack?.push(PdfViewerFragment::class.java, arguments) == true
    }
}
