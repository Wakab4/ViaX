package moe.via.browser.feature.ai.data

import android.content.ContentValues
import android.database.Cursor
import moe.via.browser.feature.ai.model.AiPrompt

class AiPromptRepository internal constructor(private val database: AiDatabase) {
    fun all(type: Int? = null): List<AiPrompt> = database.list("suggestions", if (type == null) null else "type = ?",
        type?.let { arrayOf(it.toString()) }, "created_at ASC", warn = true) { it.prompt() }
    fun get(id: String?): AiPrompt? = database.find("suggestions", id) { it.prompt() }
    fun insert(value: AiPrompt): Boolean = database.write { it.insert("suggestions", null, values(value)) > 0 }
    fun update(value: AiPrompt): Boolean = database.write { it.update("suggestions", values(value).apply { remove("id") }, "id=?", arrayOf(value.id)) > 0 }
    fun delete(id: String): Boolean = database.write { it.delete("suggestions", "id= ?", arrayOf(id)) > 0 }
    private fun values(value: AiPrompt) = ContentValues().apply {
        put("id", value.id); put("name", value.name); put("content", value.content)
        put("required", value.required.joinToString(",")); put("type", value.type); put("flags", value.flags)
        put("created_at", value.createdAt); put("updated_at", value.updatedAt)
    }
    private fun Cursor.prompt() = AiPrompt(id = text("id"), name = text("name"), content = text("content"), type = number("type"),
        required = nullableText("required").aiList(), flags = number("flags"), createdAt = time("created_at"), updatedAt = time("updated_at"))
}
