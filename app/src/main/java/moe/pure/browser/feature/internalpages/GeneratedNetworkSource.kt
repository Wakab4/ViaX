package moe.pure.browser.feature.internalpages

import moe.pure.browser.feature.network.PageRequest

/** Immutable log values; a document job cannot retain the originating tab or window. */
data class GeneratedNetworkSource(val tabId: String, val requests: List<PageRequest>)
