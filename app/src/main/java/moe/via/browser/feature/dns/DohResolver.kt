package moe.via.browser.feature.dns

import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.Proxy
import java.util.concurrent.TimeUnit

internal class DohResolver(server: DohServer) {
    private val dns = DnsOverHttps.Builder()
        .client(client)
        .url(server.url)
        .resolvePrivateAddresses(true)
        .apply {
            if (server.bootstrap.isNotEmpty()) {
                bootstrapDnsHosts(server.bootstrap.map { requireNotNull(numericAddress(it)) })
            }
        }
        .build()

    fun lookup(host: String) = numericAddress(host)?.let(::listOf) ?: dns.lookup(host)

    private companion object {
        val client = OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .followRedirects(false)
            .callTimeout(8, TimeUnit.SECONDS)
            .build()
    }
}
