package moe.via.browser.feature.download.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import moe.via.browser.feature.download.transfer.DownloadCoordinator
import moe.via.browser.feature.download.model.DownloadDraft
import moe.via.browser.feature.download.model.DownloadState
import moe.via.browser.feature.download.model.DownloadTask
import moe.via.browser.feature.download.DownloadTaskActions
import moe.via.browser.ui.components.recycler.NativeRangeSelection

/** A download view owns rows, selection and transient windows; the application still owns transfers. */
internal class DownloadPageContent(activity: FragmentActivity, private val state: DownloadPanelState,
    fragments: FragmentManager, restored: Bundle?, visible: () -> Boolean, onBack: () -> Unit,
    private val editingChanged: (Boolean) -> Unit, applyInsets: Boolean = true) : AutoCloseable {
    private val queue = DownloadCoordinator.get(activity.applicationContext)
    private var closed = false
    private var observing = false
    private val requests = DownloadRequests(activity, fragments, visible)
    private val actions = DownloadTaskActions(activity, fragments, visible) { state.editing = false; state.selected.clear(); refresh() }
    private val observer: () -> Unit = { refresh() }
    val page: DownloadManagerPage
    private val rows: DownloadListAdapter
    private val range: NativeRangeSelection
    val canNavigateBack get() = !state.editing

    init {
        state.restore(restored?.getBundle(STATE))
        page = DownloadManagerPage(activity, state, object : DownloadManagerPage.Actions {
            override fun back() = onBack()
            override fun create() = requests.confirm(DownloadDraft("https://", manual = true))
            override fun queryChanged(raw: String) { range.finish(); state.query = raw; state.selected.clear(); refresh() }
            override fun categoryChanged(category: moe.via.browser.feature.download.DownloadCategory) {
                range.finish(); state.category = category; state.selected.clear(); refresh()
            }
            override fun toggleEditing() {
                range.finish(); state.editing = !state.editing
                if (!state.editing) state.selected.clear()
                refresh()
            }
            override fun selectAll() {
                range.finish()
                val visible = rows.visible.map { it.id }
                if (visible.isNotEmpty() && visible.all { it in state.selected }) state.selected.removeAll(visible.toSet())
                else state.selected.addAll(visible)
                refresh()
            }
            override fun removeSelected() { range.finish(); actions.remove(state.selected.toList()) }
        }, applyInsets)
        rows = DownloadListAdapter(activity, state, ::clicked, ::held, actions::toggle)
        rows.attach(page.list)
        range = NativeRangeSelection(page.list, object : NativeRangeSelection.Actions {
            override fun selectable(position: Int) = state.editing && position in rows.visible.indices
            override fun selected(position: Int) = rows.selected(position)
            override fun select(position: Int, value: Boolean) { if (rows.select(position, value)) page.render(rows.visible.size) }
        })
        rows.beforeStructureChange = range::finish
        requests.restore(restored?.getBundle(REQUESTS))
        refresh()
    }
    private fun clicked(task: DownloadTask) {
        if (state.editing) { if (!state.selected.add(task.id)) state.selected.remove(task.id); refresh() }
        else if (task.state == DownloadState.COMPLETE) actions.open(task) else actions.toggle(task)
    }
    private fun held(task: DownloadTask, anchor: View) {
        if (state.editing) range.start(rows.visible.indexOfFirst { it.id == task.id }) else actions.menu(task, anchor)
    }
    private fun refresh() {
        if (closed) return
        val snapshot = queue.snapshot
        if (snapshot.isEmpty()) state.editing = false
        state.selected.retainAll(snapshot.map { it.id }.toSet())
        rows.submit(snapshot)
        if (!state.editing) range.finish()
        editingChanged(state.editing); page.render(rows.visible.size)
    }
    fun resume() {
        if (closed) return
        requests.onResume(); actions.onResume()
        if (!observing) { observing = true; queue.observe(observer) }
        refresh()
    }
    fun pause() {
        range.finish()
        if (observing) { queue.removeObserver(observer); observing = false }
    }
    fun covered() = range.finish()
    fun consumeBack(): Boolean {
        range.finish()
        if (!state.editing) return false
        state.editing = false; state.selected.clear(); refresh(); return true
    }
    fun save() = Bundle().apply {
        putBundle(STATE, state.save()); putBundle(REQUESTS, requests.save())
    }
    override fun close() {
        if (closed) return
        closed = true; pause(); range.close(); rows.close(); page.close(); requests.dispose(); actions.dispose()
    }
    companion object {
        private const val STATE = "download_list_state"
        private const val REQUESTS = "download_requests"
    }
}
