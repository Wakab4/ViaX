package moe.pure.browser.feature.library.pages

import android.app.Activity
import moe.pure.browser.browser.host.BrowserRootFragment
import moe.pure.browser.feature.library.LibraryTab
import moe.pure.browser.feature.library.ui.recycler.LibraryProcessState
import moe.pure.browser.feature.library.ui.recycler.RecyclerScrollState
import moe.pure.browser.ui.pages.RecordPageScope
import moe.pure.browser.ui.theme.ThemedActivity

object LibraryNavigationEntry {
    fun open(activity: Activity, tab: LibraryTab, query: String = "", folderId: String? = null, position: Int = 0, offset: Int = 0) {
        if (folderId != null || query.isNotEmpty()) LibraryProcessState.save(tab, folderId.orEmpty(), query, RecyclerScrollState(position, offset), 0)
        val root = requireNotNull(BrowserRootFragment.find(activity as ThemedActivity))
        if (root.isVisible && !root.isRemoving) RecordPageScope.of(root)?.push(LibraryFragment::class.java, LibraryFragment.args(tab))
    }
}
