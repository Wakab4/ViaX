package moe.via.browser.browser.ui.tabs

import android.content.Context
import android.graphics.Bitmap
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.feature.internalpages.BrowserInternalPages

/** Capture label and navigation data without waking a dormant tab. */
internal data class TabStripPresentation(
    val id: String,
    val title: String,
    val url: String,
    val home: Boolean,
    val icon: Bitmap?
) {
    companion object {
        fun capture(context: Context, tab: BrowserTab): TabStripPresentation {
            val resident = tab.liveView
            val url = if (resident == null) tab.url else resident.url.orEmpty()
            val title = if (resident == null) tab.title else resident.title.orEmpty()
            val internalTitle = BrowserInternalPages.tabTitle(context, url.ifEmpty { tab.url }, title)
                ?: title.takeIf { it.startsWith("file:", ignoreCase = true) }?.let {
                    BrowserInternalPages.tabTitle(context, it)
                }
            return TabStripPresentation(tab.id, internalTitle ?: title.ifEmpty { url }, url, tab.home, resident?.favicon)
        }
    }
}
