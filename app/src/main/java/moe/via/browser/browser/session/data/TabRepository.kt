package moe.via.browser.browser.session.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import moe.via.browser.browser.session.state.TabNativeStateCodec
import moe.via.browser.storage.database.BrowserDatabase
import java.util.UUID

/** Unified Native tab records. Every operation except construction/close belongs on the persistence worker. */
class TabRepository private constructor(context: Context, private val store: BrowserDatabase?,
    private val externalDatabase: SQLiteDatabase?) : AutoCloseable {
    constructor(context: Context, store: BrowserDatabase = BrowserDatabase(context.applicationContext)) : this(context, store, null)
    /** The archive owns its transaction/database; closing this facade never closes that database. */
    constructor(context: Context, db: SQLiteDatabase) : this(context, null, db)

    private val db get() = externalDatabase ?: checkNotNull(store).writableDatabase
    private val rows get() = TabRows(db)
    private val files = TabFileStore(context)

    fun get(id: String): TabEntry? = if (id.isEmpty()) null else rows.get(id)

    fun opened(includePrivate: Boolean = false): List<TabEntry> = rows.query(
        if (includePrivate) "(flags & 2)=2" else "(flags & 2)=2 AND (flags & 1)=0")

    fun closed(): List<TabEntry> = rows.query("(flags & 3)=0", order = "last_visited_at DESC,rowid")

    /** na.c.b intentionally searches every flag state, including open and private rows. */
    fun search(query: String, limit: Int = 1024): List<TabEntry> {
        val trimmed = query.trim { it <= ' ' }
        if (trimmed.isEmpty()) return emptyList()
        val contains = "%_$trimmed%"
        return rows.query("url LIKE ? OR title LIKE ? OR title LIKE ?",
            arrayOf(contains, contains, "$trimmed%"), "last_visited_at DESC,rowid",
            limit.takeIf { it > 0 }?.toString())
    }

    fun materialize(id: String): TabRestoration? {
        val entry = get(id) ?: return null
        return materialize(entry)
    }

    fun materialize(entry: TabEntry): TabRestoration {
        val state = files.read(entry.filePath)?.takeIf { TabNativeStateCodec.decode(it) != null }
            ?: TabNativeStateCodec.metadata(entry.url, entry.title)
        return TabRestoration(entry, state)
    }

    /** ua.M1 / C: a checkpoint advances its visit time only after a new state file is written. */
    fun checkpoint(write: TabWrite): TabEntry? {
        val data = rows
        return data.transaction {
            if (!write.persistable) {
                write.persistentId?.let { erase(data, it) }
                return@transaction null
            }
            val old = write.persistentId?.let(data::get)
            var entry = entry(write, old, TabFlags.OPEN or if (write.isPrivate) TabFlags.PRIVATE else 0)
            if (old == null || needsState(old, write)) writeState(entry, write.state)?.let { path ->
                entry = entry.copy(filePath = path, lastVisitedAt = write.visitTime)
            }
            put(data, entry)
            entry
        }
    }

    /** ua.N1 / d: two ordered metadata commits surrounding state-file writes. */
    fun saveOpened(writes: List<TabWrite>): List<TabEntry> {
        val data = rows
        val stateById = HashMap<String, ByteArray?>()
        val prepared = data.transaction {
            val pending = writes.filter { it.persistable }.map { write ->
                val old = write.persistentId?.let(data::get)
                val flags = TabFlags.OPEN or (if (write.isPrivate) TabFlags.PRIVATE else 0) or
                    (if (write.isSelected) TabFlags.SELECTED else 0)
                Pending(write, old, entry(write, old, flags))
            }
            // na.a.j returns the same mutable record when several live tabs share one UUID.
            val finalMetadata = pending.associate { it.entry.id to it.entry }
            pending.forEach { if (needsState(it.old, it.write)) stateById[it.entry.id] = it.write.state }
            val normalized = pending.map { finalMetadata.getValue(it.entry.id) }
            writes.filterNot { it.persistable }.forEach { it.persistentId?.let { id -> erase(data, id) } }
            normalized.forEach { put(data, it) }
            normalized
        }
        val finalEntries = prepared.associateBy { it.id }.toMutableMap()
        prepared.forEach { item ->
            val current = finalEntries.getValue(item.id)
            writeState(current, stateById[item.id])?.let { path ->
                finalEntries[item.id] = current.copy(filePath = path, lastVisitedAt = System.currentTimeMillis())
            }
        }
        val committed = prepared.map { finalEntries.getValue(it.id) }
        return data.transaction {
            committed.forEach { put(data, it) }
            // Duplicate live runtime IDs can share one persistent UUID; retain one result per input.
            committed.map { data.get(it.id) ?: it }
        }
    }

    /** ua.K1 / M: closing always stamps worker time; a private/excluded tab leaves no closed entry. */
    fun recordClosed(write: TabWrite): TabEntry? {
        val data = rows
        return data.transaction {
            if (!write.persistable || write.isPrivate) {
                write.persistentId?.let { erase(data, it) }
                return@transaction null
            }
            val old = write.persistentId?.let(data::get)
            var entry = entry(write, old, (old?.flags ?: 0) and 7.inv())
            if (needsState(old, write)) writeState(entry, write.state)?.let { entry = entry.copy(filePath = it) }
            entry = entry.copy(lastVisitedAt = System.currentTimeMillis())
            put(data, entry)
            entry
        }
    }

    /** ua.L1 / a: preserve the first public URL in input order, deleting skipped mapped identities. */
    fun recordClosedBatch(writes: List<TabWrite>): List<TabEntry?> {
        if (writes.size == 1) return listOf(recordClosed(writes.single()))
        val data = rows
        val stateById = HashMap<String, ByteArray?>()
        val pending = data.transaction {
            val urls = HashSet<String>()
            val skipped = mutableListOf<String>()
            val values = writes.map { write ->
                if (write.url.isEmpty() || !write.persistable || write.isPrivate || write.url in urls) {
                    write.persistentId?.let(skipped::add)
                    null
                } else {
                    urls.add(write.url)
                    val old = write.persistentId?.let(data::get)
                    Pending(write, old, entry(write, old, 0))
                }
            }
            val kept = values.filterNotNull()
            val finalMetadata = kept.associate { it.entry.id to it.entry }
            kept.forEach { if (needsState(it.old, it.write)) stateById[it.entry.id] = it.write.state }
            skipped.forEach { erase(data, it) }
            values.map { it?.let { value -> finalMetadata.getValue(value.entry.id) } }
        }
        val finalEntries = pending.filterNotNull().associateBy { it.id }.toMutableMap()
        pending.filterNotNull().forEach { item ->
            val current = finalEntries.getValue(item.id)
            val path = writeState(current, stateById[item.id])
            finalEntries[item.id] = current.copy(filePath = path ?: current.filePath,
                lastVisitedAt = System.currentTimeMillis())
        }
        val committed = pending.map { it?.let { entry -> finalEntries.getValue(entry.id) } }
        return data.transaction {
            committed.filterNotNull().forEach { put(data, it) }
            committed.map { it?.let { entry -> data.get(entry.id) ?: entry } }
        }
    }

    /** ua.o0 keeps persistent identity and state, but sets a fresh open/selected/private flag triplet. */
    fun markOpened(id: String, isPrivate: Boolean, isSelected: Boolean): TabEntry? {
        val data = rows
        return data.transaction {
            val old = data.get(id) ?: return@transaction null
            val flags = (old.flags and 7.inv()) or TabFlags.OPEN or
                (if (isPrivate) TabFlags.PRIVATE else 0) or (if (isSelected) TabFlags.SELECTED else 0)
            old.copy(flags = flags, lastVisitedAt = System.currentTimeMillis())
                .also { put(data, it) }
        }
    }

    /** ua.h2: startup abandonment keeps real visit dates; private open rows are deleted. */
    fun abandonOpened() {
        val data = rows
        data.transaction {
            data.query("(flags & 2)=2").forEach { entry ->
                if ((entry.flags and TabFlags.PRIVATE) != 0) erase(data, entry.id)
                else put(data, entry.copy(flags = entry.flags and 6.inv()))
            }
        }
    }

    fun delete(id: String): Boolean {
        val data = rows
        return data.transaction {
            val old = data.get(id) ?: return@transaction false
            val deleted = data.delete(id) > 0
            if (deleted) files.delete(old.filePath)
            deleted
        }
    }

    /** na.a.h removes all non-open records, including private remnants, then prunes orphan states. */
    fun clearClosed(): Boolean {
        val data = rows
        val references = data.transaction {
            data.query("(flags & 2)=0").forEach { erase(data, it.id) }
            data.query().map { it.filePath }
        }
        files.prune(references)
        return true
    }

    fun exportClosed(): List<TabEntry> = rows.query("(flags & 3)=0")

    /** na.a.e compares URLs only against the pre-import store, retaining duplicates inside a batch. */
    fun importClosed(entries: List<TabEntry>): Int {
        val data = rows
        return data.transaction {
            val existing = data.query()
            val urls = existing.map { it.url }.toHashSet()
            val ids = existing.map { it.id }.toHashSet()
            var count = 0
            entries.forEach { source ->
                if (source.url.isNotEmpty() && source.url !in urls) {
                    var id = source.id
                    if (id.isEmpty() || id in ids) do { id = UUID.randomUUID().toString() } while (id in ids)
                    ids.add(id)
                    // Backup records have no files or open/private bits, irrespective of input flags.
                    data.insert(source.copy(id = id, filePath = null, flags = 0))
                    count++
                }
            }
            count
        }
    }

    private fun entry(write: TabWrite, old: TabEntry?, flags: Int) = TabEntry(
        id = old?.id ?: UUID.randomUUID().toString(), url = write.url, title = write.title,
        filePath = old?.filePath, flags = flags, lastVisitedAt = old?.lastVisitedAt ?: 0,
        home = write.home, desktop = write.desktop, scrollX = write.scrollX, scrollY = write.scrollY)

    private fun needsState(old: TabEntry?, write: TabWrite): Boolean = (old?.lastVisitedAt ?: 0) < write.visitTime

    private fun writeState(entry: TabEntry, state: ByteArray?): String? {
        if (state == null || state.isEmpty()) return null
        return try {
            if (TabNativeStateCodec.decode(state) == null) null else files.write(entry.id, state)
        } catch (_: Exception) { null }
    }

    private fun put(data: TabRows, entry: TabEntry) {
        data.put(entry).forEach {
            files.delete(it.filePath)
        }
    }
    private fun erase(data: TabRows, id: String) {
        val old = data.get(id) ?: return
        data.delete(id)
        files.delete(old.filePath)
    }

    private data class Pending(val write: TabWrite, val old: TabEntry?, val entry: TabEntry)
    override fun close() { store?.close() }
}
