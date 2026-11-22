package moe.via.browser.feature.scripts.data

import moe.via.browser.feature.scripts.model.ScriptOverrides
import org.json.JSONArray
import org.json.JSONObject

object ScriptCodec {
    /** r5.e.a/l preserves raw scope strings and omits runAt when zero. */
    internal fun backupOverrides(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        return try {
            val input = JSONObject(raw)
            JSONObject().apply {
                val runAt = input.optInt("runAt", 0)
                if (runAt != 0) put("runAt", runAt)
                put("matches", input.optString("matches"))
                put("excludeMatches", input.optString("excludeMatches"))
                put("includes", input.optString("includes"))
                put("excludes", input.optString("excludes"))
            }.toString()
        } catch (_: Exception) { null }
    }

    fun list(values: List<String>) = JSONArray(values).toString()
    fun list(value: String?): List<String> = runCatching { JSONArray(value ?: "[]").let { a -> List(a.length()) { a.getString(it) } } }.getOrDefault(emptyList())
    fun map(values: Map<String, String>) = JSONObject(values).toString()
    fun map(value: String?): Map<String, String> = runCatching { JSONObject(value ?: "{}").let { a -> a.keys().asSequence().associateWith(a::getString) } }.getOrDefault(emptyMap())
    /** r5.e.l omits zero/empty fields and stores null when no override remains. */
    fun overrides(value: ScriptOverrides): String? {
        fun encoded(values: List<String>): String? = values.filter(String::isNotEmpty).takeIf { it.isNotEmpty() }?.let(::list)
        val matches = encoded(value.matches)
        val excludes = encoded(value.excludeMatches)
        val includes = encoded(value.includes)
        val excluded = encoded(value.excludes)
        if (value.runAt == 0 && matches == null && excludes == null && includes == null && excluded == null) return null
        return JSONObject().apply {
            if (value.runAt != 0) put("runAt", value.runAt)
            put("matches", matches); put("excludeMatches", excludes)
            put("includes", includes); put("excludes", excluded)
        }.toString()
    }
    fun overrides(value: String?): ScriptOverrides = runCatching { JSONObject(value ?: "{}").let { a -> ScriptOverrides(a.optInt("runAt"), list(a.optString("matches")), list(a.optString("includes")), list(a.optString("excludeMatches")), list(a.optString("excludes"))) } }.getOrDefault(ScriptOverrides())
}
