package moe.pure.browser.data.settings

import org.json.JSONObject

/** ja.c's typed record, with nullable text and one ID namespace for every settings-data kind. */
internal data class NativeSettingsRow(var id: Int = 0, val title: String? = null, val content: String? = null,
    val note: String? = null, val flag: Int = 0, val type: Int = 0,
    val updatedAt: Long = 0, val createdAt: Long = 0) {
    fun toJson(): JSONObject = JSONObject().put("id", id).putOpt("title", title).putOpt("content", content)
        .putOpt("note", note).put("flag", flag).put("type", type).put("lastUpdatedAt", updatedAt).put("createdAt", createdAt)

    companion object {
        fun fromJson(json: JSONObject?): NativeSettingsRow? {
            if (json == null || json.length() == 0) return null
            var row = NativeSettingsRow()
            try {
                row.id = json.optInt("id")
                row = row.copy(title = json.optString("title").takeIf(String::isNotEmpty))
                row = row.copy(content = json.optString("content").takeIf(String::isNotEmpty))
                row = row.copy(note = json.optString("note").takeIf(String::isNotEmpty))
                row = row.copy(flag = json.optInt("flag"), type = json.optInt("type"))
                row = row.copy(updatedAt = json.optLong("lastUpdatedAt"), createdAt = json.optLong("createdAt"))
            } catch (error: Exception) { error.printStackTrace() }
            return row
        }

        fun create(type: Int, title: String?, content: String?): NativeSettingsRow {
            val now = System.currentTimeMillis()
            return NativeSettingsRow(title = title?.takeIf(String::isNotEmpty), content = content?.takeIf(String::isNotEmpty),
                type = type, updatedAt = now, createdAt = now)
        }
    }
}
