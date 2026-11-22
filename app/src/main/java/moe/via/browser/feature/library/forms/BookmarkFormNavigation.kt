package moe.via.browser.feature.library.forms

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import moe.via.browser.ui.pages.RecordPageFragment
import moe.via.browser.ui.pages.RecordPageScope

internal object BookmarkFormNavigation {
    const val BOOKMARK = "a8.g1"
    const val FOLDER = "a8.d0"
    const val PICKER = "a8.b2"
    fun bookmark(owner: Fragment, listener: LifecycleOwner, id: String?, title: String, url: String,
        folder: String, favorite: Boolean, showHidden: Boolean, changed: () -> Unit) {
        val args = Bundle().apply {
            if (id == null) { putString("title", title); putString("url", url); putString("folderId", folder) } else putString("id", id)
            putBoolean("favorite_enabled", favorite)
            putBoolean("show_hidden_folders", showHidden)
        }
        open(owner, listener, BookmarkEditorFragment::class.java, args, BOOKMARK) { changed() }
    }
    fun folder(owner: Fragment, listener: LifecycleOwner, id: String?, parent: String, creation: Boolean, showHidden: Boolean, changed: (String?) -> Unit) {
        val args = Bundle().apply {
            if (id != null) putString("id", id)
            putString("parent_folder_id", parent)
            putBoolean("creation_enabled", creation)
            putBoolean("show_hidden_folders", showHidden)
        }
        open(owner, listener, BookmarkFolderEditorFragment::class.java, args, FOLDER) { changed(it.getString("id")) }
    }
    fun picker(owner: Fragment, listener: LifecycleOwner, selected: String, creation: Boolean, showHidden: Boolean, changed: (String) -> Unit) {
        val args = Bundle().apply {
            putString("selected_id", selected)
            putBoolean("creation_enabled", creation)
            putBoolean("show_hidden_folders", showHidden)
        }
        open(owner, listener, BookmarkFolderPickerFragment::class.java, args, PICKER) { result -> result.getString("result_id")?.let(changed) }
    }
    private fun open(owner: Fragment, listener: LifecycleOwner, type: Class<out RecordPageFragment>, args: Bundle,
        key: String, result: (Bundle) -> Unit) {
        val manager = owner.parentFragmentManager
        manager.setFragmentResultListener(key, listener) { _, bundle -> result(bundle); manager.clearFragmentResultListener(key) }
        if (owner.isVisible && !owner.isRemoving) RecordPageScope.of(owner)?.push(type, args)
    }
}
