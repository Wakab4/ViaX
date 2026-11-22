package moe.via.browser.feature.useragent.ui

import android.app.Activity
import moe.via.browser.core.logging.NativeLog as Log
import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import moe.via.browser.R
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.ui.components.recycler.NativeRecyclerView
import moe.via.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.via.browser.feature.site.ui.common.NativeSiteAdapter
import moe.via.browser.feature.site.ui.common.NativeSiteRow
import moe.via.browser.feature.useragent.data.UserAgent
import moe.via.browser.feature.useragent.UserAgentPreferences
import moe.via.browser.feature.useragent.data.UserAgentRepository
import moe.via.browser.feature.useragent.data.UserAgentOperations
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import java.lang.ref.WeakReference
import java.util.concurrent.Future

class GlobalUserAgentController(private val activity: Activity, private val repository: UserAgentRepository,
    private val settings: BrowserSettings, private val edit: (Int) -> Unit) : AutoCloseable {
    private val choices = repository.all().toMutableList()
    private var selectedId = settings.userAgentId
    private val preferences = UserAgentPreferences(settings)
    private val radios = UserAgentRadioAdapter(activity, ::select, ::menu)
    private val footer = NativeSiteAdapter(activity, ::footerClicked)
    private val scope = DialogScope()
    private var popup: BrowserDialog? = null
    private val tasks = mutableListOf<Future<*>>()
    private var closed = false
    val title get() = activity.getString(R.string.ref_ua_title)
    val view = NativeRecyclerView(activity).apply {
        id = R.id.user_agent_list
        layoutManager = LinearLayoutManager(activity)
        NativeRecyclerEdgeEffect.install(this)
        adapter = ConcatAdapter(radios, footer)
        clipToPadding = false
    }
    init { render() }
    fun render() {
        if (closed) return
        radios.submit(choices, selectedId)
        val desktop = choices.firstOrNull { it.id == settings.preferences.desktopUserAgentId }
        footer.submit(listOf(NativeSiteRow.Section(0, activity.getString(R.string.ref_ua_advanced)),
            NativeSiteRow.Link(1, activity.getString(R.string.ref_ua_desktop_title), desktop?.let { UserAgentLabels.label(activity, it) }),
            NativeSiteRow.Toggle(2, activity.getString(R.string.ref_ua_reduction_title), settings.preferences.simplifyUserAgent,
                detail = activity.getString(R.string.ref_ua_reduction_detail))))
    }
    fun create() { if (!closed) edit(0) }
    private fun select(position: Int) {
        val agent = choices.getOrNull(position) ?: return
        if (closed || agent.id == selectedId) return
        selectedId = agent.id; preferences.global(agent); render()
    }
    private fun footerClicked(row: NativeSiteRow) {
        if (closed) return
        if (row is NativeSiteRow.Toggle) { preferences.reduction(!row.checked); render() }
        else if (row is NativeSiteRow.Link) {
            val values = choices.filter { it.id >= 0 || it.id in -5..-3 }
            show(BrowserDialog(activity, activity.getString(R.string.ref_ua_desktop_title)).singleChoice(
                values.map { UserAgentLabels.label(activity, it) },
                values.indexOfFirst { it.id == settings.preferences.desktopUserAgentId }.coerceAtLeast(0)) {
                preferences.desktop(values[it]); render()
            })
            preferences.policyChanged()
        }
    }
    private fun menu(anchor: View, position: Int, agent: UserAgent) {
        if (closed) return
        show(BrowserDialog(activity).items(listOf(activity.getString(R.string.ref_ua_edit), activity.getString(R.string.ref_ua_delete))) {
            if (!closed) if (it == 0) edit(agent.id) else {
                if (selectedId == agent.id) { choices.firstOrNull { choice -> choice.id == 0 }?.let(preferences::global); selectedId = 0 }
                repository.delete(agent.id)
                // Native captures both the original ID and position for this popup.
                if (position in choices.indices) choices.removeAt(position)
                render()
            }
        }.anchoredTo(anchor))
    }
    fun edited(id: Int) {
        if (closed || id <= 0) return
        val position = choices.indexOfFirst { it.id == id }
        val reference = WeakReference(this)
        tasks.removeAll { it.isDone || it.isCancelled }
        tasks.add(UserAgentOperations.run(activity.applicationContext, { it.get(id) }) { result ->
            reference.get()?.loaded(position, id, result)
        })
    }
    private fun loaded(position: Int, id: Int, result: Result<UserAgent?>) {
        if (closed) return
        result.onFailure { Log.warn("UserAgents", "Unable to load saved user-agent", it) }
        val value = result.getOrNull() ?: return
        if (position < 0) choices.add(value) else if (position in choices.indices) choices[position] = value
        if (id == selectedId) preferences.global(value)
        if (id == settings.preferences.desktopUserAgentId) preferences.desktop(value)
        render()
        if (position < 0) view.scrollToPosition(choices.size)
    }
    private fun show(dialog: BrowserDialog) {
        popup?.dismiss(); popup = dialog
        scope.show(dialog) { if (popup === dialog) popup = null }
    }
    fun back(): Boolean { val dialog = popup ?: return false; dialog.dismiss(); return true }
    override fun close() {
        if (closed) return
        closed = true; tasks.forEach { it.cancel(false) }; tasks.clear(); scope.dispose()
        view.adapter = null; view.dispose(); footer.close(); choices.clear()
    }
}
