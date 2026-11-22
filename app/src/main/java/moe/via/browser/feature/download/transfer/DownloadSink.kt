package moe.via.browser.feature.download.transfer

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/** Transfer code uses seekable destinations, independent of MediaStore, SAF or local files. */
interface DownloadSink {
    fun size(): Long
    fun truncate(length: Long)
    fun prepareLength(length: Long) = truncate(length)
    fun open(offset: Long): SegmentOutput
}
class SegmentOutput(private val channel: FileChannel, private val owner: AutoCloseable) : AutoCloseable {
    fun write(bytes: ByteArray, count: Int) { val buffer = ByteBuffer.wrap(bytes, 0, count); while (buffer.hasRemaining()) channel.write(buffer) }
    fun flush() { channel.force(false) }
    override fun close() { owner.close() }
}
class FileDownloadSink(private val file: File) : DownloadSink {
    override fun size() = file.length()
    override fun truncate(length: Long) { file.parentFile?.mkdirs(); RandomAccessFile(file, "rw").use { it.setLength(length) } }
    override fun prepareLength(length: Long) {
        file.parentFile?.mkdirs()
        RandomAccessFile(file, "rw").use { output ->
            if (output.length() != length) try { output.setLength(length) }
            catch (error: IOException) { moe.via.browser.core.logging.NativeLog.warn("DownloadTransfer", "Could not resize destination", error) }
        }
    }
    override fun open(offset: Long): SegmentOutput {
        file.parentFile?.mkdirs()
        val output = RandomAccessFile(file, "rw")
        try { return SegmentOutput(output.channel.apply { position(offset) }, output) }
        catch (error: Exception) { output.close(); throw error }
    }
}
class ContentDownloadSink(private val context: Context, private val uri: Uri) : DownloadSink {
    override fun size(): Long = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0
    override fun truncate(length: Long) {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "rw") ?: throw IOException("Cannot open destination")
        descriptor.use { try { android.system.Os.ftruncate(it.fileDescriptor, length) }
            catch (error: android.system.ErrnoException) { throw IOException(error) } }
    }
    override fun prepareLength(length: Long) {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "rw") ?: throw IOException("Cannot open destination")
        descriptor.use { if (it.statSize != length) try { android.system.Os.ftruncate(it.fileDescriptor, length) }
            catch (error: android.system.ErrnoException) { moe.via.browser.core.logging.NativeLog.warn("DownloadTransfer", "Could not resize destination", error) } }
    }
    override fun open(offset: Long): SegmentOutput {
        val output = stream()
        try { return SegmentOutput(output.channel.apply { position(offset) }, output) }
        catch (error: Exception) { output.close(); throw error }
    }
    private fun stream(): FileOutputStream = ParcelFileDescriptor.AutoCloseOutputStream(
        context.contentResolver.openFileDescriptor(uri, "rw") ?: error("无法打开下载文件"))
}
