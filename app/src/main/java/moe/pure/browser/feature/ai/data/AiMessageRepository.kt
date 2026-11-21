package moe.pure.browser.feature.ai.data

import android.content.ContentValues
import android.database.Cursor
import moe.pure.browser.feature.ai.model.AiMessage

class AiMessageRepository internal constructor(private val database: AiDatabase) {
    fun forThread(threadId: String): List<AiMessage> = database.list("messages", "thread_id = '$threadId'", order = "updated_at ASC") { it.message() }
    fun get(id: String?): AiMessage? = database.find("messages", id) { it.message() }
    fun insert(value: AiMessage): Boolean = database.write { it.insert("messages", null, values(value)) > 0 }
    fun update(value: AiMessage): Boolean = database.write { it.update("messages", values(value).apply { remove("id") }, "id=?", arrayOf(value.id)) > 0 }
    fun delete(id: String): Boolean = database.write { it.delete("messages", "id=?", arrayOf(id)) > 0 }
    fun clear(threadId: String): Boolean = database.write { it.delete("messages", "thread_id = ? and role != ?", arrayOf(threadId, AiMessage.SYSTEM)) > 0 }
    private fun values(value: AiMessage) = ContentValues().apply {
        put("id", value.id); put("thread_id", value.threadId); put("role", value.role)
        put("content", value.content); put("reasoning", value.reasoning); put("flags", value.errorFlags); put("updated_at", value.updatedAt)
    }
    private fun Cursor.message() = AiMessage(id = text("id"), threadId = text("thread_id"), role = text("role"), content = text("content"),
        reasoning = nullableText("reasoning"), errorFlags = number("flags"), updatedAt = time("updated_at"))
}
