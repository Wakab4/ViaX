package moe.pure.browser.feature.settings.skin.ui

import android.net.Uri
import androidx.recyclerview.widget.LinearLayoutManager
import moe.pure.browser.R
import moe.pure.browser.core.app.NativeAppRestart
import moe.pure.browser.feature.settings.data.BrowserPreferences
import moe.pure.browser.feature.settings.skin.NativeSkinEntry
import moe.pure.browser.feature.settings.skin.NativeSkinRepository
import moe.pure.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.pure.browser.ui.components.recycler.NativeRecyclerView
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.message.BrowserMessages
import moe.pure.browser.ui.tasks.NativeViewJobs
import moe.pure.browser.ui.theme.ThemedActivity

internal class SkinSettingsController(private val activity: ThemedActivity, private val preferences: BrowserPreferences) {
    private val repository = NativeSkinRepository(activity)
    private val rows = ArrayList<NativeSkinEntry>()
    private var selected = preferences.skin
    private val jobs = NativeViewJobs()
    private val adapter = SkinSettingsAdapter(activity, rows, { selected }, ::select, ::held)
    val view = NativeRecyclerView(activity).apply {
        layoutManager = LinearLayoutManager(activity)
        clipToPadding = false
        NativeRecyclerEdgeEffect.install(this)
        adapter = this@SkinSettingsController.adapter
    }

    fun load() {
        rows.clear()
        rows.add(NativeSkinEntry("", activity.getString(R.string.ref_skin_default), 0))
        rows.addAll(repository.all())
        adapter.notifyDataSetChanged()
    }

    fun import(uri: Uri) {
        jobs.launch({ repository.import(uri) }) { entry ->
            if (entry == null) BrowserMessages.show(activity, activity.getString(R.string.ref_skin_import_failed))
            else {
                val position = index(entry.name)
                if (position > 0) adapter.notifyItemChanged(position)
                else { rows.add(1, entry); adapter.notifyItemInserted(1) }
            }
        }
    }

    private fun index(name: String) = rows.indexOfFirst { it.name == name }
    private fun select(entry: NativeSkinEntry, position: Int) {
        val previous = index(selected)
        if (previous == position) return
        changeSelection(entry.name)
        if (previous >= 0) adapter.notifyItemChanged(previous)
        adapter.notifyItemChanged(position)
    }

    private fun held(entry: NativeSkinEntry, position: Int): Boolean {
        if (position == 0) return false
        activity.dialogScope.show(BrowserDialog(activity, activity.getString(R.string.ref_skin_delete),
            activity.getString(R.string.ref_skin_delete_message, entry.label))
            .positive(activity.getString(android.R.string.ok)) {
                if (repository.delete(entry.name)) {
                    rows.removeAt(position); adapter.notifyItemRemoved(position)
                    if (selected == entry.name) { changeSelection(""); adapter.notifyItemChanged(0) }
                }
                true
            }.negative(activity.getString(android.R.string.cancel)))
        return true
    }

    private fun changeSelection(name: String) {
        selected = name
        preferences.skin = name
        activity.dialogScope.show(BrowserDialog(activity, activity.getString(R.string.ref_skin_changed),
            activity.getString(R.string.ref_skin_restart_message))
            .positive(activity.getString(R.string.ref_skin_restart)) { NativeAppRestart.restart(activity) }
            .negative(activity.getString(android.R.string.cancel)))
    }

    fun dispose() { jobs.close(); view.dispose(); view.adapter = null }
}
