package moe.pure.browser.feature.sync.sync

import android.content.Context
import android.util.Base64
import moe.pure.browser.feature.adblock.AdblockEngine
import moe.pure.browser.feature.adblock.CustomBlockRules
import moe.pure.browser.feature.adblock.FilterSubscriptions
import moe.pure.browser.feature.backup.data.BackupDataCodec
import moe.pure.browser.feature.backup.model.SettingsImportKind
import moe.pure.browser.feature.backup.model.BackupBookmarkRow
import moe.pure.browser.feature.backup.model.ImportMode
import moe.pure.browser.feature.scripts.model.ScriptParser
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable

/** Legacy l9 cloud payloads, also usable by a user-selected legacy text backup. No network access. */
class ViaCloudDataCodec(context: Context) : Closeable {
    private val app = context.applicationContext
    private val codec = BackupDataCodec(app)

    /** Values have Native Base64 wrapping and are URL-encoded by the cloud form caller. */
    fun exportFields(): Map<String, String?> {
        val bookmarks = codec.exportBookmarkRows().joinToString("\n") { row ->
            JSONObject().put("title", row.title.orEmpty()).put("url", row.url.orEmpty()).put("folder", row.folder).put("order", row.order)
                .put("updated_at", row.updatedAtSeconds).put("created_at", row.createdAtSeconds).toString()
        }
        val settings = codec.exportSettingValues(compact = true)
        val rules = JSONObject()
        CustomBlockRules(app).text().takeIf { it.isNotEmpty() && it.length <= 30720 }?.let { rules.put("custom", it) }
        FilterSubscriptions(app).use { subscriptions ->
            val list = subscriptions.all()
            if (list.isNotEmpty()) rules.put("subscribed", list.joinToString("\n", postfix = "\n") { JSONObject().put("url", it.url).toString() })
        }
        return linkedMapOf("adrules" to if (rules.length() == 0) "" else encode(rules.toString()),
            "bookmark" to encode(bookmarks), "settings" to encode(settings.toString()), "other" to "")
    }

    fun importText(text: String): Int {
        val payload = JSONObject(text)
        var count = 0
        var hasSettings = false
        val settingsRows = StringBuilder()
        fun record(type: String, value: JSONObject) { settingsRows.append(JSONObject().put(type, value)).append('\n') }
        fun decoded(key: String): String? = payload.optString(key).takeIf { it.isNotEmpty() && it != "null" }?.let(::decode)
        decoded("bookmark")?.let { body ->
            val rows = mutableListOf<BackupBookmarkRow>()
            var fallback = System.currentTimeMillis() / 1000 - 30
            for (line in body.lineSequence()) {
                if (line.isEmpty()) break
                val item = runCatching { JSONObject(line) }.getOrNull() ?: continue
                val url = item.optString("url"); if (url.isEmpty()) continue
                val created = item.optLong("created_at").takeIf { it != 0L } ?: fallback
                val updated = item.optLong("updated_at").takeIf { it != 0L } ?: fallback
                fallback++
                rows += BackupBookmarkRow(item.optString("title"), url, item.optString("folder"), item.optInt("order"), created, updated)
            }
            count += codec.importBookmarkRows(rows, ImportMode.MERGE)
        }
        decoded("favorite")?.let { count += codec.importFavorites(it, ImportMode.MERGE) }
        decoded("settings")?.let {
            val item = JSONObject(it)
            record("settings", item)
            hasSettings = true
        }
        decoded("kvdata")?.let { body ->
            val entries = JSONArray(body)
            for (index in 0 until entries.length()) entries.optJSONObject(index)?.let { record("settingsData", it) }
        }
        decoded("conf")?.let { record("siteConf", JSONObject(it)) }
        decoded("other")?.let { body ->
            for (line in body.lineSequence()) {
                if (line.isEmpty()) break
                val item = runCatching { JSONObject(line) }.getOrNull() ?: continue
                if (item.has("codeV2")) record("script", JSONObject().put("scriptId", item.optString("scriptId"))
                    .put("url", item.optString("url")).put("enabled", item.optBoolean("enabled"))
                    .put("pureCode", item.optString("codeV2")))
                else if (item.has("code")) legacyScript(item)?.let { record("script", it) }
                else item.keys().forEach { key ->
                    legacyScript(JSONObject().put("url", key).put("code", item.optString(key)))?.let { record("script", it) }
                }
            }
        }
        decoded("adrules")?.let { body ->
            val rules = JSONObject(body)
            if (rules.has("custom") && !rules.isNull("custom") && CustomBlockRules(app).replace(rules.getString("custom"))) count++
            if (rules.has("subscribed")) FilterSubscriptions(app).use { subscriptions ->
                for (line in rules.optString("subscribed").lineSequence().filter(String::isNotBlank)) {
                    val item = runCatching { JSONObject(line) }.getOrNull() ?: continue
                    val url = item.optString("url").trim(); if (url.isEmpty()) continue
                    val old = subscriptions.all().firstOrNull { it.url == url }
                    val saved = old ?: subscriptions.add(url, item.optString("title").takeIf(String::isNotEmpty))
                    if (item.has("filePath") && item.has("enabled")) subscriptions.setEnabled(saved.id, item.optBoolean("enabled"))
                    count++
                }
                AdblockEngine.changed(app)
            }
        }
        if (settingsRows.isNotEmpty()) count += maxOf(if (hasSettings) 1 else 0,
            codec.importSettings(settingsRows.toString(), ImportMode.MERGE, SettingsImportKind.LEGACY_CONF))
        return count
    }
    override fun close() = codec.close()
    private fun legacyScript(item: JSONObject): JSONObject? {
        val patterns = item.optString("url").split(',').map(String::trim).filter(String::isNotEmpty)
        if (patterns.isEmpty() || item.optString("code").isEmpty()) return null
        val body = runCatching { decode(item.getString("code")) }.getOrNull() ?: return null
        val name = item.optString("name").ifEmpty { item.optString("info") }.ifEmpty { "Untitled Script - ${maxOf(0, item.optInt("oid")) + 10000}" }
        val script = buildString {
            append("// ==UserScript==\n// @name         ").append(name).append("\n// @namespace    https://viayoo.com/\n// @version      0.1\n")
            if (item.optInt("oid") > 0) append("// @homepageURL  https://app.viayoo.com/addons/${item.optInt("oid")}\n")
            item.optString("author").takeIf(String::isNotEmpty)?.let { append("// @author       ").append(it).append('\n') }
            append("// @run-at       document-end\n")
            patterns.forEach { pattern -> append("// @match        ").append(if (pattern.startsWith('*')) pattern else "*$pattern")
                .append(if (pattern.endsWith('*')) "" else "*").append('\n') }
            append("// @grant        none\n// ==/UserScript==\n")
            var metadata = false
            for (line in body.lineSequence()) {
                val text = line.trim()
                if (text.startsWith("// ==UserScript==")) metadata = true
                else if (text.startsWith("// ==/UserScript==")) metadata = false
                else if (!metadata) append(text).append('\n')
            }
        }
        val parsed = ScriptParser.parse(script) ?: return null
        return JSONObject().put("scriptId", parsed.key).put("enabled", item.optInt("flag") and 1 == 0).put("pureCode", script)
    }
    private fun encode(value: String) = if (value.isEmpty()) "" else Base64.encodeToString(value.trim().toByteArray(Charsets.UTF_8), Base64.DEFAULT)
    private fun decode(value: String) = Base64.decode(value, Base64.DEFAULT).toString(Charsets.UTF_8)
}
