package moe.via.browser.feature.dns

import android.content.Context
import android.system.Os
import android.system.OsConstants
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject

internal data class DohServer(
    val id: String,
    val name: String,
    val url: HttpUrl,
    val bootstrap: List<String>,
) {
    companion object {
        fun url(text: String): HttpUrl? = text.trim().toHttpUrlOrNull()?.takeIf {
            it.isHttps && it.username.isEmpty() && it.password.isEmpty() && it.fragment == null
        }
    }
}

internal fun numericAddress(value: String) =
    Os.inet_pton(if (':' in value) OsConstants.AF_INET6 else OsConstants.AF_INET, value)

internal data class WebDnsSettings(
    val selectedId: String? = null,
    val custom: List<DohServer> = emptyList(),
) {
    val servers = PRESETS + custom
    val selected = servers.firstOrNull { it.id == selectedId }

    companion object {
        private val PRESETS = listOf(
            preset("alidns", "AliDNS", "dns.alidns.com", "223.5.5.5", "223.6.6.6", "2400:3200::1", "2400:3200:baba::1"),
            preset("cloudflare", "Cloudflare", "cloudflare-dns.com", "1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001"),
            preset("google", "Google Public DNS", "dns.google", "8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844"),
            preset("quad9", "Quad9", "dns.quad9.net", "9.9.9.9", "149.112.112.112", "2620:fe::fe", "2620:fe::9"),
        )

        private fun preset(id: String, name: String, host: String, vararg ips: String) =
            DohServer(id, name, requireNotNull(DohServer.url("https://$host/dns-query")), ips.toList())
    }
}

internal class WebDnsStore(context: Context) {
    private val values = context.getSharedPreferences("web_dns", Context.MODE_PRIVATE)

    fun read(): WebDnsSettings {
        val servers = JSONArray(values.getString("servers", "[]"))
        val custom = (0 until servers.length()).map { index ->
            val server = servers.getJSONObject(index)
            val bootstrap = server.getJSONArray("bootstrap")
            DohServer(
                server.getString("id"),
                server.getString("name"),
                requireNotNull(DohServer.url(server.getString("url"))),
                (0 until bootstrap.length()).map(bootstrap::getString),
            )
        }
        return WebDnsSettings(values.getString("selected", null), custom)
    }

    fun write(settings: WebDnsSettings) {
        val custom = JSONArray(settings.custom.map { server ->
            JSONObject().apply {
                put("id", server.id)
                put("name", server.name)
                put("url", server.url.toString())
                put("bootstrap", JSONArray(server.bootstrap))
            }
        })
        values.edit().putString("selected", settings.selectedId).putString("servers", custom.toString()).apply()
    }
}
