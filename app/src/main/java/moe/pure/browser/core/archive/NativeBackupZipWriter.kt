package moe.pure.browser.core.archive

import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipOutputStream

/** d4 owns the original zip -> shared buffered payload stream and sequential closing. */
internal class NativeBackupZipWriter(private val output: OutputStream) : Closeable {
    private var zip: ZipOutputStream? = null
    private var payload: BufferedOutputStream? = null

    fun start() {
        val stream = ZipOutputStream(output)
        zip = stream
        payload = BufferedOutputStream(stream)
    }

    fun append(file: File, alias: String?) {
        val stream = zip ?: throw IllegalStateException("zipStream is not initaliezed")
        NativeBackupZipEntries.append(file, alias, stream, payload)
    }

    override fun close() {
        payload?.close()
        zip?.close()
        output.close()
    }
}
