package moe.via.browser.feature.site.data

import android.content.Context
import android.webkit.WebStorage
import androidx.webkit.WebStorageCompat
import androidx.webkit.WebViewFeature
import moe.via.browser.feature.library.history.data.HistoryRepository
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal data class SiteDataAddress(val url: String, val origin: String, val host: String, val site: String) {
    companion object {
        fun parse(raw: String, allowBareHost: Boolean = false): SiteDataAddress? {
            val text = raw.trim()
            if (text.isEmpty() || text.any(Char::isWhitespace)) return null
            val input = if (allowBareHost && "://" !in text) "https://$text" else text
            val parsed = input.toHttpUrlOrNull() ?: return null
            val address = parsed.newBuilder().username("").password("").query(null).fragment(null).build()
            return SiteDataAddress(
                address.toString(),
                address.newBuilder().encodedPath("/").build().toString().removeSuffix("/"),
                address.host,
                address.topPrivateDomain() ?: address.host,
            )
        }
    }
}

internal data class SiteDataEntry(val address: SiteDataAddress, val reportedBytes: Long?)

internal class SiteDataStore(context: Context) {
    private val app = context.applicationContext

    val canClearSite: Boolean get() = WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA)

    fun reportedOrigins(completed: (Map<String, Long>) -> Unit) {
        WebStorage.getInstance().getOrigins { origins ->
            val sizes = origins?.values.orEmpty().filterIsInstance<WebStorage.Origin>()
                .associate { it.origin to it.usage }
            completed(sizes)
        }
    }

    fun entries(reported: Map<String, Long>): List<SiteDataEntry> {
        val sizes = reported.mapNotNull { (origin, bytes) ->
            SiteDataAddress.parse(origin)?.let { it to bytes }
        }
        val addresses = linkedMapOf<String, SiteDataAddress>()
        for (visit in HistoryRepository(app).use { it.recent(limit = 1000) }) {
            val address = visit.url?.let { SiteDataAddress.parse(it) } ?: continue
            addresses.putIfAbsent(address.origin, address)
        }
        for ((address, _) in sizes) addresses.putIfAbsent(address.origin, address)
        val bytesByOrigin = sizes.associate { (address, bytes) -> address.origin to bytes }
        return addresses.values.map { SiteDataEntry(it, bytesByOrigin[it.origin]) }
    }

    fun clearSite(address: SiteDataAddress, completed: () -> Unit) {
        deleteSite(address.host, completed)
    }

    fun clearSites(sites: List<String>, completed: (Set<String>) -> Unit) {
        val pending = ArrayDeque(sites.distinct())
        val cleared = linkedSetOf<String>()
        fun clearNext() {
            while (pending.isNotEmpty()) {
                val site = pending.removeFirst()
                try {
                    deleteSite(site) {
                        cleared.add(site)
                        clearNext()
                    }
                    return
                } catch (_: IllegalArgumentException) {
                    // A rejected site should not stop the remaining selections.
                } catch (_: UnsupportedOperationException) {
                    break
                }
            }
            completed(cleared)
        }
        clearNext()
    }

    private fun deleteSite(host: String, completed: () -> Unit) {
        WebStorageCompat.deleteBrowsingDataForSite(WebStorage.getInstance(), host, completed)
    }
}
