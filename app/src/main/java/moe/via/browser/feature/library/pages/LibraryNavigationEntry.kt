package moe.via.browser.feature.library.pages

import android.app.Activity
import moe.via.browser.browser.ui.host.BrowserRootFragment
import moe.via.browser.feature.library.LibraryTab
import moe.via.browser.feature.library.ui.recycler.LibraryProcessState
import moe.via.browser.ui.components.recycler.RecyclerScrollState
import moe.via.browser.ui.pages.RecordPageScope
import moe.via.browser.ui.theme.ThemedActivity

object LibraryNavigationEntry {
    fun open(activity: Activity, tab: LibraryTab, query: String = "", folderId: String? = null, position: Int = 0, offset: Int = 0) {
        if (folderId != null || query.isNotEmpty()) LibraryProcessState.save(tab, folderId.orEmpty(), query, RecyclerScrollState(position, offset), 0)
        val root = requireNotNull(BrowserRootFragment.find(activity as ThemedActivity))
        if (root.isVisible && !root.isRemoving) RecordPageScope.of(root)?.push(LibraryFragment::class.java, LibraryFragment.args(tab))
    }
}
