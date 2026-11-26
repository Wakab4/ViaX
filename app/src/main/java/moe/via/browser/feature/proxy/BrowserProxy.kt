package moe.via.browser.feature.proxy

import android.content.Context
import androidx.annotation.MainThread
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import moe.via.browser.feature.dns.WebDnsSettings
import moe.via.browser.feature.dns.WebDnsStore
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL

internal enum class ProxyChange { APPLIED, UNSUPPORTED, FAILED, BUSY }

/** Process-wide WebView override and explicit routing for built-in downloads. */
internal object BrowserProxy {
    private enum class Change { STARTUP, PROXY, DNS }

    private lateinit var store: BrowserProxyStore
    private lateinit var dnsStore: WebDnsStore
    private lateinit var context: Context
    private var saved: ProxySettings? = null
    private var downloadRelay: LocalProxyRelay? = null
    private var webRelay: LocalProxyRelay? = null
    private var applying = false
    private val startupWaiters = mutableListOf<(ProxyChange) -> Unit>()

    fun initialize(context: Context) {
        this.context = context.applicationContext
        store = BrowserProxyStore(this.context)
        dnsStore = WebDnsStore(this.context)
    }

    @Synchronized private fun savedSettings(): ProxySettings = saved ?: store.read().also { saved = it }

    fun configuration(): ProxySettings = runCatching { savedSettings() }.getOrDefault(ProxySettings())

    fun dnsConfiguration(): WebDnsSettings = runCatching { dnsStore.read() }.getOrDefault(WebDnsSettings())

    fun openDownload(url: URL): HttpURLConnection {
        val proxy = synchronized(this) {
            val settings = savedSettings()
            if (!settings.enabled) null else {
                val relay = downloadRelay ?: LocalProxyRelay(RelayRoute.Upstream(requireNotNull(settings.endpoint)))
                    .also { downloadRelay = it }
                Proxy(Proxy.Type.HTTP, relay.address)
            }
        }
        return (if (proxy == null) url.openConnection() else url.openConnection(proxy)) as HttpURLConnection
    }

    @MainThread fun prepareWebView(completed: (ProxyChange) -> Unit) {
        if (applying) {
            startupWaiters.add(completed)
            return
        }
        val settings = try { savedSettings() } catch (_: Exception) { completed(ProxyChange.FAILED); return }
        apply(settings, dnsConfiguration(), Change.STARTUP, completed)
    }

    @MainThread fun configure(endpoint: ProxyEndpoint, enabled: Boolean, completed: (ProxyChange) -> Unit) {
        apply(ProxySettings(endpoint, enabled), dnsConfiguration(), Change.PROXY, completed)
    }

    @MainThread fun configureDns(settings: WebDnsSettings, completed: (ProxyChange) -> Unit) {
        apply(configuration(), settings, Change.DNS, completed)
    }

    @MainThread private fun apply(
        settings: ProxySettings,
        dns: WebDnsSettings,
        change: Change,
        completed: (ProxyChange) -> Unit,
    ) {
        if (applying) { completed(ProxyChange.BUSY); return }
        val route = if (settings.enabled) RelayRoute.Upstream(requireNotNull(settings.endpoint))
            else dns.selected?.let { RelayRoute.SecureDns(it) }
        if (route != null && !WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            completed(ProxyChange.UNSUPPORTED)
            return
        }
        applying = true
        var candidate: LocalProxyRelay? = null
        fun finish(result: ProxyChange) {
            applying = false
            val pending = startupWaiters.toList()
            startupWaiters.clear()
            completed(result)
            pending.forEach { prepareWebView(it) }
        }
        try {
            val encoded = if (change == Change.PROXY && configuration().endpoint != settings.endpoint) {
                settings.endpoint?.let(store::encode)
            } else null
            val next = route?.let {
                webRelay?.takeIf { relay -> relay.route == route }
                    ?: synchronized(this) { downloadRelay?.takeIf { relay -> relay.route == route } }
                    ?: LocalProxyRelay(route).also { candidate = it }
            }
            val applied = Runnable {
                synchronized(this) {
                    if (change == Change.PROXY) {
                        store.write(settings.enabled, encoded)
                        saved = settings
                    }
                    if (change == Change.DNS) dnsStore.write(dns)
                    val nextDownload = next.takeIf { route is RelayRoute.Upstream }
                    downloadRelay?.takeIf { it !== nextDownload && it !== webRelay }?.close()
                    webRelay?.takeIf { it !== next }?.close()
                    downloadRelay = nextDownload
                    webRelay = next
                }
                finish(ProxyChange.APPLIED)
            }
            when {
                next === webRelay -> applied.run()
                next == null -> ProxyController.getInstance().clearProxyOverride(context.mainExecutor, applied)
                else -> {
                    val config = ProxyConfig.Builder().addProxyRule(next.rule)
                        .apply { if (route is RelayRoute.Upstream) removeImplicitRules() }
                        .build()
                    ProxyController.getInstance().setProxyOverride(config, context.mainExecutor, applied)
                }
            }
        } catch (_: Exception) {
            candidate?.close()
            finish(ProxyChange.FAILED)
        }
    }
}
