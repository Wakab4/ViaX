package moe.pure.browser.feature.library.dialog

import android.app.Activity
import android.os.Bundle
import moe.pure.browser.core.logging.NativeLog as Log
import moe.pure.browser.feature.library.LibraryRepository
import moe.pure.browser.feature.library.ui.recycler.RecyclerScrollState

/** Native action 30 is a compact selector; editing and multi-selection stay in the full manager. */
class BookmarksDialogController(
    private val activity: Activity,
    private val host: Host,
    repository: LibraryRepository? = null
) {
    interface Host {
        fun open(url: String, newTab: Boolean)
        fun openManager(folderId: String, position: Int, offset: Int)
    }
    private val data = BookmarkDialogData(activity, repository)
    private var window: BookmarksDialogWindow? = null
    private var workspace: BookmarksDialogView? = null
    private var adapter: BookmarkDialogAdapter? = null
    private var folder: String? = null
    private var desiredFolder = ""
    private var initialFolder: String? = null
    private var restoreAnchor: BookmarkDialogAnchor? = null
    private var chosenUrl: String? = null
    private var disposed = false
    private var recreationHandled = false
    val isShowing get() = window?.isShowing == true

    /** A null folder restores only the process cache; an explicit ID starts without its old scroll anchor. */
    fun show(folderId: String? = null) {
        if (disposed || isShowing || activity.isFinishing || activity.isDestroyed) return
        recreationHandled = true
        open(folderId, null)
    }

    private fun open(folderId: String?, hierarchy: BookmarkDialogAnchor?) {
        val cached = if (folderId == null) BookmarkDialogState.read() else null
        initialFolder = folderId
        desiredFolder = folderId ?: cached?.folder ?: ""
        // Native hierarchy restores independently before loading; only the process cache adds a later scroll override.
        restoreAnchor = cached?.let { BookmarkDialogAnchor(it.folder, it.scroll) }
        folder = null; chosenUrl = null
        val content = BookmarksDialogView(activity, ::manager)
        val rows = BookmarkDialogAdapter(activity)
        val dialog = BookmarksDialogWindow(activity, content)
        workspace = content; adapter = rows; window = dialog
        content.list.adapter = rows
        rows.clicked = clicked@{ row ->
            if (window !== dialog || !dialog.isShowing) return@clicked
            if (row.entry.isFolder) {
                desiredFolder = row.entry.id; restoreAnchor = null; load()
            } else {
                chosenUrl = row.entry.url
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener {
            if (window !== dialog) {
                content.close(); rows.close()
                return@setOnDismissListener
            }
            remember()
            data.cancel()
            val open = chosenUrl
            chosenUrl = null; restoreAnchor = null
            content.close(); rows.close()
            if (window === dialog) { workspace = null; adapter = null; window = null }
            if (!disposed && open != null) host.open(open, false)
        }
        load()
        (hierarchy?.hierarchy as? Bundle)?.let { state ->
            state.classLoader = javaClass.classLoader
            val dialogState = state.getBundle("dialog")
            if (dialogState != null) dialog.onRestoreInstanceState(dialogState)
        }
        dialog.show()
    }

    private fun load() {
        val current = window ?: return
        val requested = desiredFolder
        data.load(requested) { result ->
            if (disposed || window !== current || !current.isShowing) return@load
            result.onSuccess { snapshot ->
                folder = snapshot.id; desiredFolder = snapshot.id
                workspace?.title?.text = snapshot.title
                adapter?.rows = snapshot.rows
                workspace?.showEmpty(snapshot.rows.isEmpty())
                val anchor = restoreAnchor
                restoreAnchor = null
                if (anchor != null && requested == snapshot.id && snapshot.rows.isNotEmpty()) {
                    workspace?.list?.let { RecyclerScrollState(anchor.scroll.position.coerceIn(snapshot.rows.indices), anchor.scroll.offset).restore(it) }
                }
            }.onFailure { Log.warn("BookmarkDialog", "Cannot load bookmark folder", it) }
        }
    }

    private fun manager() {
        val selected = folder ?: return
        val scroll = workspace?.scroll() ?: BookmarkDialogState.Scroll()
        remember()
        window?.dismiss()
        host.openManager(selected, scroll.position, scroll.offset)
    }
    private fun remember() {
        val selected = folder ?: return
        val scroll = workspace?.scroll() ?: return
        BookmarkDialogState.save(selected, scroll)
    }
    /** Native caches in its fragment pause hook; Root can forward its Activity pause without a new observer. */
    fun paused() = remember()

    /** Only the original opening arguments and list view state survive process death. */
    fun save(): Bundle? {
        if (disposed) return null
        if (window?.isShowing != true) return null
        remember()
        val content = workspace
        val anchor = if (content != null) {
            val hierarchy = Bundle().apply {
                window?.onSaveInstanceState()?.let { saved -> saved.putBoolean("android:dialogShowing", false); putBundle("dialog", saved) }
            }
            BookmarkDialogAnchor(folder ?: desiredFolder, content.scroll(), hierarchy)
        } else restoreAnchor
        return BookmarkDialogRecreationState(initialFolder, anchor).bundle()
    }

    /** Root calls this once after a live browser tab is ready, using this Activity's new Host. */
    fun restore(state: Bundle) {
        if (disposed || recreationHandled) return
        recreationHandled = true
        if (isShowing || activity.isFinishing || activity.isDestroyed) return
        val saved = BookmarkDialogRecreationState.read(state) ?: return
        open(saved.initialFolder, saved.anchor)
    }

    /** System Back closes the dialog. Parent navigation is the explicit immutable '..' row. */
    fun back(): Boolean {
        val current = window?.takeIf { it.isShowing } ?: return false
        current.dismiss(); return true
    }
    fun rebind() {
        if (disposed || !isShowing) return
        // Native onResume updates the window; it does not replace the folder dataset during item animations.
        remember(); window?.updateGeometry()
        adapter?.refresh()
    }
    fun dispose() {
        if (disposed) return
        remember(); disposed = true
        window?.dismiss(); window = null
        workspace = null; adapter = null
        data.close()
    }
}
