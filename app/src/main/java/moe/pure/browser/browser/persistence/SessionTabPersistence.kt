package moe.pure.browser.browser.persistence

import moe.pure.browser.browser.ClosedTab
import moe.pure.browser.browser.TabEntry
import moe.pure.browser.browser.TabFlags
import moe.pure.browser.browser.TabPersistenceId
import moe.pure.browser.browser.TabRepository
import moe.pure.browser.browser.TabSnapshot
import moe.pure.browser.browser.TabWrite

/** Captured UI values with a shared allocation handle; database UUIDs are resolved on the worker. */
internal data class SessionTabWrite(
    val snapshot: TabSnapshot,
    val persistable: Boolean,
    val isPrivate: Boolean,
    val isSelected: Boolean,
    val visitTime: Long
) {
    fun record() = TabWrite(persistentId = snapshot.persistence.get(), url = snapshot.url, title = snapshot.title,
        persistable = persistable, isPrivate = isPrivate, isSelected = isSelected, visitTime = visitTime,
        state = snapshot.state, home = snapshot.home, desktop = snapshot.desktop,
        scrollX = snapshot.scrollX, scrollY = snapshot.scrollY)

    fun bind(entry: TabEntry?) { entry?.let { snapshot.persistence.bind(it.id) } }
}

internal object SessionTabPersistence {
    fun checkpoint(repository: TabRepository, write: SessionTabWrite) = write.bind(repository.checkpoint(write.record()))
    fun close(repository: TabRepository, writes: List<SessionTabWrite>): List<TabSnapshot> =
        writes.zip(repository.recordClosedBatch(writes.map(SessionTabWrite::record)))
            .mapNotNull { (write, entry) -> write.bind(entry); entry?.let { write.snapshot } }

    fun save(repository: TabRepository, writes: List<SessionTabWrite>) {
        // Native N1 batches metadata before writing states. Allocate/check existing UUIDs in this
        // same worker turn so a queued close/reopen cannot observe a late main-thread assignment.
        val entries = repository.saveOpened(writes.map(SessionTabWrite::record))
        val persisted = writes.filter { it.persistable }
        persisted.zip(entries).forEach { (write, entry) -> write.bind(entry) }
    }

    fun snapshot(entry: TabEntry, state: ByteArray? = null) = TabSnapshot(url = entry.url, title = entry.title,
        home = entry.home, desktop = entry.desktop, scrollX = entry.scrollX, scrollY = entry.scrollY,
        state = state, persistence = TabPersistenceId(entry.id), lastVisitedAt = entry.lastVisitedAt)

    fun closed(entry: TabEntry) = ClosedTab(entry.id, snapshot(entry), entry.lastVisitedAt)
    fun selected(entry: TabEntry) = entry.flags and TabFlags.SELECTED != 0
}
