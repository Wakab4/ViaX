package moe.via.browser.feature.backup.data.site

import moe.via.browser.feature.backup.model.SettingsImportKind
import moe.via.browser.feature.site.data.NativeSiteConfig
import moe.via.browser.feature.site.data.NativeSiteConfigCodec
import org.json.JSONObject

/** Collects one Native import map before the DAO replaces any configurations. */
internal class SiteBackupRecords(private val userAgentsByContent: Map<String, Int>) {
    fun collect(records: List<JSONObject>, kind: SettingsImportKind): Map<String, NativeSiteConfig?> {
        val result = HashMap<String, NativeSiteConfig?>()
        for (record in records) {
            when (kind) {
                SettingsImportKind.CURRENT_LINES -> {
                    // m9.l.p consumes only the first key, even when that child is invalid.
                    val keys = record.keys()
                    if (keys.hasNext()) {
                        val key = keys.next()
                        val json = record.optJSONObject(key)
                        val config = json?.let(NativeSiteConfigCodec::decode)
                        if (key.isNotEmpty() && json != null && config != null)
                            result[key] = remapUserAgent(config, json)
                    }
                }
                SettingsImportKind.LEGACY_CONF -> for (key in record.keys()) {
                    // l9.j.b retains null parses: da.a.a then deletes the existing key.
                    result[key] = record.optJSONObject(key)?.let(NativeSiteConfigCodec::decode)
                }
            }
        }
        return result
    }

    private fun remapUserAgent(config: NativeSiteConfig, json: JSONObject): NativeSiteConfig {
        if (config.userAgentChoice <= 0) return config
        val fallback = json.optString("uafallback")
        val matched = userAgentsByContent[fallback]
        return when {
            matched != null -> config.copy(userAgentChoice = matched)
            fallback.isEmpty() -> config.copy(userAgentChoice = -1000)
            else -> config.copy(userAgentChoice = -999,
                customUserAgent = fallback.replace(Regex("[\\t\\n\\r]+"), " ").trim { it <= ' ' })
        }
    }

}
