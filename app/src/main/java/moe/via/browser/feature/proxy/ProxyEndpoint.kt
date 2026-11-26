package moe.via.browser.feature.proxy

import android.util.Base64
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal enum class ProxyProtocol(val scheme: String) {
    HTTP("http"), HTTPS("https"), SOCKS5("socks5");

    companion object {
        fun fromAddress(address: String): ProxyProtocol? {
            val scheme = address.trim().substringBefore("://", "").lowercase()
            return if (scheme == "socks") SOCKS5 else entries.firstOrNull { it.scheme == scheme }
        }
    }
}

internal data class ProxyServerAddress(val protocol: ProxyProtocol?, val host: String, val port: Int?) {
    companion object {
        fun parse(value: String): ProxyServerAddress? {
            val address = value.trim()
            val scheme = ProxyProtocol.fromAddress(address)
            if ("://" in address && scheme == null) return null
            val authority = address.substringAfter("://").removeSuffix("/")
            if (authority.isEmpty() || authority.any { it.isWhitespace() || it.isISOControl() || it in "/?#@\\" }) return null

            // Try a bare host first, so unbracketed IPv6 addresses are not mistaken for a port.
            canonicalHost(authority)?.let { return ProxyServerAddress(scheme, it, null) }
            val host = authority.substringBeforeLast(':', "")
            if (':' in host && !(host.startsWith('[') && host.endsWith(']'))) return null
            val port = authority.substringAfterLast(':', "").toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
            return ProxyServerAddress(scheme, canonicalHost(host) ?: return null, port)
        }

        private fun canonicalHost(host: String): String? = try {
            HttpUrl.Builder().scheme("http").host(host).build().host
        } catch (_: IllegalArgumentException) { null }
    }
}

@ConsistentCopyVisibility
internal data class ProxyEndpoint private constructor(
    val protocol: ProxyProtocol,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
) {
    val hasCredentials get() = username.isNotEmpty()

    fun basicAuthorization(): String? = if (hasCredentials) {
        "Basic " + Base64.encodeToString("$username:$password".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    } else null

    override fun toString() = "$protocol ${proxyAuthority(host, port)}"

    companion object {
        fun fromLink(value: String): ProxyEndpoint? {
            val link = value.trim()
            val protocol = ProxyProtocol.fromAddress(link) ?: return null
            val authority = link.substringAfter("://").removeSuffix("/")
            if (authority.any { it.isWhitespace() || it.isISOControl() || it in "/?#\\" }) return null
            val address = ProxyServerAddress.parse(authority.substringAfterLast('@')) ?: return null
            val port = address.port ?: return null
            val url = "http://$authority".toHttpUrlOrNull() ?: return null
            return try {
                create(protocol, address.host, port, url.username, url.password)
            } catch (_: IllegalArgumentException) { null }
        }

        fun create(protocol: ProxyProtocol, host: String, port: Int, username: String, password: String): ProxyEndpoint {
            val address = HttpUrl.Builder().scheme("http").host(host.trim()).port(port).build()
            require(username.isNotEmpty() || password.isEmpty())
            require((username + password).none(Char::isISOControl))
            if (protocol != ProxyProtocol.SOCKS5) require(':' !in username)
            if (protocol == ProxyProtocol.SOCKS5 && username.isNotEmpty()) {
                require(username.toByteArray(Charsets.UTF_8).size in 1..255)
                require(password.toByteArray(Charsets.UTF_8).size in 1..255)
            }
            return ProxyEndpoint(protocol, address.host, port, username, password)
        }
    }
}

internal fun proxyAuthority(host: String, port: Int) = if (':' in host) "[$host]:$port" else "$host:$port"
