package moe.pure.browser.feature.scripts.runtime.http

import java.io.InputStream
import java.io.InputStreamReader
import java.io.PushbackInputStream
import java.io.Reader

/** s5.f's four-byte BOM probe retains its original UTF-16-before-UTF-32LE branch order. */
internal class NativeGmTextReader(source: InputStream, charset: String?) : Reader() {
    private val reader: InputStreamReader
    init {
        val bytes = ByteArray(4)
        val input = PushbackInputStream(source, 4)
        val count = input.read(bytes, 0, 4)
        val first = bytes[0].toInt() and 255
        val second = bytes[1].toInt() and 255
        val third = bytes[2].toInt() and 255
        val fourth = bytes[3].toInt() and 255
        val (encoding, skip) = when {
            first == 239 && second == 187 && third == 191 -> "UTF-8" to 3
            first == 254 && second == 255 -> "UTF-16BE" to 2
            first == 255 && second == 254 -> "UTF-16LE" to 2
            first == 0 && second == 0 && third == 254 && fourth == 255 -> "UTF-32BE" to 4
            first == 255 && second == 254 && third == 0 && fourth == 0 -> "UTF-32LE" to 4
            else -> charset to 0
        }
        val remaining = count - skip
        if (remaining > 0) input.unread(bytes, count - remaining, remaining)
        else if (remaining < -1) input.unread(bytes, 0, 0)
        reader = if (encoding == null) InputStreamReader(input) else InputStreamReader(input, encoding)
    }
    override fun read(buffer: CharArray, offset: Int, length: Int): Int = reader.read(buffer, offset, length)
    override fun close() = reader.close()
}
