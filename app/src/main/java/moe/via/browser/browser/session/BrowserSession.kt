package moe.via.browser.browser.session

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import moe.via.browser.R
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.home.HomeDocument
import moe.via.browser.feature.home.data.HomePreferences
import moe.via.browser.feature.startpage.document.ConfiguredStartPageDocuments
import moe.via.browser.feature.startpage.document.ConfiguredStartPageRequest
import moe.via.browser.browser.session.data.SessionTabPersistence
import moe.via.browser.browser.session.data.SessionTabWrite
import moe.via.browser.feature.internalpages.BrowserInternalPages
import moe.via.browser.browser.session.page.PageStackNavigationPolicy
import moe.via.browser.browser.session.data.TabRepository

class BrowserSession(private val context: Context, private val isPrivate: () -> Boolean,
    private val configure: (BrowserTab, WebView) -> Unit, private val callbacks: Callbacks,
    private val navigationPolicy: PageStackNavigationPolicy, private val pageHost: BrowserTab.PageHost,
    private val privateUrl: ((String) -> Boolean)? = null,
    private val persistentSession: Boolean = true) {
    interface Callbacks {
        fun changed()
        fun closed(batch: ClosedTabBatch)
        fun persistenceError(error: Throwable)
        fun engineUnavailable(error: moe.via.browser.webview.compat.WebEngineStartupException) { throw error }
        fun requestDefaultPage()
    }
    private val records = mutableListOf<BrowserTab>()
    val tabs: List<BrowserTab> get() = records
    var selectedIndex = -1
        private set
    val current: BrowserTab? get() = records.getOrNull(selectedIndex)
    private val main = Handler(Looper.getMainLooper())
    private val recentClosed = moe.via.browser.browser.session.closed.RecentClosedState()
    private var disposed = false
    private val privateMode get() = isPrivate()

    fun create(url: String, select: Boolean = true, index: Int = records.size,
        beforeStart: ((BrowserTab) -> Unit)? = null): BrowserTab {
        invalidateRecentClosed()
        val target = HomeDocument.normalize(context, url)
        val home = HomeDocument.isHome(context, target)
        return add(TabSnapshot(url = target, title = if (home) context.getString(R.string.ref_home_page_title) else target,
            home = home, lastVisitedAt = System.currentTimeMillis()), select, index, beforeStart)
    }

    /** g1(null)'s completion adds a resolved model without the explicit-input s0 side effect. */
    fun createResolvedStart(url: String, select: Boolean = true, index: Int = records.size): BrowserTab {
        val home = BrowserInternalPages.kind(context, url) == 1
        return add(TabSnapshot(url = url, title = if (home) context.getString(R.string.ref_home_page_title) else url,
            home = home, lastVisitedAt = System.currentTimeMillis(), literalStartUrl = url), select, index)
    }

    private fun add(snapshot: TabSnapshot, select: Boolean, index: Int = records.size,
        beforeStart: ((BrowserTab) -> Unit)? = null): BrowserTab {
        val id = if (snapshot.id.isEmpty()) TabRuntimeIds.allocate() else TabRuntimeIds.preserve(snapshot.id)
        val tab = BrowserTab(context, snapshot.copy(id = id), configure, navigationPolicy, pageHost)
        val position = index.coerceIn(0, records.size)
        val existing = current
        records.add(position, tab)
        if (existing != null && position <= selectedIndex) selectedIndex++
        beforeStart?.invoke(tab)
        if (select) select(position) else try { tab.deactivate() }
        catch (error: moe.via.browser.webview.compat.WebEngineStartupException) { callbacks.engineUnavailable(error) }
        callbacks.changed()
        return tab
    }

    fun select(index: Int) {
        if (index !in records.indices) return
        val previous = current
        if (previous !== records[index]) {
            previous?.lastVisitedAt = System.currentTimeMillis()
            previous?.let { checkpoint(it, selected = false) }
        }
        previous?.deactivate()
        selectedIndex = index
        val tab = records[index]
        try { tab.activate() }
        catch (error: moe.via.browser.webview.compat.WebEngineStartupException) { callbacks.engineUnavailable(error); return }
        callbacks.changed()
        checkpoint(tab)
    }

    fun close(index: Int) {
        closeIndices(listOf(index))
    }

    fun closeIndices(indices: List<Int>) {
        val removed = indices.distinct().sorted().mapNotNull(records::getOrNull)
        if (removed.isEmpty()) return
        val previous = current?.id
        val nextIndex = selectedIndex
        records.removeAll(removed.toSet())
        // Capture all page slots before final disposal; disposal is distinct from memory hibernation.
        val writes = removed.map { capture(it, selected = it.id == previous) }
        val singleClose = if (removed.size == 1 && writes.first().persistable && !writes.first().isPrivate)
            recentClosed.beginSingleClose() else null
        removed.forEach(BrowserTab::dispose)
        if (persistentSession) persist { repository ->
            val restored = SessionTabPersistence.close(repository, writes)
            main.post {
                if (!disposed) {
                    singleClose?.let { recentClosed.completed(it, restored.firstOrNull()?.persistence?.get()); callbacks.changed() }
                    if (restored.isNotEmpty()) callbacks.closed(ClosedTabBatch(restored, previous, removed.size))
                }
            }
        }
        if (records.isEmpty()) { selectedIndex = -1; callbacks.changed(); callbacks.requestDefaultPage() }
        else select(records.indexOfFirst { it.id == previous }.takeIf { it >= 0 } ?: nextIndex.coerceIn(records.indices))
        save()
    }

    fun move(from: Int, to: Int) {
        if (from !in records.indices || to !in records.indices || from == to) return
        val selectedId = current?.id
        records.add(to, records.removeAt(from))
        selectedIndex = records.indexOfFirst { it.id == selectedId }
        callbacks.changed(); save()
    }

    fun undo(batch: ClosedTabBatch) {
        val restored = batch.tabs.asReversed().map { it.id to addRestored(it, select = false) }
        restored.firstOrNull { it.first == batch.selectedId }?.second?.let { select(records.indexOf(it)) }
        save()
    }
    fun reopen(snapshot: TabSnapshot, select: Boolean = true) {
        addRestored(snapshot, select)
        save()
    }
    private fun addRestored(snapshot: TabSnapshot, select: Boolean): BrowserTab {
        val restored = snapshot.copy(id = "", persistence = snapshot.persistence.fork(),
            state = moe.via.browser.browser.session.state.TabNativeStateCodec.forkPageIdentities(snapshot.state))
        val privatePage = privateUrl?.invoke(restored.url) ?: privateMode
        if (persistentSession) persist { repository ->
            restored.persistence.get()?.let { id -> repository.markOpened(id,
                isPrivate = privatePage, isSelected = select) }
        }
        return add(restored, select)
    }

    fun duplicate(source: BrowserTab, select: Boolean = true, beforeStart: ((BrowserTab) -> Unit)? = null): BrowserTab {
        invalidateRecentClosed()
        val original = source.snapshot()
        val copy = original.copy(id = "", persistence = TabPersistenceId(), lastVisitedAt = System.currentTimeMillis(),
            state = moe.via.browser.browser.session.state.TabNativeStateCodec.forkPageIdentities(original.state))
        return add(copy, select, records.indexOf(source).takeIf { it >= 0 }?.plus(1) ?: records.size, beforeStart).also { save() }
    }

    fun forwardAvailable(): Boolean = current?.canGoForward() == true || recentClosed.available

    fun goForward() {
        val tab = current ?: return
        if (tab.canGoForward()) {
            if (tab.progress < 100) tab.stop()
            tab.goForward()
            invalidateRecentClosed()
        } else {
            val persistentId = recentClosed.take() ?: return
            callbacks.changed()
            val app = context.applicationContext
            val target = java.lang.ref.WeakReference(this)
            val dispatcher = main
            BrowserExecutors.io.execute {
                val result = runCatching { TabRepository(app).use { repository ->
                    repository.materialize(persistentId)?.let { SessionTabPersistence.snapshot(it.entry, it.state) }
                } }
                dispatcher.post {
                    val owner = target.get()?.takeUnless { it.disposed } ?: return@post
                    result.onSuccess { snapshot -> snapshot?.let { owner.reopen(it) } }
                        .onFailure { moe.via.browser.core.logging.NativeLog.warn("RecentClosed", "Unable to restore recent closed tab", it) }
                }
            }
        }
    }

    fun invalidateRecentClosed() {
        val available = recentClosed.available
        recentClosed.invalidate()
        if (available && !disposed) callbacks.changed()
    }

    fun readSaved(policy: Int = 1, forced: Boolean = false, configured: ConfiguredStartPageRequest,
        callback: (SavedSession) -> Unit) {
        if (!persistentSession) { callback(SavedSession(emptyList(), null)); return }
        val globalPrivate = privateMode
        val app = context.applicationContext
        BrowserExecutors.persistence.execute {
            val result = runCatching { TabRepository(app).use { repository ->
                val entries = repository.opened(includePrivate = forced)
                val selected = entries.firstOrNull(SessionTabPersistence::selected)?.id
                val snapshots = entries.mapNotNull { entry -> repository.materialize(entry.id)?.let {
                    SessionTabPersistence.snapshot(it.entry, it.state)
                } }
                val conditional = !forced || snapshots.isEmpty()
                val discard = conditional && (snapshots.isEmpty() || policy == 0 || globalPrivate)
                if (discard || conditional && policy == 2) repository.abandonOpened()
                if (discard) SavedSession(emptyList(), null)
                else if ((!conditional || policy != 2) && snapshots.isNotEmpty() && selected == null) {
                    // ua.v/N0 reads the configured choice here, within the actual restore worker.
                    val url = ConfiguredStartPageDocuments.generate(app,
                        configured.copy(configured = HomePreferences(app).homepage))
                    val home = BrowserInternalPages.kind(app, url) == 1
                    val fallback = TabSnapshot(url = url,
                        title = if (home) app.getString(R.string.ref_home_page_title) else url,
                        home = home, literalStartUrl = url)
                    SavedSession(snapshots + fallback, null)
                } else SavedSession(snapshots, null, selected)
            } }
            main.post {
                if (disposed) return@post
                result.onSuccess(callback).onFailure { callbacks.persistenceError(it); callback(SavedSession(emptyList(), null)) }
            }
        }
    }

    fun restore(session: SavedSession, privateHistoryIds: Set<String> = emptySet(), runtimeStates: Map<String, TabRuntimeState> = emptyMap(),
        preserveRuntime: Boolean = false, activate: Boolean = true) {
        if (activate) {
            records.forEach(BrowserTab::dispose); records.clear(); selectedIndex = -1
        }
        session.tabs.forEach { snapshot -> add(if (preserveRuntime) snapshot else snapshot.copy(id = ""), false,
            beforeStart = { tab ->
                tab.hasPrivateHistory = snapshot.id in privateHistoryIds
                runtimeStates[snapshot.id]?.let { tab.runtime = it }
            }) }
        if (activate) {
            if (records.isEmpty()) callbacks.changed() else activateRestored(session)
        }
    }

    /** R1's completion chooses once, after restored and incoming models have all been appended. */
    fun activateRestored(saved: SavedSession) {
        if (records.isEmpty()) return
        val persistent = saved.selectedPersistentId?.let { id -> records.indexOfFirst { it.persistence.get() == id } }
        val runtime = saved.selectedId?.let { id -> records.indexOfFirst { it.id == id } }
        select(persistent?.takeIf { it >= 0 } ?: runtime?.takeIf { it >= 0 } ?: records.lastIndex)
        save()
    }

    fun save() {
        if (!persistentSession || records.isEmpty() || disposed) return
        val writes = records.map { capture(it) }
        persist { SessionTabPersistence.save(it, writes) }
    }

    fun clearClosed() { persist { it.clearClosed() } }
    fun retainInMemory() = RetainedSession(SavedSession(records.map(BrowserTab::snapshot), current?.id),
        records.filter { it.hasPrivateHistory }.mapTo(mutableSetOf()) { it.id }, records.associate { it.id to it.runtime })

    private fun capture(tab: BrowserTab, selected: Boolean = tab === current): SessionTabWrite {
        val snapshot = tab.snapshot()
        val address = snapshot.url
        val allowed = address.isNotEmpty() && !BrowserInternalPages.isInternal(context, address) &&
            !(address.length > 6 && address.startsWith("about:", ignoreCase = true))
        return SessionTabWrite(snapshot,
            allowed, privateUrl?.invoke(address) ?: privateMode, selected,
            if (selected) System.currentTimeMillis() else tab.lastVisitedAt)
    }
    fun checkpoint(tab: BrowserTab, selected: Boolean = tab === current) {
        if (!persistentSession || disposed || tab !in records) return
        val write = capture(tab, selected)
        persist { SessionTabPersistence.checkpoint(it, write) }
    }
    fun trimMemory(level: Int) {
        val tier = when {
            level == 10 -> 1
            level in 11..15 || level == 60 -> 2
            level > 60 -> 3
            else -> return
        }
        records.forEach { tab ->
            val selected = tab === current
            val keep = when (tier) {
                1 -> if (selected) return@forEach else 4
                2 -> if (selected) 4 else 2
                else -> if (selected) 2 else 1
            }
            tab.trimMemory(keep)
        }
    }
    fun dispose() {
        if (disposed) return
        save(); disposed = true
        records.forEach(BrowserTab::dispose); records.clear(); selectedIndex = -1
    }
    private fun persist(operation: (TabRepository) -> Unit) {
        BrowserExecutors.persistence.execute {
            runCatching { TabRepository(context.applicationContext).use(operation) }.onFailure { error ->
                main.post { if (!disposed) callbacks.persistenceError(error) }
            }
        }
    }
}
