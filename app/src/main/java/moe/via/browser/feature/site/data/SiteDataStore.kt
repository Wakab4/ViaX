package moe.via.browser.feature.site.data

import android.content.Context
import android.webkit.WebStorage
import androidx.webkit.WebStorageCompat
import androidx.webkit.WebViewFeature
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

internal data class SiteDataEntry(val site: String, val addresses: List<SiteDataAddress>, val bytes: Long)
internal data class SiteDataSnapshot(val entries: List<SiteDataEntry>, val shared: SharedSiteData)

internal class SiteDataStore(context: Context) {
    private data class HostUsage(val address: SiteDataAddress, val bytes: Long)

    private val files = SiteDataFiles(context.applicationContext)

    val canClearSite: Boolean get() = WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA)

    fun reportedOrigins(completed: (Map<String, Long>) -> Unit) {
        WebStorage.getInstance().getOrigins { origins ->
            val sizes = origins?.values.orEmpty().filterIsInstance<WebStorage.Origin>()
                .associate { it.origin to it.usage }
            completed(sizes)
        }
    }

    fun snapshot(reported: Map<String, Long>): SiteDataSnapshot {
        val origins = files.indexedDbBytes().toMutableMap()
        for ((origin, bytes) in reported) {
            val address = SiteDataAddress.parse(origin) ?: continue
            // IndexedDB files are also covered by the quota API when it reports usage.
            if (bytes > 0) origins[address] = bytes
        }
        val byHost = mutableMapOf<String, HostUsage>()
        fun add(address: SiteDataAddress, bytes: Long) {
            val previous = byHost[address.host]
            byHost[address.host] = HostUsage(previous?.address ?: address, (previous?.bytes ?: 0) + bytes)
        }
        origins.entries.sortedByDescending { it.key.origin.startsWith("https://") }.forEach { (address, bytes) ->
            add(address, bytes)
        }
        for ((host, bytes) in files.cookieBytes()) {
            val address = byHost[host]?.address ?: SiteDataAddress.parse(host, allowBareHost = true) ?: continue
            add(address, bytes)
        }
        // WebView deletes by registrable domain, including all of its subdomains.
        val entries = byHost.values.sortedBy { it.address.host }.groupBy { it.address.site }
            .map { (site, hosts) -> SiteDataEntry(site, hosts.map { it.address }, hosts.sumOf { it.bytes }) }
            .sortedBy { it.site }
        return SiteDataSnapshot(entries, files.sharedUsage())
    }

    fun clearSite(site: String, completed: () -> Unit) {
        WebStorageCompat.deleteBrowsingDataForSite(WebStorage.getInstance(), site, completed)
    }

    fun clearSites(sites: List<String>, completed: (Set<String>) -> Unit) {
        val pending = ArrayDeque(sites.distinct())
        val cleared = linkedSetOf<String>()
        fun clearNext() {
            while (pending.isNotEmpty()) {
                val site = pending.removeFirst()
                try {
                    clearSite(site) {
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
}
