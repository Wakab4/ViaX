package moe.via.browser.browser.ui.tabs

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.view.View
import android.widget.FrameLayout
import moe.via.browser.R
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.browser.ui.tabs.switcher.TabSwitcherItem
import moe.via.browser.browser.ui.tabs.switcher.TabSwitcherPopup

/** f8.l0 coordinator: the popup owns layout/gestures; tab mutations remain with BrowserSession. */
class TabSwitcher(private val activity: Activity, private val parent: FrameLayout, private val actions: Actions) {
    interface Actions {
        fun tabs(): List<BrowserTab>
        fun selectedIndex(): Int
        fun select(index: Int)
        fun close(indices: List<Int>)
        fun create()
        fun duplicate(index: Int)
        fun move(from: Int, to: Int)
        fun closedTabs() {}
    }
    private var popup: TabSwitcherPopup? = null
    private var menus: DialogScope? = null
    private val retiring = mutableSetOf<TabSwitcherPopup>()
    private var ids = emptyList<String>()
    val isShowing get() = popup != null

    fun show() {
        if (close()) return
        if (activity.isFinishing || activity.isDestroyed) return
        val data = snapshot()
        ids = data.map { it.id }
        val owner = TabSwitcherPopup(activity, parent, object : TabSwitcherPopup.Actions {
            override fun dismiss() { this@TabSwitcher.close() }
            override fun select(id: String) { index(id)?.let { this@TabSwitcher.close(); actions.select(it) } }
            override fun close(id: String) { index(id)?.let { actions.close(listOf(it)); refresh() } }
            override fun context(anchor: View, id: String) = contextMenu(anchor, id)
            override fun move(from: String, to: String): Boolean {
                val source = index(from) ?: return false
                val target = index(to) ?: return false
                if (source == target) return false
                actions.move(source, target)
                refresh()
                return actions.tabs().getOrNull(target)?.id == from
            }
            override fun create() { actions.create() }
            override fun closedTabs() { this@TabSwitcher.close(); actions.closedTabs() }
        })
        menus = DialogScope(); popup = owner
        owner.show(data)
    }

    fun refresh() {
        val owner = popup ?: return
        val data = snapshot()
        // s6.Z closes the fragment on tab creation; deletion and reordering retain it.
        if (data.any { it.id !in ids }) { close(); return }
        ids = data.map { it.id }
        owner.render(data)
    }

    fun close(animate: Boolean = true): Boolean {
        val owner = popup
        if (owner != null) {
            popup = null; ids = emptyList()
            menus?.dispose(); menus = null
            retiring.add(owner)
            owner.close(animate) { retiring.remove(owner) }
        }
        if (!animate) retiring.toList().forEach(TabSwitcherPopup::dispose)
        return owner != null
    }

    private fun snapshot() = actions.tabs().mapIndexed { index, tab -> TabSwitcherItem.capture(activity, tab, index == actions.selectedIndex()) }
    private fun index(id: String) = actions.tabs().indexOfFirst { it.id == id }.takeIf { it >= 0 }

    private fun contextMenu(anchor: View, id: String) {
        val position = index(id) ?: return
        val data = snapshot()
        val item = data.getOrNull(position) ?: return
        val commands = buildList {
            if (data.size > 1) { add(4 to R.string.ref_tab_switcher_close_all); add(1 to R.string.ref_tab_switcher_close_others) }
            if (position != 0 && position != data.lastIndex) { add(3 to R.string.ref_tab_switcher_close_above); add(2 to R.string.ref_tab_switcher_close_below) }
            if (item.url.length > 6 && (item.url.startsWith("http://", true) || item.url.startsWith("https://", true))) add(6 to R.string.ref_tab_switcher_duplicate)
            if (!item.url.startsWith("file://", true)) add(5 to R.string.ref_tab_switcher_copy_link)
        }
        if (commands.isEmpty()) return
        menus?.show(BrowserDialog(activity).items(commands.map { activity.getString(it.second) }) { choice ->
            // f8.l0 captures the menu's original position and URL, even if tabs change behind it.
            val current = position
            val tabs = actions.tabs()
            when (val command = commands[choice].first) {
                1, 2, 3, 4 -> {
                    val selected = actions.selectedIndex()
                    if (command == 4 || command == 1 && current != selected || command == 2 && selected > current || command == 3 && selected < current) close()
                    val indices = when (command) {
                        4 -> tabs.indices.toList()
                        1 -> tabs.indices.filter { it != current }
                        2 -> (current + 1..tabs.lastIndex).toList()
                        else -> (0 until current).toList()
                    }
                    actions.close(indices); refresh()
                }
                5 -> {
                    activity.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, item.url))
                    BrowserMessage(activity).show(activity.getString(R.string.ref_tab_switcher_copied))
                }
                6 -> { close(); actions.duplicate(current) }
            }
        }.anchoredTo(anchor))
    }
}
