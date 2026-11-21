package moe.pure.browser.feature.scripts.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import moe.pure.browser.data.BrowserStore
import moe.pure.browser.feature.scripts.model.ScriptOverrides
import moe.pure.browser.feature.scripts.model.UserScript
import moe.pure.browser.feature.scripts.model.ScriptUpdateTarget
import moe.pure.browser.feature.scripts.model.ScriptRestoreTarget
import moe.pure.browser.feature.scripts.model.ScriptBackupTarget
import moe.pure.browser.feature.scripts.model.ScriptDetailsData

class ScriptRepository(context: Context, private val store: BrowserStore = BrowserStore(context.applicationContext)) : AutoCloseable {
    private val db get() = store.writableDatabase
    private val resourceData = ScriptResourceData(context.applicationContext)
    private val detailsData = ScriptDetailsDataSource { store.readableDatabase }
    internal fun details(id: Int): ScriptDetailsData {
        val source = ScriptExecutionCache.get(id.toLong())
        return ScriptDetailsData(detailsData.metadata(id), source, detailsData.defaults(id), detailsData.overrides(id))
    }
    internal fun detailsMetadata(key: String) = detailsData.metadata(key)
    @Synchronized fun all(): List<UserScript> = query(null, null, "created_at DESC,_id DESC")
    /** p5.b.s preserves SQLite LENGTH(content) order and accumulated records on a read failure. */
    @Synchronized internal fun executionRecords(): List<UserScript> {
        val result = ArrayList<UserScript>()
        try {
            db.query("scripts", null, null, null, null, null, "LENGTH(content) ASC").use { rows ->
                if (rows is android.database.AbstractWindowedCursor) rows.window = android.database.CursorWindow("scripts", 8 * 1024 * 1024L)
                while (rows.moveToNext()) result.add(read(rows))
            }
        } catch (error: Exception) { moe.pure.browser.core.logging.NativeLog.warn("Scripts", "Load execution records", error) }
        return result
    }
    @Synchronized internal fun addonHomepages(): List<String?> = db.query("scripts", arrayOf("homepage_url"),
        null, null, null, null, "created_at DESC").use { rows ->
        buildList { while (rows.moveToNext()) add(rows.getString(0)) }
    }
    @Synchronized fun get(id: Long): UserScript? = query("_id=?", arrayOf(id.toString())).firstOrNull()
    @Synchronized fun find(key: String): UserScript? = query("script_id=?", arrayOf(key)).firstOrNull()
    @Synchronized fun findForPageInstall(key: String, downloadUrl: String?): UserScript? =
        if (downloadUrl == null) find(key) else query("script_id=? OR download_url=?", arrayOf(key, downloadUrl)).firstOrNull()

    @Synchronized internal fun backupTargets(): List<ScriptBackupTarget> {
        val result = ArrayList<ScriptBackupTarget>()
        try {
            db.query("scripts", arrayOf("_id", "name", "download_url", "enabled", "last_updated_at"),
                null, null, null, null, "created_at DESC").use { rows ->
                while (rows.moveToNext()) result.add(ScriptBackupTarget(rows.getLong(0), rows.getString(1), rows.getString(2),
                    rows.getInt(3) != 0, rows.getLong(4)))
            }
        } catch (error: Exception) { error.printStackTrace() }
        return result
    }

    internal fun backupKey(id: Long): String? = ScriptExecutionCache.get(id)?.key

    @Synchronized internal fun backupBody(id: Long): String? {
        ScriptExecutionCache.get(id)?.let { return it.content }
        return backupString(id, "content", large = true)
    }

    @Synchronized internal fun backupOverrides(id: Long): String? = ScriptCodec.backupOverrides(backupString(id, "user_overrides"))

    private fun backupString(id: Long, column: String, large: Boolean = false): String? {
        var value: String? = null
        try {
            db.query("scripts", arrayOf(column), "_id = ?", arrayOf(id.toString()), null, null, null).use { rows ->
                if (large && rows is android.database.AbstractWindowedCursor) rows.window = android.database.CursorWindow("test", 8 * 1024 * 1024L)
                if (rows.moveToFirst()) value = rows.getString(0)
            }
        } catch (error: Exception) { error.printStackTrace() }
        return value
    }

