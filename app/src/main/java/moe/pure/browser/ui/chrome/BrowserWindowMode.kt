package moe.pure.browser.ui.chrome

/** Window-local presentation, independent of the user's saved main-browser toolbar preference. */
interface BrowserWindowMode {
    val customTabMode: Boolean
}
