package moe.via.browser.feature.site.ui.all

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.feature.library.ui.recycler.LibraryRecyclerView
import moe.via.browser.feature.site.NativeSiteKeys
import moe.via.browser.feature.site.data.SiteSettingsRepository
import moe.via.browser.feature.site.data.SiteRenameResult
import moe.via.browser.feature.site.ui.common.NativeSiteSettingsOwner
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.tasks.NativeViewJobs
import moe.via.browser.ui.theme.palette
import moe.via.browser.feature.site.ui.common.NativeSiteKeys as DisplayKeys

/** w6 owns raw keys and renames; it does not create a configuration until its editor commits. */
class AllSitesController(private val activity: Activity, private val repository: SiteSettingsRepository,
    private val open: (String) -> Unit) : NativeSiteSettingsOwner {
    private val adapter = AllSitesAdapter(activity, open, ::contextMenu)
    override val list = LibraryRecyclerView(activity).apply { id = R.id.site_page_list; adapter = this@AllSitesController.adapter }
    private val empty = NativeTypography.text(activity).apply {
        text = "¯\\_(ツ)_/¯"; textSize = 22f; gravity = Gravity.CENTER; setTextColor(activity.palette.secondary)
        setPadding(0, dp(20), 0, dp(20))
    }
    override val view = FrameLayout(activity).apply {
        addView(list, FrameLayout.LayoutParams(-1, -1)); addView(empty, FrameLayout.LayoutParams(-1, -1))
    }
    override val title: CharSequence get() = activity.getString(R.string.ref_site_all_title)
    private val scope = DialogScope()
    private val messages = BrowserMessage(activity)
    private var popup: BrowserDialog? = null
    private var closed = false
    private val jobs = NativeViewJobs()

    override fun render() {
        if (closed) return
        jobs.launch({ repository.keys().sortedWith(DisplayKeys.order) }) { keys ->
            adapter.submit(keys)
            empty.visibility = if (keys.isEmpty()) View.VISIBLE else View.GONE
            list.visibility = if (keys.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    fun add() = input(activity.getString(R.string.ref_site_all_add), "", allowEmptyKey = true) { key -> open(key) }
    private fun rename(key: String) = input(activity.getString(R.string.ref_site_all_edit), key) { target ->
        if (target == key) return@input
        when (repository.rename(key, target)) {
            SiteRenameResult.RENAMED -> render()
            SiteRenameResult.UNCHANGED, SiteRenameResult.INVALID -> Unit
            else -> messages.show(activity.getString(R.string.ref_site_all_conflict, target))
        }
    }
    private fun input(title: String, original: String, allowEmptyKey: Boolean = false, accepted: (String) -> Unit) {
        if (closed) return
        val dialog = BrowserDialog(activity, title)
        dialog.setCanceledOnTouchOutside(false)
        val field = dialog.field(original, "www.example.com").apply { inputType = InputType.TYPE_CLASS_TEXT }
        show(dialog.positive(activity.getString(android.R.string.ok)) {
            val raw = field.text.toString()
            if (raw.isNotEmpty()) {
                val key = NativeSiteKeys.input(raw)
                if (allowEmptyKey || key.isNotEmpty()) accepted(key)
            }
            true
        }.negative(activity.getString(android.R.string.cancel)))
    }
    private fun contextMenu(anchor: View, key: String) {
        show(BrowserDialog(activity).items(listOf(activity.getString(R.string.ref_site_all_edit),
            activity.getString(R.string.ref_site_delete))) { choice ->
            if (!closed) if (choice == 0) rename(key) else { repository.remove(key); render() }
        }.anchoredTo(anchor))
    }
    private fun show(dialog: BrowserDialog) {
        popup?.dismiss(); popup = dialog
        scope.show(dialog) { if (popup === dialog) popup = null }
    }
    override fun back(): Boolean { val dialog = popup ?: return false; dialog.dismiss(); return true }
    override fun close() {
        if (closed) return
        closed = true; jobs.close(); scope.dispose(); messages.close(); list.adapter = null; list.dispose()
    }
}
