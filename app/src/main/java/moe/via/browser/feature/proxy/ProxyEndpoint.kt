package moe.via.browser.feature.proxy

import android.util.Base64
import okhttp3.HttpUrl

internal enum class ProxyProtocol { HTTP, SOCKS5 }

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
        fun create(protocol: ProxyProtocol, host: String, port: Int, username: String, password: String): ProxyEndpoint {
            val address = HttpUrl.Builder().scheme("http").host(host.trim()).port(port).build()
            require(username.isNotEmpty() || password.isEmpty())
            require((username + password).none(Char::isISOControl))
            if (protocol == ProxyProtocol.HTTP) require(':' !in username)
            if (protocol == ProxyProtocol.SOCKS5 && username.isNotEmpty()) {
                require(username.toByteArray(Charsets.UTF_8).size in 1..255)
                require(password.toByteArray(Charsets.UTF_8).size in 1..255)
            }
            return ProxyEndpoint(protocol, address.host, port, username, password)
        }
    }
}

internal fun proxyAuthority(host: String, port: Int) = if (':' in host) "[$host]:$port" else "$host:$port"
