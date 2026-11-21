package moe.pure.browser.core.config

import org.json.JSONObject

/** r9.d/i: first case-insensitive name wins; only enabled toggles supply a value. */
internal class RemoteFeatureDocument private constructor(private val toggles: List<Toggle>) {
    private data class Toggle(val name: String, val enabled: Boolean, val payload: String?, val variant: String?)
    val isEmpty: Boolean get() = toggles.isEmpty()

    fun getString(name: String, default: String?): String? {
        val toggle = toggles.firstOrNull { it.name.equals(name, true) } ?: return default
        if (!toggle.enabled) return default
        return toggle.payload?.takeIf(String::isNotEmpty) ?: toggle.variant?.takeIf(String::isNotEmpty) ?: default
    }

    fun getInt(name: String, default: Int): Int = getString(name, null)?.takeIf(String::isNotEmpty)?.let {
        runCatching { Integer.decode(it) }.getOrNull()
    } ?: default

    companion object {
        val EMPTY = RemoteFeatureDocument(emptyList())

        fun parse(raw: String?): RemoteFeatureDocument {
            if (raw.isNullOrEmpty()) return EMPTY
            val array = runCatching { JSONObject(raw).optJSONArray("toggles") }.getOrNull() ?: return EMPTY
            val result = buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val name = item.optString("name")
                    if (name.isEmpty()) continue
                    val enabled = item.optBoolean("enabled", false)
                    val variant = item.optJSONObject("variant")
                    val payload = variant?.optJSONObject("payload")?.takeIf { it.has("value") }?.optString("value")
                    val variantName = variant?.let { variantObject ->
                        val value = variantObject.optString("name")
                        value.takeUnless { value == "disabled" && variantObject.optBoolean("enabled", !enabled) == !enabled }
                    }
                    add(Toggle(name, enabled, payload, variantName))
                }
            }
            return RemoteFeatureDocument(result)
        }
    }
}
