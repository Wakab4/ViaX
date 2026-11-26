package moe.via.browser.feature.proxy

import android.os.Build
import android.os.SystemClock
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.dns.DohResolver
import moe.via.browser.feature.dns.DohServer
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.BufferedInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.security.SecureRandom
import java.util.concurrent.Semaphore
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

internal sealed interface RelayRoute {
    data class Upstream(val endpoint: ProxyEndpoint) : RelayRoute
    data class SecureDns(val server: DohServer) : RelayRoute
}

internal class LocalProxyRelay(val route: RelayRoute) : AutoCloseable {
    private val upstream = (route as? RelayRoute.Upstream)?.endpoint
    private val resolver = (route as? RelayRoute.SecureDns)?.let { DohResolver(it.server) }
    private val clients = Semaphore(128)
    private val server = listen()
    val address: InetSocketAddress get() = server.localSocketAddress as InetSocketAddress
    val rule get() = "http://${proxyAuthority(address.address.hostAddress!!, address.port)}"

    init { BrowserExecutors.io.execute(::accept) }

    private fun accept() {
        while (!server.isClosed) {
            val client = try { server.accept() } catch (_: IOException) { return }
            if (!clients.tryAcquire()) {
                runCatching { client.close() }
                continue
            }
            BrowserExecutors.io.execute {
                try { client.use(::forward) } catch (_: IOException) {
                    // The client's request is complete or the connection has been terminated.
                } finally { clients.release() }
            }
        }
    }

    private fun forward(client: Socket) {
        client.soTimeout = CONNECT_TIMEOUT
        val clientInput = BufferedInputStream(client.getInputStream())
        val clientOutput = client.getOutputStream()
        val request = ProxyHttpHead.read(clientInput) ?: return
        val target = Target.parse(request.line) ?: run {
            error(client, "400 Bad Request")
            return
        }
        var startedResponse = false
        try {
            val httpProxy = upstream != null && upstream.protocol != ProxyProtocol.SOCKS5
            val remoteSocket = if (upstream == null) connectDirect(target) else connectUpstream(upstream)
            remoteSocket.use { remote ->
                val remoteInput = BufferedInputStream(remote.getInputStream())
                val remoteOutput = remote.getOutputStream()
                if (upstream?.protocol == ProxyProtocol.SOCKS5) {
                    Socks5Tunnel.connect(remoteInput, remoteOutput, upstream, target.host, target.port)
                }
                val authorization = if (httpProxy) upstream?.basicAuthorization() else null

                if (target.connect) {
                    if (httpProxy) {
                        val headers = listOfNotNull(ProxyHeader("Host", target.authority),
                            authorization?.let { ProxyHeader("Proxy-Authorization", it) })
                        writeProxyHead(remoteOutput, "CONNECT ${target.authority} HTTP/1.1", headers)
                        if (response(remoteInput).second !in 200..299) throw IOException("Proxy CONNECT rejected")
                    }
                    writeProxyHead(clientOutput, "HTTP/1.1 200 Connection Established")
                    startedResponse = true
                    tunnel(client, clientInput, remote, remoteInput)
                    return
                }

                val upload = request.hasRequestBody
                val requestTarget = if (httpProxy) target.absoluteForm else target.originForm
                request.forward(remoteOutput, "${target.method} $requestTarget HTTP/1.1", target.hostHeader,
                    authorization, keepUpgrade = request.upgrading)
                client.soTimeout = BODY_TIMEOUT
                remote.soTimeout = BODY_TIMEOUT
                if (upload) BrowserExecutors.io.execute {
                    try { request.copyBody(clientInput, remoteOutput, throughEof = false) }
                    catch (_: IOException) { runCatching { finishRemoteOutput(remote) } }
                }

                val (head, status) = response(remoteInput) { interim -> interim.forward(clientOutput, interim = true) }
                if (httpProxy && status == 407) throw IOException("HTTP proxy authentication rejected")
                val switching = status == 101 && request.upgrading && head.upgrading && !upload
                startedResponse = true
                head.forward(clientOutput, keepUpgrade = switching)
                if (switching) tunnel(client, clientInput, remote, remoteInput)
                else if (target.method != "HEAD" && status >= 200 && status != 204 && status != 304) {
                    head.copyBody(remoteInput, clientOutput, throughEof = true)
                }
            }
        } catch (failure: IOException) {
            if (!startedResponse) error(client, "502 Bad Gateway") else throw failure
        }
    }

    private fun connectDirect(target: Target): Socket {
        val addresses = requireNotNull(resolver).lookup(target.host)
        val deadline = SystemClock.elapsedRealtime() + CONNECT_TIMEOUT
        var failure: IOException? = null
        for (address in addresses) {
            val remaining = deadline - SystemClock.elapsedRealtime()
            if (remaining <= 0) break
            val timeout = if (addresses.size == 1) remaining else minOf(remaining, 5_000L)
            val socket = Socket(Proxy.NO_PROXY)
            try {
                socket.connect(InetSocketAddress(address, target.port), timeout.toInt())
                socket.soTimeout = CONNECT_TIMEOUT
                return socket
            } catch (error: IOException) {
                runCatching { socket.close() }
                failure = error
            }
        }
        throw IOException("Unable to connect to ${target.host}", failure)
    }

