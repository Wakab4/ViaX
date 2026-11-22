package moe.via.browser.feature.library.dialog

import android.os.Bundle
import android.os.Parcelable

/** The view hierarchy restores independently from the opening directory. */
internal data class BookmarkDialogAnchor(
    val folder: String,
    val scroll: BookmarkDialogState.Scroll,
    val hierarchy: Parcelable? = null
)

/** DialogFragment arguments and view state are separate from Native's process-only directory cache. */
internal data class BookmarkDialogRecreationState(
    val initialFolder: String?,
    val anchor: BookmarkDialogAnchor?
) {
    fun bundle() = Bundle().apply {
        putInt(VERSION, 3)
        putBoolean(VISIBLE, true)
        putBoolean(DEFAULT_RESTORE, initialFolder == null)
        initialFolder?.let { putString(INITIAL_FOLDER, it) }
        anchor?.let {
            putString(ANCHOR_FOLDER, it.folder)
            putInt(POSITION, it.scroll.position)
            putInt(OFFSET, it.scroll.offset)
            it.hierarchy?.let { hierarchy -> putParcelable(LIST_STATE, hierarchy) }
        }
    }

    companion object {
        private const val VERSION = "version"
        private const val VISIBLE = "visible"
        private const val DEFAULT_RESTORE = "default_restore"
        private const val INITIAL_FOLDER = "initial_folder"
        private const val ANCHOR_FOLDER = "anchor_folder"
        private const val POSITION = "position"
        private const val OFFSET = "offset"
        private const val LIST_STATE = "list_state"

        @Suppress("DEPRECATION")
        fun read(state: Bundle): BookmarkDialogRecreationState? {
            val version = state.getInt(VERSION)
            if (version != 3 || !state.getBoolean(VISIBLE)) return null
            val initialFolder = if (state.getBoolean(DEFAULT_RESTORE)) null else {
                if (!state.containsKey(INITIAL_FOLDER)) return null
                state.getString(INITIAL_FOLDER) ?: return null
            }
            val anchor = if (state.containsKey(ANCHOR_FOLDER)) {
                val folder = state.getString(ANCHOR_FOLDER).orEmpty()
                BookmarkDialogAnchor(
                    folder,
                    BookmarkDialogState.Scroll(state.getInt(POSITION).coerceAtLeast(0), state.getInt(OFFSET)),
                    state.getParcelable(LIST_STATE)
                )
            } else null
            return BookmarkDialogRecreationState(initialFolder, anchor)
        }
    }
}
