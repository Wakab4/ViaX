package moe.pure.browser.feature.tools

import android.app.Activity
import android.webkit.WebView
import moe.pure.browser.R
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.ui.components.BrowserMessage
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.theme.ThemedActivity
import moe.pure.browser.feature.tools.printing.BrowserPrintAction

class PageToolDialogs(private val activity: Activity, private val settings: BrowserSettings) {
    private val printingDelegate = lazy { BrowserPrintAction(activity) }
    private val printing by printingDelegate
    private val dialogs = (activity as? ThemedActivity)?.dialogScope ?: DialogScope()
    private val messagesDelegate = lazy { BrowserMessage(activity) }
    private val messages by messagesDelegate
    fun orientation(onChanged: () -> Unit = {}) {
        val labels = activity.resources.getStringArray(R.array.ref_page_orientation_options).toList()
        val original = settings.orientation
        dialogs.show(BrowserDialog(activity, activity.getString(R.string.ref_page_orientation_title))
            .singleChoice(labels, original) { index ->
                if (index == original) return@singleChoice
                settings.orientation = index
                settings.applyOrientation(activity)
                messages.show(activity.getString(R.string.ref_page_tool_changed,
                    activity.getString(R.string.ref_page_orientation_title), labels[index]))
                onChanged()
            })
    }

    fun images(onChanged: () -> Unit) {
        val labels = listOf(activity.getString(R.string.ref_image_page_mode_show), activity.getString(R.string.ref_image_page_mode_block),
            activity.getString(R.string.ref_image_page_mode_wifi))
        val preferences = settings.preferences
        val original = if (preferences.wifiImages) 2 else if (preferences.imagesEnabled) 0 else 1
        dialogs.show(BrowserDialog(activity, activity.getString(R.string.ref_image_page_title))
            .singleChoice(labels, original) { index ->
                if (index == original) return@singleChoice
                settings.imageMode = index
                onChanged()
                messages.show(labels[index])
            })
    }

    fun print(view: WebView) {
        printing.print(view)
    }

    fun dispose() {
        if (printingDelegate.isInitialized()) printing.close()
        if (messagesDelegate.isInitialized()) messages.close()
    }

}