    private fun connectUpstream(upstream: ProxyEndpoint): Socket {
        val socket = Socket(Proxy.NO_PROXY)
        try {
            socket.connect(InetSocketAddress(upstream.host, upstream.port), CONNECT_TIMEOUT)
            socket.soTimeout = CONNECT_TIMEOUT
            if (upstream.protocol != ProxyProtocol.HTTPS) return socket

            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            return (factory.createSocket(socket, upstream.host, upstream.port, true) as SSLSocket).apply {
                soTimeout = CONNECT_TIMEOUT
                sslParameters = sslParameters.apply {
                    endpointIdentificationAlgorithm = "HTTPS"
                    if (Build.VERSION.SDK_INT >= 29) applicationProtocols = arrayOf("http/1.1")
                }
                startHandshake()
            }
        } catch (failure: Exception) {
            runCatching { socket.close() }
            if (failure is IOException) throw failure
            throw IOException("Unable to open proxy connection", failure)
        }
    }

    private fun response(input: InputStream, interim: (ProxyHttpHead) -> Unit = {}): Pair<ProxyHttpHead, Int> {
        while (true) {
            val head = ProxyHttpHead.read(input) ?: throw EOFException()
            val parts = head.line.split(' ', limit = 3)
            val status = parts.getOrNull(1)?.toIntOrNull()
            if (!parts.first().startsWith("HTTP/1.") || status == null || status !in 100..599) throw IOException("Invalid proxy response")
            if (status == 101 || status >= 200) return head to status
            interim(head)
        }
    }

    private fun tunnel(client: Socket, clientInput: InputStream, remote: Socket, remoteInput: InputStream) {
        client.soTimeout = 0
        remote.soTimeout = 0
        val uplink = BrowserExecutors.io.submit {
            try {
                clientInput.copyTo(remote.getOutputStream(), 16_384)
                finishRemoteOutput(remote)
            } catch (_: IOException) {
                runCatching { remote.close() }
                runCatching { client.close() }
            }
        }
        try {
            remoteInput.copyTo(client.getOutputStream(), 16_384)
            client.shutdownOutput()
            uplink.get()
        } catch (_: IOException) {
            // Closing either socket releases the peer copy.
        }
    }

    private fun finishRemoteOutput(socket: Socket) {
        if (socket is SSLSocket) socket.close() else socket.shutdownOutput()
    }

    // Stop accepting new connections. Existing transfers can finish on their original route.
    override fun close() { runCatching { server.close() } }

    private data class Target(val method: String, val host: String, val port: Int,
        val absoluteForm: String, val originForm: String) {
        val connect get() = method == "CONNECT"
        val authority get() = proxyAuthority(host, port)
        val hostHeader get() = if (port != 80) authority else if (':' in host) "[$host]" else host

        companion object {
            fun parse(line: String): Target? {
                val parts = line.split(' ')
                if (parts.size != 3 || parts[2] !in setOf("HTTP/1.1", "HTTP/1.0")) return null
                val (method, destination) = parts
                if (method.isEmpty() || !method.all(::httpTokenCharacter)) return null
                if (method == "CONNECT") {
                    val uri = runCatching { URI("http://$destination") }.getOrNull() ?: return null
                    if (uri.rawAuthority != destination || uri.rawUserInfo != null || uri.port !in 1..65535) return null
                    val host = "http://$destination".toHttpUrlOrNull()?.host ?: return null
                    return Target(method, host, uri.port, "", "")
                }
                val requestUrl = if (destination.startsWith("ws://", true)) "http://" + destination.substring(5) else destination
                val url = requestUrl.toHttpUrlOrNull() ?: return null
                if (url.isHttps || url.username.isNotEmpty() || url.password.isNotEmpty() || url.fragment != null) return null
                val origin = url.encodedPath + (url.encodedQuery?.let { "?$it" } ?: "")
                return Target(method, url.host, url.port, url.toString(), origin)
            }
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT = 20_000
        private const val BODY_TIMEOUT = 120_000

        private fun listen(): ServerSocket {
            // Use an unadvertised loopback address and an OS-assigned port for this process.
            val bytes = ByteArray(4).also(SecureRandom()::nextBytes)
            bytes[0] = 127
            if (bytes[3] == 0.toByte() || bytes[3] == (-1).toByte()) bytes[3] = 1
            val socket = ServerSocket()
            try {
                socket.bind(InetSocketAddress(InetAddress.getByAddress(bytes), 0))
                return socket
            } catch (error: IOException) {
                socket.close()
                throw error
            }
        }

        private fun error(client: Socket, status: String) {
            writeProxyHead(client.getOutputStream(), "HTTP/1.1 $status",
                listOf(ProxyHeader("Connection", "close"), ProxyHeader("Content-Length", "0")))
        }
    }
}
