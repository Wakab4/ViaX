package moe.via.browser.feature.download.panel

import android.view.View
import android.view.ViewGroup
import moe.via.browser.browser.ui.chrome.BrowserChrome

/** Owns only the native popup's page focus/blur policy, restoring the same views on release. */
internal class DownloadPanelEffects(private val chrome: BrowserChrome) : AutoCloseable {
    private val descendants = chrome.pageFocusHost.descendantFocusability
    private val accessibility = chrome.pageFocusHost.importantForAccessibility
    init {
        chrome.pageFocusHost.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        chrome.pageFocusHost.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        chrome.pageFocusHost.clearFocus()
    }
    fun update(geometry: DownloadPanelGeometry, blur: Boolean, anchored: Boolean = true) {
        chrome.externalPanel(if (!anchored) null else if (geometry.top) android.view.Gravity.TOP else android.view.Gravity.BOTTOM, true)
        chrome.panelBlur(blur)
    }
    override fun close() {
        chrome.pageFocusHost.descendantFocusability = descendants
        chrome.pageFocusHost.importantForAccessibility = accessibility
        chrome.panelBlur(false); chrome.externalPanel(null)
    }
}
