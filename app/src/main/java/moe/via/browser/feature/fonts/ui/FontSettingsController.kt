package moe.via.browser.feature.fonts.ui

import android.graphics.Typeface
import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import moe.via.browser.ui.components.recycler.NativeRecyclerView
import moe.via.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.via.browser.R
import moe.via.browser.storage.font.FontEntry
import moe.via.browser.feature.fonts.FontPreviewCache
import moe.via.browser.feature.fonts.NativeFontImport
import moe.via.browser.storage.font.NativeFontStore
import moe.via.browser.storage.preferences.BrowserPreferences
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.tasks.NativeViewJobs

/** hb.j2 keeps page previews, selection and list publication apart from the process font store. */
internal class FontSettingsController(private val activity: ThemedActivity, private val preferences: BrowserPreferences,
    private val previews: FontPreviewCache) {
    private val fonts = NativeFontStore.get(activity)
    private val importer = NativeFontImport(activity, fonts)
    private val items = ArrayList<FontEntry>()
    private var selected = ""
    private val jobs = NativeViewJobs()
    private val adapter: FontListAdapter = FontListAdapter(activity, previews, { if (select(it)) render() }, ::menu)
    val view = NativeRecyclerView(activity).apply {
        layoutManager = LinearLayoutManager(activity)
        clipToPadding = false
        NativeRecyclerEdgeEffect.install(this)
        this.adapter = this@FontSettingsController.adapter
    }
    fun render(): Unit = adapter.submit(items, selected)
    fun load() {
        jobs.launch(fonts::all) { loaded ->
            selected = preferences.uiFont
            items.clear(); items.addAll(loaded); render()
        }
    }
    fun import(uri: Uri) {
        jobs.launch({ importer.import(uri) }) { result ->
            val entry = result.entry
            if (entry == null) BrowserMessages.show(activity, activity.getString(R.string.ref_font_import_error, result.code))
            else {
                previews.invalidate(entry)
                if (selected == entry.name && previews.typeface(entry) === Typeface.DEFAULT && items.isNotEmpty()) select(items[0])
                load()
            }
        }
    }
    private fun select(entry: FontEntry): Boolean {
        if (entry.name == selected || previews.invalid(entry)) return false
        selected = entry.name
        preferences.uiFont = selected
        return true
    }
    private fun menu(entry: FontEntry, anchor: View) {
        val position = items.indexOf(entry)
        if (position <= 0) return
        val choices = buildList<Pair<String, () -> Unit>> {
            if (!previews.invalid(entry)) add(activity.getString(R.string.ref_font_edit) to { rename(position) })
            add(activity.getString(R.string.ref_font_delete) to { delete(position) })
        }
        activity.dialogScope.show(BrowserDialog(activity).items(choices.map { it.first }) { choices[it].second() }.anchoredTo(anchor))
    }
    private fun delete(position: Int) {
        if (position !in items.indices) return
        val entry = items.removeAt(position)
        if (entry.name == selected) select(items[0])
        render()
        fonts.delete(entry.name)
    }
    private fun rename(position: Int) {
        val entry = items.getOrNull(position) ?: return
        val dot = entry.name.lastIndexOf('.')
        val stem = if (dot > 0) entry.name.substring(0, dot) else entry.name
        val extension = if (dot > 0) entry.name.substring(dot + 1) else ""
        val dialog = BrowserDialog(activity, activity.getString(R.string.ref_font_rename))
        val field = dialog.field(stem, stem)
        activity.dialogScope.show(dialog.positive(activity.getString(android.R.string.ok)) {
            val text = field.text.toString()
            if (text.isNotEmpty() && !stem.equals(text, true)) {
                val wasSelected = entry.name == selected
                val replacement = fonts.rename(entry.name, text + if (extension.isEmpty()) "" else ".$extension")
                if (replacement != null) {
                    items[position] = replacement
                    if (wasSelected) select(replacement)
                    render()
                }
            }
            true
        }.negative(activity.getString(android.R.string.cancel)))
    }
    fun dispose() { jobs.close(); view.dispose(); view.adapter = null }
}
