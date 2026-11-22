package moe.via.browser.feature.download.data

import android.content.ContentValues
import android.content.Context
import android.util.Base64
import moe.via.browser.storage.database.BrowserDatabase
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.UUID
import moe.via.browser.feature.download.transfer.DataUriInputStream
import moe.via.browser.feature.download.transfer.DownloadControl

data class DownloadPayload(val token: String, val mime: String, val size: Long, val file: File, val format: String = "BINARY")

/** Inline sources remain independent of their public destination so paused/re-downloaded tasks work. */
class DownloadPayloads(context: Context, private val store: BrowserDatabase = BrowserDatabase(context),
    private val directory: File = File(context.filesDir, "download-payloads")) : AutoCloseable {
    private val db get() = store.writableDatabase
    private var recovered = false
    @Synchronized private fun recover() {
        if (recovered) return
        val expired = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val tokens = db.rawQuery("SELECT token FROM download_payloads WHERE (state='PREPARING' OR created<?) AND NOT EXISTS(SELECT 1 FROM download_tasks WHERE payload=token)", arrayOf(expired.toString())).use {
            buildList { while (it.moveToNext()) add(it.getString(0)) }
        }
        tokens.forEach { removeUnused(it) }
        recovered = true
    }
    @Synchronized fun begin(mime: String, format: String = "BINARY"): Writer {
        recover(); check(directory.mkdirs() || directory.isDirectory)
        val token = UUID.randomUUID().toString()
        db.insertOrThrow("download_payloads", null, ContentValues().apply {
            put("token", token); put("mime", mime); put("state", "PREPARING"); put("created", System.currentTimeMillis())
            put("format", format)
        })
        var stream: FileOutputStream? = null
        try { stream = FileOutputStream(file(token)); return Writer(token, mime, stream, format) }
        catch (error: Exception) { runCatching { stream?.close() }; db.delete("download_payloads", "token=?", arrayOf(token)); file(token).delete(); throw error }
    }
    @Synchronized fun get(token: String): DownloadPayload? {
        recover()
        return db.query("download_payloads", arrayOf("mime", "size", "format"), "token=? AND state='READY'", arrayOf(token), null, null, null).use {
            if (it.moveToFirst() && file(token).isFile) DownloadPayload(token, it.getString(0), it.getLong(1), file(token), it.getString(2)) else null
        }
    }
    @Synchronized fun removeUnused(token: String) {
        val referenced = db.rawQuery("SELECT 1 FROM download_tasks WHERE payload=? LIMIT 1", arrayOf(token)).use { it.moveToFirst() }
        if (!referenced) { db.delete("download_payloads", "token=?", arrayOf(token)); file(token).delete() }
    }
    fun dataUrl(token: String): String {
        val payload = get(token) ?: throw FileNotFoundException("页面下载内容已失效")
        if (payload.format == "DATA_URI_GZIP") return GZIPInputStream(payload.file.inputStream()).reader(Charsets.UTF_8).use { it.readText() }
        return "data:${payload.mime};base64," + Base64.encodeToString(payload.file.readBytes(), Base64.NO_WRAP)
    }
    fun input(token: String): InputStream {
        val payload = get(token) ?: throw FileNotFoundException("页面下载内容已失效")
        return if (payload.format == "DATA_URI_GZIP") DataUriInputStream.openEncoded(GZIPInputStream(payload.file.inputStream())) else payload.file.inputStream()
    }
    fun fromDataUri(url: String, control: DownloadControl? = null): DownloadPayload {
        val header = DataUriInputStream.header(url)
        val writer = begin(header.mime, "DATA_URI_GZIP")
        try {
            var count = 0L
            DataUriInputStream.open(url).use { input ->
                val buffer = ByteArray(32 * 1024)
                while (true) { control?.check(); val read = input.read(buffer); if (read < 0) break; count += read }
            }
            writer.writeText(url)
            control?.check()
            return writer.finish(count)
        } catch (error: Exception) { writer.close(); removeUnused(writer.token); throw error }
    }
    private fun file(token: String): File {
        require(TOKEN.matches(token)) { "无效的下载内容标识" }
        return File(directory, "$token.bin")
    }
    inner class Writer(val token: String, private val mime: String, private val raw: FileOutputStream, private val format: String) : AutoCloseable {
        private val output: OutputStream = if (format == "DATA_URI_GZIP") GZIPOutputStream(raw) else raw
        var size = 0L; private set
        private var closed = false
        @Synchronized fun write(bytes: ByteArray, count: Int = bytes.size) { check(!closed); output.write(bytes, 0, count); size += count }
        @Synchronized fun writeText(text: String) { check(!closed); OutputStreamWriter(output, Charsets.UTF_8).apply { write(text); flush() } }
        @Synchronized fun finish(decodedSize: Long = size): DownloadPayload {
            check(!closed)
            if (output is GZIPOutputStream) output.finish()
            output.flush(); raw.channel.force(false); output.close(); closed = true; size = decodedSize
            synchronized(this@DownloadPayloads) {
                check(db.update("download_payloads", ContentValues().apply { put("size", size); put("state", "READY") }, "token=?", arrayOf(token)) == 1)
            }
            return DownloadPayload(token, mime, size, file(token), format)
        }
        @Synchronized override fun close() { if (!closed) { closed = true; output.close() } }
    }
    override fun close() { store.close() }
    companion object {
        private val TOKEN = Regex("[0-9a-fA-F-]{36}")
        @Volatile private var current: DownloadPayloads? = null
        fun get(context: Context): DownloadPayloads = current ?: synchronized(this) { current ?: DownloadPayloads(context.applicationContext).also { current = it } }
    }
}
