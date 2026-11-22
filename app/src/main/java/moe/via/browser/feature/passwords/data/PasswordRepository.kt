package moe.via.browser.feature.passwords.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import moe.via.browser.feature.passwords.crypto.PasswordCipher
import moe.via.browser.feature.passwords.crypto.PasswordCrypto
import moe.via.browser.feature.passwords.format.PasswordCsv
import moe.via.browser.feature.passwords.model.PasswordCredential
import moe.via.browser.feature.passwords.model.PasswordEntry
import moe.via.browser.feature.passwords.model.PasswordSites
import moe.via.browser.feature.passwords.model.StoredPassword
import java.io.Closeable
import java.io.Reader
import java.io.Writer
import java.util.UUID

/** All methods are synchronous worker operations. Constructing a repository does not open SQL or KeyStore. */
class PasswordRepository(context: Context) : Closeable {
    private val app = context.applicationContext
    private val helper = lazy { PasswordDatabase(app) }
    private val crypto = PasswordCrypto(app)
    private val preferences = PasswordPreferences(app)
    private val ignored = PasswordIgnoredSites(app)
    private var closed = false
    private val db: SQLiteDatabase get() { check(!closed) { "Password repository is closed" }; return helper.value.writableDatabase }

    fun all(query: String = ""): List<PasswordEntry> = synchronized(LOCK) {
        if (query.isEmpty()) entries(null, null) else entries("name LIKE ? OR username LIKE ?", arrayOf("%$query%", "%$query%"))
    }
    fun get(id: String): PasswordEntry? = synchronized(LOCK) {
        if (id.isEmpty()) null else db.query("pass", META, "id=?", arrayOf(id), null, null, null, "1").use {
            if (it.moveToFirst()) it.entry() else null
        }
    }
    fun count(): Int = synchronized(LOCK) {
        db.rawQuery("SELECT COUNT(*) FROM pass", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }
    fun find(name: String, username: String): String? = synchronized(LOCK) {
        if (name.isEmpty() || username.isEmpty()) null else
            db.query("pass", arrayOf("id"), "name=? AND username=?", arrayOf(name, username), null, null, null, "1").use {
                if (it.moveToFirst()) it.getString(0) else null
            }
    }

    fun candidates(url: String): List<PasswordEntry> = synchronized(LOCK) {
        val authority = PasswordSites.authority(url)
        if (authority.isEmpty()) return@synchronized emptyList()
        val base = PasswordSites.baseDomain(url)
        val suffix = ".$base"
        val candidates = if (authority == base) entries("name=? OR substr(name,?)=?", arrayOf(authority, "-${suffix.length}", suffix))
        else entries("name=? OR name=? OR substr(name,?)=?", arrayOf(authority, base, "-${suffix.length}", suffix))
        fun priority(entry: PasswordEntry) = when {
            entry.name.equals(authority, true) -> 3
            entry.name.equals(base, true) -> 2
            else -> 1
        }
        candidates.sortedWith(compareByDescending<PasswordEntry>(::priority).thenBy { it.createdAt })
    }

    fun shouldPrompt(url: String): Boolean = preferences.promptToSave && !ignored.contains(PasswordSites.authority(url))

    fun decrypt(id: String): PasswordCredential? = synchronized(LOCK) {
        stored(id)?.let { PasswordCredential(it.entry, crypto.decrypt(it.ciphertext)) }
    }

    /** Capture does not trim fields. The manager editor applies its own four-field trim. */
    fun save(entry: PasswordEntry, password: String): PasswordEntry = synchronized(LOCK) {
        require(entry.name.isNotEmpty() && password.isNotEmpty()) { "站点和密码不能为空。" }
        val previous = get(entry.id)
        val now = System.currentTimeMillis()
        val saved = entry.copy(id = entry.id.ifEmpty { UUID.randomUUID().toString() },
            createdAt = previous?.createdAt ?: entry.createdAt.takeIf { it > 0 } ?: now, updatedAt = now)
        val encrypted = crypto.encrypt(password, allowCreate = count() == 0)
        transaction {
            if (previous == null) {
                delete("pass", "name=? AND username=?", arrayOf(saved.name, saved.username))
                insertOrThrow("pass", null, values(saved, encrypted))
            } else {
                check(update("pass", values(saved, encrypted, includeId = false), "id=?", arrayOf(saved.id)) == 1)
            }
        }
        saved
    }

    fun delete(id: String): Boolean = synchronized(LOCK) {
        id.isNotEmpty() && db.delete("pass", "id=?", arrayOf(id)) > 0
    }
    fun delete(ids: Collection<String>): Int = synchronized(LOCK) {
        var removed = 0
        transaction { for (id in ids.distinct()) if (id.isNotEmpty()) removed += delete("pass", "id=?", arrayOf(id)) }
        removed
    }

    fun importCsv(reader: Reader): Int {
        val parsed = PasswordCsv.parse(reader)
        return if (parsed.format == 0) -1 else importCredentials(parsed.entries)
    }

    /** Actual committed count; an exception rolls back the entire batch rather than reporting Native's false success. */
    fun importCredentials(credentials: List<PasswordCredential>): Int = synchronized(LOCK) {
        if (credentials.isEmpty()) return@synchronized 0
        val mayCreate = count() == 0
        val rows = credentials.map { credential ->
            require(credential.entry.name.isNotEmpty() && credential.password.isNotEmpty())
            val now = System.currentTimeMillis()
            val entry = credential.entry.copy(id = credential.entry.id.ifEmpty { UUID.randomUUID().toString() },
                createdAt = credential.entry.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = credential.entry.updatedAt.takeIf { it > 0 } ?: now)
            StoredPassword(entry, crypto.encrypt(credential.password, mayCreate), PasswordCipher.VERSION)
        }
        transaction {
            for (row in rows) {
                delete("pass", "name=? AND username=?", arrayOf(row.entry.name, row.entry.username))
                insertOrThrow("pass", null, values(row.entry, row.ciphertext))
            }
        }
        rows.size
    }

    fun exportCredentials(): List<PasswordCredential> = synchronized(LOCK) {
        all().map { entry -> decrypt(entry.id) ?: error("Password was removed during export") }
    }
    fun exportCsv(writer: Writer): Int = PasswordCsv.write(writer, exportCredentials())

    private fun entries(selection: String?, arguments: Array<String>?): List<PasswordEntry> =
        db.query("pass", META, selection, arguments, null, null, "created_at ASC").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.entry()) }
        }
    private fun stored(id: String): StoredPassword? {
        if (id.isEmpty()) return null
        return db.query("pass", null, "id=?", arrayOf(id), null, null, null, "1").use {
            if (it.moveToFirst()) StoredPassword(it.entry(), it.getString(it.getColumnIndexOrThrow("password")).orEmpty()) else null
        }
    }
    private fun Cursor.entry() = PasswordEntry(
        getString(getColumnIndexOrThrow("id")), getString(getColumnIndexOrThrow("name")).orEmpty(),
        getString(getColumnIndexOrThrow("url")).orEmpty(), getString(getColumnIndexOrThrow("username")).orEmpty(),
        getString(getColumnIndexOrThrow("note")).orEmpty(), getLong(getColumnIndexOrThrow("created_at")), getLong(getColumnIndexOrThrow("updated_at"))
    )
    private fun values(entry: PasswordEntry, encrypted: String, includeId: Boolean = true) = ContentValues().apply {
        if (includeId) put("id", entry.id)
        put("name", entry.name); put("url", entry.url); put("username", entry.username); put("note", entry.note)
        put("password", encrypted); put("created_at", entry.createdAt); put("updated_at", entry.updatedAt)
    }
    private fun <T> transaction(block: SQLiteDatabase.() -> T): T {
        val database = db
        database.beginTransaction()
        try { val result = database.block(); database.setTransactionSuccessful(); return result }
        finally { database.endTransaction() }
    }
    override fun close() = synchronized(LOCK) {
        if (closed) return@synchronized
        try { ignored.close() } finally {
            crypto.close(); if (helper.isInitialized()) helper.value.close(); closed = true
        }
    }
    private companion object {
        val LOCK = Any()
        val META = arrayOf("id", "name", "url", "username", "note", "created_at", "updated_at")
    }
}