    /** p5.b.i compares the first matching metadata row without ordering or reading its content. */
    @Synchronized internal fun findForBackup(key: String, url: String): ScriptRestoreTarget? {
        if (key.isEmpty() && url.isEmpty()) return null
        val (where, args) = when {
            key.isEmpty() -> "download_url = ?" to arrayOf(url)
            url.isEmpty() -> "script_id = ?" to arrayOf(key)
            else -> "script_id = ? OR download_url = ?" to arrayOf(key, url)
        }
        return try {
            db.query("scripts", arrayOf("_id", "last_updated_at", "enabled"), where, args, null, null, null).use { rows ->
                if (rows.moveToFirst()) ScriptRestoreTarget(rows.getLong(0), rows.getLong(1), rows.getInt(2) != 0) else null
            }
        } catch (error: Exception) { error.printStackTrace(); null }
    }

    /** Native h/p does not perform save()'s extra key lookup, enabled retention or insert-or-throw. */
    @Synchronized internal fun restoreFromBackup(script: UserScript, target: ScriptRestoreTarget?, rawOverrides: String?): Long {
        var value = script
        val data = values(script).apply {
            remove("support_url")
            if (rawOverrides == null) putNull("user_overrides") else put("user_overrides", rawOverrides)
        }
        val id = if (target == null) db.insert("scripts", null, data) else {
            data.remove("created_at")
            if (rawOverrides == null) {
                data.remove("user_overrides")
                var stored: String? = null
                try {
                    db.query("scripts", arrayOf("user_overrides"), "_id = ?", arrayOf(target.id.toString()), null, null, null).use { rows ->
                        if (rows.moveToFirst()) stored = rows.getString(0)
                    }
                } catch (error: Exception) { error.printStackTrace() }
                value = script.copy(overrides = ScriptCodec.overrides(stored))
            }
            if (db.update("scripts", data, "_id = ?", arrayOf(target.id.toString())) > 0) target.id else 0L
        }
        if (id > 0) ScriptExecutionCache.put(value.copy(id = id))
        return id
    }

