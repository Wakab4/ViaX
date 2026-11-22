package moe.via.browser.feature.library.navigation

import android.app.Activity
import android.view.View
import moe.via.browser.R
import moe.via.browser.feature.library.LibraryEntry
import moe.via.browser.feature.library.LibraryKind
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope

/** cb.e0.W3/X3 and their centered deletion confirmations; no record title is added to the list. */
internal class LibraryContextMenuController(private val activity: Activity, private val host: Host) : AutoCloseable {
    interface Host {
        fun open(entry: LibraryEntry, where: LibraryOpenWhere)
        fun edit(entry: LibraryEntry)
        fun favorite(entry: LibraryEntry)
        fun delete(entries: List<LibraryEntry>)
        fun copy(entry: LibraryEntry)
        fun share(entry: LibraryEntry)
    }
    private enum class Action(val label: Int) {
        BACKGROUND(R.string.ref_library_dialog_background), FOREGROUND(R.string.ref_library_dialog_foreground),
        EDIT(R.string.ref_library_dialog_edit), FAVORITE(R.string.ref_library_dialog_favorite),
        DELETE(R.string.ref_library_dialog_delete), COPY(R.string.ref_library_dialog_copy), SHARE(R.string.ref_library_dialog_share)
    }
    private val scope = DialogScope()
    private var closed = false

    fun show(anchor: View, entry: LibraryEntry) {
        if (closed || activity.isFinishing || activity.isDestroyed) return
        val actions = when (entry.kind) {
            LibraryKind.FOLDER -> listOf(Action.BACKGROUND, Action.FOREGROUND, Action.EDIT, Action.FAVORITE, Action.DELETE)
            LibraryKind.BOOKMARK -> listOf(Action.BACKGROUND, Action.FOREGROUND, Action.EDIT, Action.FAVORITE, Action.DELETE, Action.COPY, Action.SHARE)
            // Keep the existing offline actions; PDF/export and its file-sharing protocol are separate missing work.
            else -> listOf(Action.FOREGROUND, Action.SHARE, Action.DELETE)
        }
        scope.show(BrowserDialog(activity).items(actions.map { activity.getString(it.label) }) { index ->
            if (closed) return@items
            when (actions[index]) {
                Action.BACKGROUND -> host.open(entry, LibraryOpenWhere.BACKGROUND)
                Action.FOREGROUND -> host.open(entry, LibraryOpenWhere.FOREGROUND)
                Action.EDIT -> host.edit(entry)
                Action.FAVORITE -> host.favorite(entry)
                Action.DELETE -> deleteOne(entry)
                Action.COPY -> host.copy(entry)
                Action.SHARE -> host.share(entry)
            }
        }.anchoredTo(anchor))
    }

    private fun deleteOne(entry: LibraryEntry) {
        val title = activity.getString(if (entry.isFolder) R.string.ref_library_dialog_delete_folder_title else R.string.ref_library_dialog_delete)
        val message = activity.getString(if (entry.isFolder) R.string.ref_library_dialog_delete_folder_message else R.string.ref_library_dialog_delete_item_message, entry.title)
        confirm(title, message) { host.delete(listOf(entry)) }
    }

    fun deleteSelected(values: () -> List<LibraryEntry>) {
        if (closed) return
        val snapshot = values()
        if (snapshot.isEmpty()) return
        val offline = snapshot.all { it.kind == LibraryKind.OFFLINE }
        val message = when {
            !offline -> activity.getString(R.string.ref_library_dialog_delete_selected_message)
            snapshot.size == 1 -> activity.getString(R.string.ref_library_dialog_delete_item_message, snapshot.first().title)
            else -> activity.getString(R.string.ref_library_dialog_delete_multiple_message, snapshot.size)
        }
        confirm(activity.getString(R.string.ref_library_dialog_delete), message) {
            // The bookmark footer reads selection at confirmation; Native offline confirmation captures its positions.
            val current = if (offline) snapshot else values()
            if (current.isNotEmpty()) host.delete(current)
        }
    }

    private fun confirm(title: String, message: String, confirmed: () -> Unit) {
        var submitted = false
        scope.show(BrowserDialog(activity, title, message).positive(activity.getString(android.R.string.ok)) {
            if (!closed && !submitted) { submitted = true; confirmed() }
            true
        }.negative(activity.getString(android.R.string.cancel)))
    }
    override fun close() { if (!closed) { closed = true; scope.dispose() } }
}
