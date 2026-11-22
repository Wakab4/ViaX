package moe.via.browser.browser.ui.gestures

import android.view.KeyEvent
import moe.via.browser.storage.preferences.BrowserPreferences
import moe.via.browser.storage.preferences.NativeAction

/** Fixed GP Shell.dispatchKeyEvent -> s6.d8/z9/A9 map, including its key-up consumption. */
internal class BrowserHardwareKeys(
    private val preferences: BrowserPreferences,
    private val callbacks: BrowserInputCallbacks,
) {
    fun onKey(event: KeyEvent): Boolean {
        val state = callbacks.state()
        if (!state.browserVisible) return false
        val key = event.keyCode
        if (state.contentOverlayOpen || state.inputEditing) {
            if (event.action == KeyEvent.ACTION_DOWN && key == KeyEvent.KEYCODE_ESCAPE) {
                callbacks.dismissOverlay(); return true
            }
            return false
        }
        if (event.action == KeyEvent.ACTION_UP) {
            return (key == KeyEvent.KEYCODE_VOLUME_UP || key == KeyEvent.KEYCODE_VOLUME_DOWN) && !state.customViewShowing
        }
        if (event.action != KeyEvent.ACTION_DOWN) return false
        val ctrl = event.isCtrlPressed
        val alt = event.isAltPressed
        val shift = event.isShiftPressed
        when (key) {
            KeyEvent.KEYCODE_HOME -> if (alt) { callbacks.goHome(); return true }
            KeyEvent.KEYCODE_B -> if (ctrl && shift) return action(NativeAction.BOOKMARKS)
            KeyEvent.KEYCODE_L -> if (ctrl) return action(NativeAction.ADDRESS)
            KeyEvent.KEYCODE_R -> if (ctrl) return action(NativeAction.RELOAD)
            KeyEvent.KEYCODE_W -> if (ctrl) return action(NativeAction.CLOSE_TAB)
            KeyEvent.KEYCODE_TAB -> if (ctrl) return action(if (shift) NativeAction.PREVIOUS_TAB else NativeAction.NEXT_TAB)
            KeyEvent.KEYCODE_MENU -> { callbacks.showMenu(); return true }
            KeyEvent.KEYCODE_SEARCH -> return action(NativeAction.ADDRESS)
            KeyEvent.KEYCODE_ESCAPE -> { callbacks.dismissOverlay(); return true }
            KeyEvent.KEYCODE_F5 -> return action(NativeAction.RELOAD)
            KeyEvent.KEYCODE_DPAD_LEFT -> if (alt) return action(NativeAction.BACK)
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (alt) return action(NativeAction.FORWARD)
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (state.customViewShowing || state.gameMode || state.panelOpen || !preferences.volumePaging) return false
                return action(if (key == KeyEvent.KEYCODE_VOLUME_UP) NativeAction.PAGE_UP else NativeAction.PAGE_DOWN)
            }
            KeyEvent.KEYCODE_T -> if (ctrl) { callbacks.newTabAndAddress(); return true }
            KeyEvent.KEYCODE_U -> if (ctrl) return action(NativeAction.SOURCE)
            KeyEvent.KEYCODE_PAGE_UP -> return action(NativeAction.PAGE_UP)
            KeyEvent.KEYCODE_PAGE_DOWN -> return action(NativeAction.PAGE_DOWN)
            in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_9 -> if (ctrl) {
                callbacks.selectTab(if (key == KeyEvent.KEYCODE_9) -1 else key - KeyEvent.KEYCODE_1)
                return true
            }
            KeyEvent.KEYCODE_D -> if (ctrl) return action(NativeAction.ADD_BOOKMARK)
            KeyEvent.KEYCODE_E -> if (alt) { callbacks.showMenu(); return true }
            KeyEvent.KEYCODE_F -> {
                if (ctrl) return action(NativeAction.FIND)
                if (alt) { callbacks.showMenu(); return true }
            }
            KeyEvent.KEYCODE_G -> if (ctrl) { callbacks.findNext(!shift); return true }
            KeyEvent.KEYCODE_H -> if (ctrl) return action(NativeAction.HISTORY)
        }
        return false
    }

    private fun action(action: NativeAction): Boolean { callbacks.execute(action); return true }
}
