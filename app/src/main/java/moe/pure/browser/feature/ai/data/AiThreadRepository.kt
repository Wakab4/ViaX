package moe.pure.browser.feature.ai.data

import android.content.ContentValues
import android.database.Cursor
import moe.pure.browser.feature.ai.model.AiThread

class AiThreadRepository internal constructor(private val database: AiDatabase) {
    fun all(): List<AiThread> = database.list("threads", order = "updated_at DESC") { it.thread() }
    fun get(id: String?): AiThread? = database.find("threads", id) { it.thread() }
    fun insert(value: AiThread): Boolean = database.write { it.insert("threads", null, values(value)) > 0 }
    fun update(value: AiThread): Boolean = database.write { it.update("threads", values(value).apply { remove("id") }, "id=?", arrayOf(value.id)) > 0 }
    fun delete(id: String): Boolean = delete(listOf(id))
    fun delete(ids: Collection<String>): Boolean = database.transaction { db ->
        var removed = 0
        ids.forEach { id ->
            removed += db.delete("threads", "id=?", arrayOf(id))
            removed += db.delete("messages", "thread_id=?", arrayOf(id))
        }
        removed > 0
    }
    private fun values(value: AiThread) = ContentValues().apply {
        put("id", value.id); put("name", value.name); put("preview", value.preview); put("flags", value.flags)
        put("created_at", value.createdAt); put("updated_at", value.updatedAt)
    }
    private fun Cursor.thread() = AiThread(id = text("id"), name = nullableText("name"), preview = nullableText("preview"),
        flags = number("flags"), createdAt = time("created_at"), updatedAt = time("updated_at"))
}
