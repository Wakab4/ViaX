package moe.via.browser.app.startup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import moe.via.browser.R
import moe.via.browser.browser.navigation.entry.BrowserEntry
import moe.via.browser.browser.session.BrowserSession
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.browser.session.RetainedSession
import moe.via.browser.browser.session.SavedSession
import moe.via.browser.feature.startpage.StartupRestoreNotice
import moe.via.browser.feature.startpage.document.ConfiguredStartPageDocuments
import moe.via.browser.storage.preferences.BrowserPreferences

internal class BrowserSessionRestore(
    private val activity: Activity,
    private val startup: BrowserStartup,
    private val preferences: BrowserPreferences,
    private val host: Host,
) : AutoCloseable {
    interface Host {
        val session: BrowserSession
        fun incoming(): Intent?
        fun prepareEntry(intent: Intent?, completed: (BrowserEntry?) -> Unit)
        fun openEntry(entry: BrowserEntry, select: Boolean, completed: (BrowserTab?) -> Unit)
        fun openConfiguredPage(completed: () -> Unit)
        fun restoreClosedTabs(link: String)
        fun consumeIncoming(intent: Intent)
    }

    var restoring = false
        private set
    private var queued: Intent? = null
    private val version = lazy { NativeStartupVersion(activity.applicationContext) }
    private val notice = lazy { StartupRestoreNotice(activity) }

    fun start(state: Bundle?, retained: RetainedSession?, customTabMode: Boolean) {
        restoring = true
        if (retained != null && retained.session.tabs.isNotEmpty()) {
            host.session.restore(retained.session, retained.privateHistoryIds, retained.runtime, preserveRuntime = true)
            finish()
        } else if (customTabMode) {
            host.prepareEntry(host.incoming()) { entry ->
                if (entry == null) {
                    host.session.create("about:blank")
                    finish()
                } else host.openEntry(entry, true) { finish() }
            }
        } else restoreSaved(state)
    }

    private fun restoreSaved(state: Bundle?) {
        if (!startup.ready()) return
        val policy = preferences.restoreTabs
        val forced = state?.getBoolean("restore_session", false) == true
        val pendingEntry = state?.getBoolean("entry_pending", false) == true
        val session = host.session
        session.readSaved(policy, forced, ConfiguredStartPageDocuments.prepare(activity)) { saved ->
            if (!startup.ready()) return@readSaved
            if (session.tabs.isNotEmpty()) { finish(); return@readSaved }
            val restored = saved.takeIf { it.tabs.isNotEmpty() && (forced || policy == 1) }
            if (restored != null) session.restore(restored, activate = false)
            else if (saved.tabs.isNotEmpty() && policy == 2) {
                notice.value.show(saved.tabs.mapNotNull { it.persistence.get() }, saved.selectedPersistentId) { ids, selectedId ->
                    val link = "v://tabs/restore?ids=" + ids.joinToString(",") + (selectedId?.let { "&selected=$it" } ?: "")
                    host.restoreClosedTabs(link)
                }
            }
            host.prepareEntry(host.incoming().takeUnless { forced && !pendingEntry }) { entry ->
                val generation = startup.generation
                version.value.apply(activity.getString(R.string.ref_startup_custom)) {
                    if (startup.ready(generation)) {
                        if (entry == null) finishModels(restored, null)
                        else host.openEntry(entry, false) { finishModels(restored, it) }
                    }
                }
            }
        }
    }

    private fun finishModels(restored: SavedSession?, incoming: BrowserTab?) {
        if (!startup.ready()) return
        val session = host.session
        val tabs = session.tabs
        if (tabs.isEmpty()) {
            host.openConfiguredPage(::finish)
            return
        }
        if (incoming != null) tabs.indexOf(incoming).takeIf { it >= 0 }?.let(session::select)
        else if (restored != null) session.activateRestored(restored)
        else if (session.current == null) session.select(tabs.lastIndex)
        session.save()
        finish()
    }

    fun queueIfRestoring(intent: Intent): Boolean {
        if (!restoring) return false
        queued = Intent(intent)
        return true
    }

    fun abort() { restoring = false }

    private fun finish() {
        restoring = false
        val intent = queued
        queued = null
        if (startup.started && intent != null) host.consumeIncoming(intent)
    }

    override fun close() {
        if (version.isInitialized()) version.value.close()
        if (notice.isInitialized()) notice.value.close()
        queued = null
    }
}