    @Synchronized fun save(script: UserScript, targetId: Long? = null): Long {
        val old = targetId?.let(::get) ?: find(script.key)
        val value = script.copy(id = old?.id ?: 0, enabled = old?.enabled ?: script.enabled, created = old?.created ?: script.created, overrides = old?.overrides ?: script.overrides)
        val data = values(value)
        val id = if (old == null) db.insertOrThrow("scripts", null, data) else { check(db.update("scripts", data, "_id=?", arrayOf(old.id.toString())) == 1); old.id }
        ScriptExecutionCache.put(value.copy(id = id))
        return id
    }
    /** p5.b.p updates the requested row, retaining created_at and stored source overrides. */
    @Synchronized internal fun updateFromSource(id: Long, script: UserScript): Boolean {
        if (id <= 0) return false
        val overrides = db.query("scripts", arrayOf("user_overrides"), "_id=?", arrayOf(id.toString()), null, null, null).use {
            ScriptCodec.overrides(if (it.moveToFirst()) it.getString(0) else null)
        }
        val value = script.copy(id = id, overrides = overrides)
        val data = values(value).apply { remove("created_at"); remove("user_overrides"); remove("support_url") }
        if (db.update("scripts", data, "_id=?", arrayOf(id.toString())) <= 0) return false
        ScriptExecutionCache.put(value)
        return true
    }
    @Synchronized internal fun updateTargets(): List<ScriptUpdateTarget> {
        val targets = ArrayList<ScriptUpdateTarget>()
        try {
            db.query("scripts", arrayOf("_id", "name", "download_url", "enabled", "last_updated_at"), null, null, null, null,
                "created_at DESC").use { rows ->
                if (rows is android.database.AbstractWindowedCursor) rows.window = android.database.CursorWindow("scripts", 8 * 1024 * 1024L)
                while (rows.moveToNext()) targets.add(ScriptUpdateTarget(rows.getLong(0), rows.getString(1), rows.getString(2),
                    rows.getInt(3) != 0, rows.getLong(4)))
            }
        } catch (error: Exception) { moe.pure.browser.core.logging.NativeLog.warn("ScriptUpdater", "Load update metadata", error) }
        return targets
    }
    @Synchronized fun enable(id: Long, enabled: Boolean) {
        if (db.update("scripts", ContentValues().apply { put("enabled", if (enabled) 1 else 0) }, "_id=?", arrayOf(id.toString())) > 0)
            ScriptExecutionCache.enabled(id, enabled)
    }
    @Synchronized fun remove(id: Long) {
        if (db.delete("scripts", "_id=?", arrayOf(id.toString())) > 0) {
            val key = ScriptExecutionCache.get(id)?.key
            ScriptExecutionCache.remove(id)
            if (key != null) clearValues(key)
        }
    }
    @Synchronized fun override(id: Long, value: ScriptOverrides) {
        if (id <= 0) return
        if (db.update("scripts", ContentValues().apply { put("user_overrides", ScriptCodec.overrides(value)) }, "_id=?", arrayOf(id.toString())) > 0)
            get(id)?.let(ScriptExecutionCache::put)
    }
    @Synchronized fun setValue(key: String, name: String, value: String) { db.insertWithOnConflict("script_values", null, ContentValues().apply { put("script_id", key); put("name", name); put("value", value) }, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE) }
    @Synchronized fun value(key: String, name: String): String? = db.query("script_values", arrayOf("value"), "script_id=? AND name=?", arrayOf(key, name), null, null, null).use { if (it.moveToFirst()) it.getString(0) else null }
    @Synchronized fun valueNames(key: String): List<String> = db.query("script_values", arrayOf("name"), "script_id=?", arrayOf(key), null, null, "rowid").use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
    @Synchronized fun deleteValue(key: String, name: String) { db.delete("script_values", "script_id=? AND name=?", arrayOf(key, name)) }
    @Synchronized fun clearValues(key: String) { db.delete("script_values", "script_id=?", arrayOf(key)) }
    @Synchronized fun resource(url: String): ByteArray? = resourceData.read(url)
    @Synchronized fun resourceText(url: String): String? = resourceData.text(url)
    internal fun resourcePath(url: String): String? = resourceData.path(url)
    internal fun downloadResource(url: String): Boolean = !ScriptResourceFileStore.http(url) || fetchResource(url).success
    @Synchronized fun resourceTimestamp(url: String): Long? = resourceData.timestamp(url)
    internal fun fetchResource(url: String): ScriptResourceFetch = resourceData.fetch(url)
    /** p5.a.t keeps references of every installed script, including disabled ones. Worker only. */
    fun pruneResourceFiles(): Boolean = resourceData.prune(ScriptExecutionCache.all().flatMap { it.requires + it.resources.values }.toSet())
    private fun query(where: String?, args: Array<String>?, order: String? = null): List<UserScript> = db.query("scripts", null, where, args, null, null, order).use { c ->
        if (c is android.database.AbstractWindowedCursor) c.window = android.database.CursorWindow("scripts", 8 * 1024 * 1024L)
        buildList { while (c.moveToNext()) add(read(c)) }
    }
    private fun read(c: Cursor): UserScript {
        fun text(key: String): String? = c.getString(c.getColumnIndexOrThrow(key))
        fun number(key: String) = c.getLong(c.getColumnIndexOrThrow(key))
        return UserScript(number("_id"), text("script_id")!!, text("name")!!, text("content")!!, text("version"),
            ScriptCodec.list(text("matches")), ScriptCodec.list(text("includes")), ScriptCodec.list(text("exclude_matches")), ScriptCodec.list(text("excludes")),
            number("run_at").toInt(), ScriptCodec.list(text("requires")), ScriptCodec.map(text("resources")), number("enabled") != 0L,
            number("flags").toInt(), number("grant").toInt(), text("icon_url"), text("homepage_url"), text("support_url"), text("download_url"),
            number("last_updated_at"), number("created_at"), ScriptCodec.overrides(text("user_overrides")))
    }
    private fun values(s: UserScript) = ContentValues().apply {
        put("script_id", s.key); put("name", s.name); put("version", s.version); put("content", s.content)
        put("matches", ScriptCodec.list(s.matches)); put("includes", ScriptCodec.list(s.includes)); put("exclude_matches", ScriptCodec.list(s.excludeMatches)); put("excludes", ScriptCodec.list(s.excludes))
        put("run_at", s.runAt); put("requires", ScriptCodec.list(s.requires)); put("resources", ScriptCodec.map(s.resources)); put("enabled", if (s.enabled) 1 else 0)
        put("flags", s.flags); put("grant", s.grant); put("icon_url", s.iconUrl); put("homepage_url", s.homepageUrl); put("support_url", s.supportUrl); put("download_url", s.downloadUrl)
        put("user_overrides", ScriptCodec.overrides(s.overrides)); put("last_updated_at", s.updated); put("created_at", s.created)
    }
    override fun close() = store.close()
}
