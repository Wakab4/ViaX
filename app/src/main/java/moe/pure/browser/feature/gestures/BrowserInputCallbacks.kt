package moe.pure.browser.feature.gestures

import moe.pure.browser.feature.settings.data.NativeAction

interface BrowserInputCallbacks {
    fun state(): BrowserInputState
    fun execute(action: NativeAction)
    fun dismissOverlay(): Boolean
    fun showMenu()
    fun goHome()
    /** Zero based; -1 selects the last tab, matching Ctrl+9. */
    fun selectTab(index: Int)
    fun findNext(forward: Boolean)
    fun newTabAndAddress()
    fun setChromeVisible(visible: Boolean)
    /** -1 previous tab, +1 next tab, 0 restores the regular tab count. */
    fun onToolbarPreview(direction: Int) {}
    fun onPageGesturePreview(preview: PageGesturePreview) {}
}
