package moe.via.browser.feature.internalpages

import moe.via.browser.feature.network.PageRequest

/** Immutable log values; a document job cannot retain the originating tab or window. */
data class GeneratedNetworkSource(val tabId: String, val requests: List<PageRequest>)
