package moe.via.browser.feature.proxy

import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL

internal enum class ProxyChange { APPLIED, UNSUPPORTED, FAILED, BUSY }

/** Process-wide WebView override and explicit routing for built-in downloads. */
internal object BrowserProxy {
    private lateinit var store: BrowserProxyStore
    private lateinit var context: Context
    private var saved: ProxySettings? = null
    private var downloadRelay: LocalProxyRelay? = null
    private var webRelay: LocalProxyRelay? = null
    private var applying = false
    private val startupWaiters = mutableListOf<(ProxyChange) -> Unit>()
    private val state = MutableLiveData(ProxySettings())
    val updates: LiveData<ProxySettings> get() = state

    fun initialize(context: Context) {
        this.context = context.applicationContext
        store = BrowserProxyStore(this.context)
    }

    @Synchronized private fun savedSettings(): ProxySettings = saved ?: store.read().also { saved = it }

    fun configuration(): ProxySettings = runCatching { savedSettings() }.getOrDefault(ProxySettings())

    fun openDownload(url: URL): HttpURLConnection {
        val proxy = synchronized(this) {
            val settings = savedSettings()
            if (!settings.enabled) null else {
                val route = downloadRelay ?: LocalProxyRelay(requireNotNull(settings.endpoint)).also { downloadRelay = it }
                Proxy(Proxy.Type.HTTP, route.address)
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
        if (!settings.enabled || webRelay != null) {
            state.value = settings
            completed(ProxyChange.APPLIED)
        } else apply(settings, persist = false, completed)
    }

    @MainThread fun configure(endpoint: ProxyEndpoint, enabled: Boolean, completed: (ProxyChange) -> Unit) {
        apply(ProxySettings(endpoint, enabled), persist = true, completed)
    }

    @MainThread private fun apply(settings: ProxySettings, persist: Boolean, completed: (ProxyChange) -> Unit) {
        if (applying) { completed(ProxyChange.BUSY); return }
        if (settings.enabled && !WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
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
            val encoded = if (persist && runCatching { savedSettings().endpoint }.getOrNull() != settings.endpoint) {
                settings.endpoint?.let(store::encode)
            } else null
            val next = if (settings.enabled) {
                synchronized(this) { downloadRelay?.takeIf { it.upstream == settings.endpoint } }
                    ?: LocalProxyRelay(requireNotNull(settings.endpoint)).also { candidate = it }
            } else null
            val applied = Runnable {
                synchronized(this) {
                    if (persist) store.write(settings.enabled, encoded)
                    downloadRelay?.takeIf { it !== next }?.close()
                    downloadRelay = next
                    webRelay = next
                    saved = settings
                }
                state.value = settings
                finish(ProxyChange.APPLIED)
            }
            when {
                next === webRelay -> applied.run()
                next == null -> ProxyController.getInstance().clearProxyOverride(context.mainExecutor, applied)
                else -> {
                    val config = ProxyConfig.Builder().addProxyRule(next.rule).removeImplicitRules().build()
                    ProxyController.getInstance().setProxyOverride(config, context.mainExecutor, applied)
                }
            }
        } catch (_: Exception) {
            candidate?.close()
            finish(ProxyChange.FAILED)
        }
    }
}
