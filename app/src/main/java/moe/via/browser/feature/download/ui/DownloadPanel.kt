package moe.via.browser.feature.download.ui

import android.os.Bundle
import moe.via.browser.feature.download.DownloadCategory

/** Window-owned UI values. The old browser ContentOverlay is no longer a reachable download host. */
class DownloadPanelState {
    var query = ""
    var category = DownloadCategory.ALL
    var editing = false
    val selected = mutableSetOf<Long>()

    fun save() = Bundle().apply {
        putString("query", query); putString("category", category.name)
        putBoolean("editing", editing); putLongArray("selected", selected.toLongArray())
    }

    fun restore(bundle: Bundle?) {
        bundle ?: return
        query = bundle.getString("query").orEmpty()
        category = runCatching { DownloadCategory.valueOf(bundle.getString("category").orEmpty()) }.getOrDefault(DownloadCategory.ALL)
        editing = bundle.getBoolean("editing")
        selected.clear(); bundle.getLongArray("selected")?.let { selected.addAll(it.toList()) }
    }
}
