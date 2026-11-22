package moe.via.browser.feature.ai.data

import android.content.ContentValues
import android.database.Cursor
import moe.via.browser.feature.ai.model.AiBackend

class AiBackendRepository internal constructor(private val database: AiDatabase) {
    fun all(): List<AiBackend> = database.list("backends", order = "created_at ASC") { it.backend() }
    fun get(id: String?): AiBackend? = if (id.isNullOrEmpty()) null else database.find("backends", id) { it.backend() }
    fun insert(value: AiBackend): Boolean = database.write { it.insert("backends", null, values(value)) > 0 }
    fun update(value: AiBackend): Boolean = database.write { it.update("backends", values(value).apply { remove("id") }, "id=?", arrayOf(value.id)) > 0 }
    fun delete(id: String): Boolean = database.write { it.delete("backends", "id=?", arrayOf(id)) > 0 }
    private fun values(value: AiBackend) = ContentValues().apply {
        put("id", value.id); put("name", value.name); put("url", value.baseUrl); put("token", value.token)
        put("models", value.models.joinToString(",")); put("flags", value.flags)
        put("created_at", value.createdAt); put("updated_at", value.updatedAt)
    }
    private fun Cursor.backend() = AiBackend(id = text("id"), name = text("name"), baseUrl = text("url"), token = text("token"),
        models = nullableText("models").aiList(), flags = number("flags"), createdAt = time("created_at"), updatedAt = time("updated_at"))
}
