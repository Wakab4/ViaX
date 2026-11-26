package moe.via.browser.feature.proxy

import android.net.InetAddresses
import android.os.Build
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress

internal object Socks5Tunnel {
    fun connect(input: InputStream, output: OutputStream, proxy: ProxyEndpoint, host: String, port: Int) {
        val reader = DataInputStream(input)
        val writer = DataOutputStream(output)
        val method = if (proxy.hasCredentials) 2 else 0
        writer.write(byteArrayOf(5, 1, method.toByte()))
        writer.flush()
        if (reader.readUnsignedByte() != 5 || reader.readUnsignedByte() != method) throw IOException("SOCKS5 method rejected")

        if (proxy.hasCredentials) {
            val username = proxy.username.toByteArray(Charsets.UTF_8)
            val password = proxy.password.toByteArray(Charsets.UTF_8)
            try {
                writer.writeByte(1)
                writer.writeByte(username.size)
                writer.write(username)
                writer.writeByte(password.size)
                writer.write(password)
                writer.flush()
                if (reader.readUnsignedByte() != 1 || reader.readUnsignedByte() != 0) throw IOException("SOCKS5 authentication rejected")
            } finally {
                username.fill(0)
                password.fill(0)
            }
        }

        writer.write(byteArrayOf(5, 1, 0))
        val literal = numericAddress(host)
        if (literal == null) {
            val domain = host.toByteArray(Charsets.US_ASCII)
            if (domain.size !in 1..255) throw IOException("Invalid SOCKS5 destination")
            writer.writeByte(3)
            writer.writeByte(domain.size)
            writer.write(domain)
        } else {
            writer.writeByte(if (literal.size == 4) 1 else 4)
            writer.write(literal)
        }
        writer.writeShort(port)
        writer.flush()

        if (reader.readUnsignedByte() != 5 || reader.readUnsignedByte() != 0 || reader.readUnsignedByte() != 0) {
            throw IOException("SOCKS5 connection rejected")
        }
        val replySize = when (reader.readUnsignedByte()) {
            1 -> 4
            3 -> reader.readUnsignedByte()
            4 -> 16
            else -> throw IOException("Invalid SOCKS5 reply")
        }
        reader.readFully(ByteArray(replySize + 2))
    }

    // Pass hostnames to the upstream proxy. Only parse local IP literals here.
    private fun numericAddress(host: String): ByteArray? {
        if (Build.VERSION.SDK_INT >= 29) return if (InetAddresses.isNumericAddress(host)) InetAddresses.parseNumericAddress(host).address else null
        if (':' in host) return InetAddress.getByName(host).address
        val parts = host.split('.')
        if (parts.size != 4) return null
        return ByteArray(4) { index ->
            val octet = parts[index].toIntOrNull()?.takeIf { it in 0..255 } ?: return null
            octet.toByte()
        }
    }
}
