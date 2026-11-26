package moe.via.browser.feature.proxy

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal data class ProxyHeader(val name: String, val value: String)

internal class ProxyHttpHead(val line: String, private val headers: List<ProxyHeader>) {
    private fun values(name: String) = headers.filter { it.name.equals(name, true) }.map { it.value }

    private val connectionOptions = values("Connection").flatMap { it.split(',') }.map { it.trim().lowercase() }
    private val transferEncoding = values("Transfer-Encoding").flatMap { it.split(',') }.map { it.trim() }
    private val length = values("Content-Length").flatMap { it.split(',') }.map {
        it.trim().toLongOrNull()?.takeIf { size -> size >= 0 } ?: throw IOException("Invalid Content-Length")
    }.distinct()

    init {
        if (length.size > 1 || transferEncoding.isNotEmpty() && length.isNotEmpty() ||
            connectionOptions.any { it in FRAMING_HEADERS }) throw IOException("Conflicting HTTP framing")
    }

    val upgrading get() = "upgrade" in connectionOptions && values("Upgrade").isNotEmpty()
    val hasRequestBody: Boolean get() {
        if (transferEncoding.isNotEmpty() && !chunked) throw IOException("Unframed proxy request")
        return chunked || (length.singleOrNull() ?: 0) > 0
    }
    private val chunked get() = transferEncoding.lastOrNull().equals("chunked", true)

    fun forward(output: OutputStream, startLine: String = line, host: String? = null,
        authorization: String? = null, keepUpgrade: Boolean = false, interim: Boolean = false) {
        val fields = headers.filter { header ->
            val name = header.name.lowercase()
            when {
                name == "host" && host != null -> false
                name == "upgrade" -> keepUpgrade
                name in HOP_HEADERS || name in connectionOptions -> false
                else -> true
            }
        }.toMutableList()
        if (host != null) fields.add(ProxyHeader("Host", host))
        if (authorization != null) fields.add(ProxyHeader("Proxy-Authorization", authorization))
        if (!interim) fields.add(ProxyHeader("Connection", if (keepUpgrade) "Upgrade" else "close"))
        writeProxyHead(output, startLine, fields)
    }

    fun copyBody(input: InputStream, output: OutputStream, throughEof: Boolean) {
        val buffer = ByteArray(16_384)
        when {
            chunked -> copyChunks(input, output, buffer)
            length.isNotEmpty() -> copyBytes(input, output, length.single(), buffer)
            throughEof -> input.copyTo(output, buffer.size)
        }
        output.flush()
    }

    companion object {
        private val FRAMING_HEADERS = setOf("content-length", "transfer-encoding")
        private val HOP_HEADERS = setOf("connection", "keep-alive", "proxy-authenticate", "proxy-authorization", "proxy-connection", "te")

        fun read(input: InputStream): ProxyHttpHead? {
            val line = readProxyLine(input, MAX_LINE) ?: return null
            if (line.isEmpty()) throw IOException("Missing HTTP start line")
            val fields = mutableListOf<ProxyHeader>()
            var remaining = MAX_HEADERS
            while (true) {
                val field = readProxyLine(input, minOf(MAX_LINE, remaining)) ?: throw EOFException()
                if (field.isEmpty()) return ProxyHttpHead(line, fields)
                remaining -= field.length + 2
                if (remaining < 0) throw IOException("HTTP headers too large")
                val separator = field.indexOf(':')
                if (separator <= 0 || !field.substring(0, separator).all(::httpTokenCharacter)) throw IOException("Invalid HTTP header")
                fields.add(ProxyHeader(field.substring(0, separator), field.substring(separator + 1).trim()))
            }
        }
    }
}

internal fun httpTokenCharacter(char: Char) = char in '0'..'9' || char in 'A'..'Z' || char in 'a'..'z' || char in "!#$%&'*+-.^_`|~"

internal fun writeProxyHead(output: OutputStream, line: String, headers: List<ProxyHeader> = emptyList()) {
    val text = buildString {
        append(line).append("\r\n")
        headers.forEach { append(it.name).append(": ").append(it.value).append("\r\n") }
        append("\r\n")
    }
    output.write(text.toByteArray(Charsets.ISO_8859_1))
    output.flush()
}

private const val MAX_LINE = 16_384
private const val MAX_HEADERS = 65_536

private fun readProxyLine(input: InputStream, limit: Int): String? {
    val bytes = ByteArrayOutputStream(128)
    while (true) {
        when (val byte = input.read()) {
            -1 -> if (bytes.size() == 0) return null else throw EOFException()
            13 -> {
                if (input.read() != 10) throw IOException("Invalid HTTP line ending")
                return bytes.toString(Charsets.ISO_8859_1.name())
            }
            10 -> throw IOException("Invalid HTTP line ending")
            else -> {
                if (byte < 32 && byte != 9 || byte == 127) throw IOException("Invalid control byte in HTTP headers")
                if (bytes.size() >= limit) throw IOException("HTTP line too large")
                bytes.write(byte)
            }
        }
    }
}

private fun copyChunks(input: InputStream, output: OutputStream, buffer: ByteArray) {
    fun transferLine(limit: Int): String {
        val line = readProxyLine(input, limit) ?: throw EOFException()
        output.write((line + "\r\n").toByteArray(Charsets.ISO_8859_1))
        return line
    }

    while (true) {
        val length = transferLine(MAX_LINE).substringBefore(';').trim().toLongOrNull(16)
            ?.takeIf { it >= 0 } ?: throw IOException("Invalid HTTP chunk")
        if (length == 0L) break
        copyBytes(input, output, length, buffer)
        if (transferLine(0).isNotEmpty()) throw IOException("Invalid HTTP chunk ending")
        output.flush()
    }
    var remaining = MAX_HEADERS
    while (true) {
        val trailer = transferLine(minOf(MAX_LINE, remaining))
        if (trailer.isEmpty()) return
        remaining -= trailer.length + 2
        if (remaining < 0) throw IOException("HTTP trailers too large")
    }
}

private fun copyBytes(input: InputStream, output: OutputStream, length: Long, buffer: ByteArray) {
    var remaining = length
    while (remaining > 0) {
        val count = input.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
        if (count < 0) throw EOFException()
        output.write(buffer, 0, count)
        remaining -= count
    }
}
