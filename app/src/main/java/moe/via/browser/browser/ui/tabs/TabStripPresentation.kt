package moe.via.browser.browser.ui.tabs

import android.graphics.Bitmap
import moe.via.browser.browser.session.BrowserTab

/** t.c binds raw titles and URLs; its empty-title rule differs from the popup tab selector. */
internal data class TabStripPresentation(
    val id: String,
    val title: String,
    val url: String,
    val home: Boolean,
    val icon: Bitmap?
) {
    companion object {
        fun capture(tab: BrowserTab): TabStripPresentation {
            val resident = tab.liveView
            val url = if (resident == null) tab.url else resident.url.orEmpty()
            val title = if (resident == null) tab.title else resident.title.orEmpty()
            return TabStripPresentation(tab.id, title.ifEmpty { url }, url, tab.home, resident?.favicon)
        }
    }
}
