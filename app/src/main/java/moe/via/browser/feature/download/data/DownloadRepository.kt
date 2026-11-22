package moe.via.browser.feature.download.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import moe.via.browser.storage.database.BrowserDatabase
import org.json.JSONObject
import moe.via.browser.feature.download.model.DownloadMetadata
import moe.via.browser.feature.download.model.DownloadPart
import moe.via.browser.feature.download.model.DownloadState
import moe.via.browser.feature.download.model.DownloadTask

class DownloadRepository(context: Context, private val store: BrowserDatabase = BrowserDatabase(context)) : AutoCloseable {
    private val db get() = store.writableDatabase
    @Synchronized fun add(task: DownloadTask): Long = db.insertOrThrow("download_tasks", null, ContentValues().apply {
        put("url", task.url); put("name", task.name); put("target", task.target); put("mime", task.mime)
        put("headers", JSONObject(task.headers).toString()); put("state", task.state.name)
        put("total", task.total); put("received", task.received); put("connections", task.connections.coerceIn(1, 8))
        put("created", task.created); put("updated", task.updated)
        put("payload", task.payload)
        put("transfer_flags", task.nativeFlags)
    })
    @Synchronized fun get(id: Long): DownloadTask? = db.query("download_tasks", null, "_id=?", arrayOf(id.toString()), null, null, null).use {
        if (it.moveToFirst()) task(it) else null
    }
    @Synchronized fun all(query: String = ""): List<DownloadTask> {
        val where = if (query.isBlank()) null else "(name LIKE ? ESCAPE '\\' OR url LIKE ? ESCAPE '\\')"
        val value = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
        return db.query("download_tasks", null, where, if (where == null) null else arrayOf(value, value), null, null, "created DESC,_id DESC").use { cursor ->
            buildList { while (cursor.moveToNext()) add(task(cursor)) }
        }
    }
    @Synchronized fun parts(id: Long): List<DownloadPart> = db.query("download_parts", null, "task_id=?", arrayOf(id.toString()), null, null, "part_index").use { cursor ->
        buildList { while (cursor.moveToNext()) add(DownloadPart(cursor.int("part_index"), cursor.long("start_byte"), cursor.long("end_byte"), cursor.long("received"))) }
    }
    @Synchronized fun prepare(id: Long, metadata: DownloadMetadata, parts: List<DownloadPart>, connections: Int? = null) = transaction {
        db.delete("download_parts", "task_id=?", arrayOf(id.toString()))
        parts.forEach { part -> db.insertOrThrow("download_parts", null, ContentValues().apply {
            put("task_id", id); put("part_index", part.index); put("start_byte", part.start); put("end_byte", part.end); put("received", part.received)
        }) }
        db.update("download_tasks", ContentValues().apply {
            put("total", metadata.total); put("received", parts.sumOf { it.received }); put("etag", metadata.etag)
            put("last_modified", metadata.lastModified); metadata.mime?.let { put("mime", it) }; put("updated", System.currentTimeMillis())
            connections?.let { put("connections", it.coerceIn(1, 8)) }
        }, "_id=?", arrayOf(id.toString()))
        if (metadata.ready) db.execSQL("UPDATE download_tasks SET transfer_flags=(transfer_flags & -4) | ? WHERE _id=?",
            arrayOf(if (metadata.ranges) 3 else 2, id))
    }
    @Synchronized fun resetTransfer(id: Long) = transaction {
        val headers = get(id)?.headers?.toMutableMap()?.apply { remove("ETag") }
        db.delete("download_parts", "task_id=?", arrayOf(id.toString()))
        db.update("download_tasks", ContentValues().apply {
            put("connections", 8); putNull("etag"); putNull("last_modified")
            headers?.let { put("headers", JSONObject(it).toString()) }
        }, "_id=?", arrayOf(id.toString()))
        db.execSQL("UPDATE download_tasks SET transfer_flags=(transfer_flags & -3) WHERE _id=?", arrayOf(id))
    }
    @Synchronized fun requestUrl(id: Long, url: String) {
        db.update("download_tasks", ContentValues().apply { put("url", url) }, "_id=?", arrayOf(id.toString()))
    }
    @Synchronized fun acceptProbe(id: Long, url: String, headers: Map<String, String>, name: String, mime: String, metadata: DownloadMetadata) = transaction {
        db.update("download_tasks", ContentValues().apply {
            put("url", url); put("headers", JSONObject(headers).toString()); put("name", name); put("mime", mime)
            put("total", metadata.total); put("etag", metadata.etag)
        }, "_id=?", arrayOf(id.toString()))
        db.execSQL("UPDATE download_tasks SET transfer_flags=(transfer_flags & -4) | ? WHERE _id=?",
            arrayOf(if (metadata.ranges) 3 else 2, id))
    }
    @Synchronized fun clearParts(id: Long) { db.delete("download_parts", "task_id=?", arrayOf(id.toString())) }
    @Synchronized fun progress(id: Long, partIndex: Int, bytes: Long) = transaction {
        db.update("download_parts", ContentValues().apply { put("received", bytes) }, "task_id=? AND part_index=?", arrayOf(id.toString(), partIndex.toString()))
        db.execSQL("UPDATE download_tasks SET received=(SELECT COALESCE(SUM(received),0) FROM download_parts WHERE task_id=?),updated=? WHERE _id=?",
            arrayOf(id, System.currentTimeMillis(), id))
    }
    @Synchronized fun partLength(id: Long, partIndex: Int, start: Long, length: Long) {
        db.update("download_parts", ContentValues().apply { put("end_byte", start + length - 1) },
            "task_id=? AND part_index=?", arrayOf(id.toString(), partIndex.toString()))
    }
    @Synchronized fun state(id: Long, state: DownloadState, error: String? = null) {
        db.update("download_tasks", ContentValues().apply {
            put("state", state.name); put("error", error); put("updated", System.currentTimeMillis())
            if (state == DownloadState.COMPLETE) put("finished", System.currentTimeMillis())
        }, "_id=?", arrayOf(id.toString()))
    }
    @Synchronized fun rename(id: Long, name: String) { db.update("download_tasks", ContentValues().apply { put("name", name) }, "_id=?", arrayOf(id.toString())) }
    @Synchronized fun retarget(id: Long, name: String, target: String, mime: String? = null) {
        db.update("download_tasks", ContentValues().apply { put("name", name); put("target", target); mime?.let { put("mime", it) } }, "_id=?", arrayOf(id.toString()))
    }
    @Synchronized fun complete(id: Long) {
        val now = System.currentTimeMillis()
        db.execSQL("UPDATE download_tasks SET state=?,total=CASE WHEN total<0 THEN received ELSE total END,error=NULL,updated=?,finished=? WHERE _id=?",
            arrayOf<Any>(DownloadState.COMPLETE.name, now, now, id))
    }
    @Synchronized fun remove(ids: Collection<Long>) = transaction { ids.forEach { db.delete("download_tasks", "_id=?", arrayOf(it.toString())) } }
    @Synchronized fun recoverInterrupted() { db.execSQL("UPDATE download_tasks SET state=? WHERE state=?", arrayOf(DownloadState.PAUSED.name, DownloadState.RUNNING.name)) }
    private fun task(cursor: Cursor): DownloadTask {
        val json = JSONObject(cursor.string("headers"))
        return DownloadTask(cursor.long("_id"), cursor.string("url"), cursor.string("name"), cursor.string("target"), cursor.string("mime"),
            json.keys().asSequence().associateWith { json.getString(it) }, DownloadState.valueOf(cursor.string("state")),
            cursor.long("total"), cursor.long("received"), cursor.nullable("etag"), cursor.nullable("last_modified"),
            cursor.int("connections"), cursor.nullable("error"), cursor.long("created"), cursor.long("updated"), cursor.long("finished"), payload = cursor.nullable("payload"), nativeFlags = cursor.int("transfer_flags"))
    }
    private fun transaction(body: () -> Unit) { db.beginTransaction(); try { body(); db.setTransactionSuccessful() } finally { db.endTransaction() } }
    private fun Cursor.string(name: String) = getString(getColumnIndexOrThrow(name))
    private fun Cursor.nullable(name: String): String? = getString(getColumnIndexOrThrow(name))
    private fun Cursor.int(name: String) = getInt(getColumnIndexOrThrow(name))
    private fun Cursor.long(name: String) = getLong(getColumnIndexOrThrow(name))
    override fun close() { store.close() }
}
