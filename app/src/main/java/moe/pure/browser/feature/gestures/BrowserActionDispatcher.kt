package moe.pure.browser.feature.gestures

import moe.pure.browser.feature.menu.MenuAction
import moe.pure.browser.feature.settings.data.NativeAction
import moe.pure.browser.model.BrowserTab

/** Fixed input actions delegate to existing feature owners instead of duplicating their data or UI logic. */
class BrowserActionDispatcher(private val host: Host) {
    interface Host {
        fun current(): BrowserTab?
        fun tabCount(): Int
        fun selectedIndex(): Int
        fun select(index: Int)
        fun newTab()
        fun close(index: Int)
        fun closeAll()
        fun duplicate()
        fun address()
        fun back()
        fun forward()
        fun bookmarks(dialog: Boolean)
        fun history()
        fun settings()
        fun perform(action: MenuAction)
    }
    fun execute(action: NativeAction) {
        if (NativePageScroller.scroll(host.current()?.liveView, action)) return
        when (action) {
            NativeAction.NONE -> Unit
            NativeAction.ADDRESS -> host.address()
            NativeAction.NEW_TAB -> host.newTab()
            NativeAction.CLOSE_TAB -> host.close(host.selectedIndex())
            NativeAction.CLOSE_ALL -> host.closeAll()
            NativeAction.PREVIOUS_TAB, NativeAction.NEXT_TAB -> {
                val size = host.tabCount()
                if (size > 0) host.select((host.selectedIndex() + if (action == NativeAction.PREVIOUS_TAB) -1 else 1).let { if (it < 0) size - 1 else it % size })
            }
            NativeAction.DUPLICATE -> host.duplicate()
            NativeAction.BACK -> host.back()
            NativeAction.FORWARD -> host.forward()
            NativeAction.BOOKMARKS -> host.bookmarks(false)
            NativeAction.BOOKMARK_DIALOG -> host.bookmarks(true)
            NativeAction.HISTORY -> host.history()
            NativeAction.SETTINGS -> host.settings()
            else -> MENU[action]?.let(host::perform)
        }
    }
    private companion object {
        val MENU = mapOf(NativeAction.RELOAD to MenuAction.RELOAD, NativeAction.ADD_BOOKMARK to MenuAction.ADD_BOOKMARK,
            NativeAction.FIND to MenuAction.FIND, NativeAction.TRANSLATE to MenuAction.TRANSLATE, NativeAction.SAVE to MenuAction.SAVE,
            NativeAction.SOURCE to MenuAction.SOURCE, NativeAction.READ_ALOUD to MenuAction.READ_ALOUD, NativeAction.READER to MenuAction.READER,
            NativeAction.DOWNLOADS to MenuAction.DOWNLOADS, NativeAction.AI to MenuAction.AI)
    }
}
