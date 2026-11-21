package moe.pure.browser.feature.library.ui

import android.app.Activity
import android.view.View
import moe.pure.browser.R
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.dialog.DialogScope

/** cb.e0.a4/b4: both lists use the left footer action's original raw anchor offset. */
internal class BookmarkMoreMenuController(private val activity: Activity, private val host: Host) : AutoCloseable {
    data class State(val root: Boolean, val details: Boolean, val order: Int)
    interface Host {
        fun state(): State
        fun addBookmark()
        fun addFolder()
        fun setOrder(order: Int)
        fun setDetails(details: Boolean)
        fun importBookmarks()
        fun exportBookmarks()
    }
    private enum class Action(val label: Int) {
        ADD(R.string.ref_bookmark_more_add), FOLDER(R.string.ref_bookmark_more_folder),
        SORT(R.string.ref_bookmark_more_sort), HIDE_DETAILS(R.string.ref_bookmark_more_hide_details),
        SHOW_DETAILS(R.string.ref_bookmark_more_show_details), IMPORT(R.string.ref_bookmark_more_import),
        EXPORT(R.string.ref_bookmark_more_export)
    }
    private val scope = DialogScope()
    private var closed = false

    fun show(anchor: View) {
        if (closed || activity.isFinishing || activity.isDestroyed) return
        val state = host.state()
        val actions = buildList {
            add(Action.ADD); add(Action.FOLDER); add(Action.SORT)
            add(if (state.details) Action.HIDE_DETAILS else Action.SHOW_DETAILS)
            if (state.root) { add(Action.IMPORT); add(Action.EXPORT) }
        }
        scope.show(position(BrowserDialog(activity).items(actions.map { activity.getString(it.label) }) { index ->
            if (closed) return@items
            when (actions[index]) {
                Action.ADD -> host.addBookmark()
                Action.FOLDER -> host.addFolder()
                Action.SORT -> sort(anchor)
                Action.HIDE_DETAILS -> host.setDetails(false)
                Action.SHOW_DETAILS -> host.setDetails(true)
                Action.IMPORT -> host.importBookmarks()
                Action.EXPORT -> host.exportBookmarks()
            }
        }, anchor))
    }

    private fun sort(anchor: View) {
        val labels = listOf(R.string.ref_bookmark_more_sort_oldest, R.string.ref_bookmark_more_sort_newest,
            R.string.ref_bookmark_more_sort_name).map(activity::getString)
        scope.show(position(BrowserDialog(activity).singleChoice(labels, host.state().order) { order ->
            if (!closed) host.setOrder(order)
        }, anchor))
    }

    private fun position(dialog: BrowserDialog, anchor: View): BrowserDialog {
        val point = IntArray(2)
        anchor.getLocationOnScreen(point)
        return dialog.anchoredAt(point[0] - anchor.width, point[1])
    }

    override fun close() { if (!closed) { closed = true; scope.dispose() } }
}
