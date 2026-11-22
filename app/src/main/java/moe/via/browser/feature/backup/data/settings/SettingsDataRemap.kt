package moe.via.browser.feature.backup.data.settings

import org.json.JSONObject

/** m9.j preserves negative built-ins, drops unmapped positive IDs and keeps the first mapped key. */
internal object SettingsDataRemap {
    private fun id(value: Int, ids: Map<Int, Int>): Int? = when {
        value < 0 -> value
        value > 0 -> ids[value]
        else -> null
    }

    fun list(raw: String?, ids: Map<Int, Int>): String {
        if (raw.isNullOrEmpty()) return ""
        val mapped = LinkedHashSet<Int>()
        for (part in raw.split(',')) {
            try { id(Integer.parseInt(part.trim { it <= ' ' }), ids)?.let(mapped::add) }
            catch (_: NumberFormatException) { }
        }
        return mapped.joinToString(",")
    }

    fun shortcuts(raw: String?, ids: Map<Int, Int>): String {
        if (raw.isNullOrEmpty()) return ""
        return try {
            val source = JSONObject(raw)
            val result = JSONObject()
            for (key in source.keys()) {
                val mapped = id(Integer.parseInt(key), ids)
                if (mapped != null && !result.has(mapped.toString())) result.put(mapped.toString(), source.optString(key, ""))
            }
            result.toString()
        } catch (_: Exception) { raw }
    }
}
