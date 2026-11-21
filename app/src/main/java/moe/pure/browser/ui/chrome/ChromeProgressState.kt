package moe.pure.browser.ui.chrome

import moe.pure.browser.ui.chrome.progress.NativeProgressState

/** Chrome restores its action icon separately from the shared bar's animation owner. */
data class ChromeProgressState(val animation: NativeProgressState, val loading: Boolean)
