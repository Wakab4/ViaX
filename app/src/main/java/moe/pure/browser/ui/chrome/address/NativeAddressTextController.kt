package moe.pure.browser.ui.chrome.address

import android.content.Context
import moe.pure.browser.R
import moe.pure.browser.core.BrowserInternalPages
import moe.pure.browser.core.NativePageTitles

/** s6.F/t retain the previous text when a callback does not apply to the active display mode. */
class NativeAddressTextController(context: Context, private val apply: (String) -> Unit) {
    private val app = context.applicationContext
    private val untitled = context.getString(R.string.ref_tab_switcher_untitled)
    private var text = context.getString(R.string.ref_home_page_title)

    init { apply(text) }

    fun urlChanged(rawUrl: String?, mode: Int, effectiveTabBar: Boolean) {
        val url = NativeAddressDisplayUrl.unwrap(rawUrl)
        val displayMode = if (mode == 0 && effectiveTabBar) 2 else mode
        if (displayMode == 0 || url.isNullOrEmpty() || internal(url)) return
        val display = NativeAddressDisplayUrl.display(url)
        update(if (displayMode == 2) NativeAddressDisplayUrl.domain(display) else NativeAddressDisplayUrl.url(display))
    }

    fun titleChanged(title: String?, rawUrl: String?, mode: Int, effectiveTabBar: Boolean) {
        val url = NativeAddressDisplayUrl.unwrap(rawUrl)
        val pageTitle = if (rawUrl != null && url != null && rawUrl.length != url.length)
            NativeAddressDisplayUrl.display(url) else title
        if (internal(url)) update(pageTitle)
        else if (mode == 0 && !effectiveTabBar)
            update(NativePageTitles.decorate(pageTitle?.takeIf(String::isNotEmpty) ?: untitled, url))
    }

    /** e8.n0.D emits F before t, and leaves an untitled internal page's old text untouched. */
    fun selected(title: String?, rawUrl: String?, mode: Int, effectiveTabBar: Boolean) {
        urlChanged(rawUrl, mode, effectiveTabBar)
        if (!title.isNullOrEmpty()) titleChanged(title, rawUrl, mode, effectiveTabBar)
        else if (!rawUrl.isNullOrEmpty() && !internal(rawUrl)) titleChanged(rawUrl, rawUrl, mode, effectiveTabBar)
    }

    fun capture(): String = text
    fun restore(value: String) = update(value)

    private fun internal(url: String?) = url != null && BrowserInternalPages.isInternal(app, url)
    private fun update(value: String?) {
        val next = value.orEmpty()
        if (next != text) { text = next; apply(next) }
    }
}
