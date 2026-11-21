package moe.pure.browser.feature.gestures

/** Snapshot of the browser owners' guards. No Activity, View or page object is retained here. */
data class BrowserInputState(
    val browserVisible: Boolean = true,
    /** Native parent fragment back stack (URL editor and other full page overlays). */
    val contentOverlayOpen: Boolean = false,
    /** Native child fragment back stack (find/menu/library panels). */
    val panelOpen: Boolean = false,
    val inputEditing: Boolean = false,
    val keyboardVisible: Boolean = false,
    val gameMode: Boolean = false,
    val customViewShowing: Boolean = false,
    val customTab: Boolean = false,
    val chromeVisible: Boolean = true,
    val backAvailable: Boolean = false,
    val forwardAvailable: Boolean = false,
)

/** Preview values expose the drag without making the controller depend on tab presentation. */
data class PageGesturePreview(val direction: Int, val translationX: Float, val alpha: Float)
