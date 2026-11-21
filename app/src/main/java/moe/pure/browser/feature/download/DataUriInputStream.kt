package moe.pure.browser.feature.download

import android.util.Base64
import android.util.Base64InputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

/** Decodes RFC2397 without copying a whole URL into another byte array. */
object DataUriInputStream {
    data class Header(val mime: String, val base64: Boolean, val body: Int)
    fun header(url: String): Header {
        require(url.startsWith("data:", true))
        val comma = url.indexOf(','); require(comma >= 5) { "无效的 data 链接" }
        val parts = url.substring(5, comma).split(';')
        return Header(parts.first().ifBlank { "text/plain" }, parts.drop(1).any { it.equals("base64", true) }, comma + 1)
    }
    fun open(url: String): InputStream {
        val header = header(url)
        val stream = PercentInputStream(Utf8Input(url, header.body))
        return if (header.base64) Base64InputStream(stream, Base64.DEFAULT) else stream
    }
    fun openEncoded(source: InputStream): InputStream {
        val header = StringBuilder()
        while (true) {
            val byte = source.read(); require(byte >= 0) { "无效的 data 链接" }
            header.append(byte.toChar())
            if (byte == ','.code) break
            require(header.length <= 65536) { "data 链接头过长" }
        }
        val parsed = header(header.toString())
        val stream = PercentInputStream(source)
        return if (parsed.base64) Base64InputStream(stream, Base64.DEFAULT) else stream
    }
    private class PercentInputStream(private val source: InputStream) : InputStream() {
        override fun read(): Int {
            val first = source.read()
            if (first != '%'.code) return first
            val a = source.read(); val b = source.read()
            require(a >= 0 && b >= 0) { "无效的百分号编码" }
            val high = Character.digit(a.toChar(), 16); val low = Character.digit(b.toChar(), 16)
            require(high >= 0 && low >= 0) { "无效的百分号编码" }
            return (high shl 4) or low
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            var count = 0
            while (count < length) { val value = read(); if (value < 0) break; buffer[offset + count++] = value.toByte() }
            return if (count == 0) -1 else count
        }
        override fun close() = source.close()
    }
    private class Utf8Input(text: String, start: Int) : InputStream() {
        private val chars = CharBuffer.wrap(text, start, text.length)
        private val encoder = Charsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
        private val bytes = ByteBuffer.allocate(4096).apply { limit(0) }
        private var ended = false
        private fun fill(): Boolean {
            if (bytes.hasRemaining()) return true
            if (ended) return false
            bytes.clear()
            val result = encoder.encode(chars, bytes, true)
            if (result.isUnderflow) { encoder.flush(bytes); ended = true }
            bytes.flip(); return bytes.hasRemaining()
        }
        override fun read(): Int = if (fill()) bytes.get().toInt() and 255 else -1
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (!fill()) return -1
            val count = minOf(length, bytes.remaining()); bytes.get(buffer, offset, count); return count
        }
    }
}
