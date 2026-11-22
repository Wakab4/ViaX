package moe.via.browser.feature.library.pages

import android.os.Bundle
import android.view.View
import moe.via.browser.feature.library.LibraryTab

class OfflinePageFragment : LibraryItemsPageFragment() {
    override val tab = LibraryTab.OFFLINE
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); reload() }
    override fun reload() { val search = query; work({ repository.offline(search) }) { show(it) } }
}
