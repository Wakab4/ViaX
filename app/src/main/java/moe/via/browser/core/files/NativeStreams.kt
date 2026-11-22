package moe.via.browser.core.files

import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException

/** g6.j.a closes without replacing the caller's result with an IOException. */
internal object NativeStreams {
    fun close(stream: Closeable?) {
        try { stream?.close() } catch (error: IOException) { error.printStackTrace() }
    }

    fun appendLines(reader: BufferedReader, text: StringBuilder) {
        while (true) {
            val line = reader.readLine() ?: break
            text.append(line).append('\n')
        }
    }
}
