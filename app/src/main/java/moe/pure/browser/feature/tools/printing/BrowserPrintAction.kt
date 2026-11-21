package moe.pure.browser.feature.tools.printing

import android.app.Activity
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import moe.pure.browser.R
import moe.pure.browser.ui.components.BrowserMessage

/** The menu and page command share X7's platform action and short failure message. */
class BrowserPrintAction(private val activity: Activity) : AutoCloseable {
    private val messages = BrowserMessage(activity)
    private var closed = false

    fun print(view: WebView?) {
        if (closed || view == null) return
        val manager = activity.getSystemService(PrintManager::class.java)
        if (manager == null) { failed(); return }
        val title = PrintJobName.from(activity, view.title, view.url)
        try {
            manager.print(title, view.createPrintDocumentAdapter(title), PrintAttributes.Builder().build())
        } catch (_: Exception) { failed() }
    }

    private fun failed() { messages.show(activity.getString(R.string.ref_print_failure)) }

    override fun close() {
        if (closed) return
        closed = true; messages.close()
    }
}
