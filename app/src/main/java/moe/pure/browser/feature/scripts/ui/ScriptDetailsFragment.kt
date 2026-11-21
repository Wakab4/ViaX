package moe.pure.browser.feature.scripts.ui

import android.os.Bundle
import android.text.format.DateUtils
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import moe.pure.browser.R
import moe.pure.browser.feature.scripts.model.ScriptDetailsModel
import moe.pure.browser.feature.scripts.model.ScriptScope
import moe.pure.browser.feature.scripts.model.ScriptDetailsMetadata
import moe.pure.browser.feature.scripts.ui.details.ScriptDetailsAdapter
import moe.pure.browser.feature.scripts.ui.details.ScriptDetailsItem
import moe.pure.browser.feature.scripts.ui.details.ScriptResourceLocator
import moe.pure.browser.feature.scripts.ui.resource.ScriptResourceDialogs
import moe.pure.browser.feature.scripts.ui.resource.ScriptResourceViewerFragment
import moe.pure.browser.ui.components.NativeToolbarTap
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.pure.browser.ui.components.recycler.NativeRecyclerView
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.pages.RecordPageScope
import moe.pure.browser.ui.theme.ThemedActivity
import java.util.Locale

/** ta.u's installed-script record page; source changes are forwarded as editor_result. */
class ScriptDetailsFragment : ScriptRecordPage() {
    private lateinit var model: ScriptDetailsModel
    private var page: PageScaffold? = null
    private var list: NativeRecyclerView? = null
    private var adapter: ScriptDetailsAdapter? = null
    private var resourcesUi: ScriptResourceDialogs? = null
    private var located = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ScriptDetailsModel(repository) as T
        })[ScriptDetailsModel::class.java]
    }
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val page = PageScaffold(activity, getString(R.string.ref_search_page_edit), ::closePage, appliesWindowInsets).also { this.page = it }
        adapter = ScriptDetailsAdapter(activity, ::activate)
        val recycler = NativeRecyclerView(activity).apply {
            id = R.id.settings_page_list; layoutManager = LinearLayoutManager(activity); clipToPadding = false
            NativeRecyclerEdgeEffect.install(this); adapter = this@ScriptDetailsFragment.adapter
        }.also { list = it }
        page.showContent(recycler, bottomInsets = recycler); NativeToolbarTap.bind(page.toolbar) { recycler.smoothScrollToPosition(0) }
        return page.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val id = arguments?.getInt("id", -1) ?: -1
        val key = arguments?.getString("script_id")
        if (id == -1 && key.isNullOrEmpty()) return
        resourcesUi = ScriptResourceDialogs(requireActivity() as ThemedActivity, { viewLifecycleOwner.lifecycle }, model::downloadResource,
            { adapter?.changed(it) }, { adapter?.downloaded(it) }) { path -> ScriptResourceViewerFragment.open(this, path) }
        model.changed.observe(viewLifecycleOwner) { render(model.metadata) }
        if (key != null) model.load(key) else model.load(id)
    }
    private fun render(script: ScriptDetailsMetadata?) {
        val rows = ArrayList<ScriptDetailsItem>()
        if (script != null) {
            rows.add(ScriptDetailsItem.Setting(5, script.name, script.version))
            rows.add(ScriptDetailsItem.Setting(3, getString(R.string.ref_script_page_run_at), runName(model.runAt).ifEmpty(::defaultRunName)))
        }
        val scopes = model.scopes()
        listOf(true, false).forEach { matching ->
            rows.add(ScriptDetailsItem.Section(getString(if (matching) R.string.ref_script_page_matches else R.string.ref_script_page_excludes)))
            scopes.filter { it.match == matching }.forEach { rows.add(ScriptDetailsItem.Scope(it)) }
            rows.add(ScriptDetailsItem.Add(matching, getString(if (matching) R.string.ref_script_page_add_match else R.string.ref_script_page_add_exclude)))
        }
        listOf(R.string.ref_script_page_dependencies to model.dependencies, R.string.ref_script_page_resources to model.resources).forEach { (title, values) ->
            if (values.isNotEmpty()) {
                rows.add(ScriptDetailsItem.Section(getString(title)))
                values.forEach { rows.add(ScriptDetailsItem.Resource(it)) }
            }
        }
        rows.add(ScriptDetailsItem.Section(getString(R.string.ref_script_page_advanced)))
        rows.add(ScriptDetailsItem.Setting(4, getString(R.string.ref_script_page_source)))
        rows.add(ScriptDetailsItem.Setting(6, getString(R.string.ref_script_page_reset)))
        adapter?.submit(rows)
        if (!located) {
            located = true
            if (arguments?.getBoolean("view_dependencies", false) == true) {
                val position = adapter!!.firstMissingResource()
                if (position >= 0) list!!.post { list?.let { ScriptResourceLocator.scroll(it, position) } }
            }
        }
    }
    private fun activate(row: ScriptDetailsItem, position: Int) {
        when (row) {
            is ScriptDetailsItem.Setting -> when (row.id) {
                5 -> model.metadata?.let(::information)
                3 -> timing(defaultRunName())
                4 -> editor()
                6 -> dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_script_page_reset), getString(R.string.ref_script_page_reset_message)).negative()
                    .positive(getString(android.R.string.ok)) { model.reset(); true })
            }
            is ScriptDetailsItem.Scope -> scope(row.value)
            is ScriptDetailsItem.Add -> addScope(row.match)
            is ScriptDetailsItem.Resource -> resourcesUi!!.show(row.value, position)
            is ScriptDetailsItem.Section -> Unit
        }
    }
    private fun defaultRunName(): String {
        val base = runName(model.defaultRunAt)
        return if (base.isEmpty()) getString(R.string.ref_script_page_default) else getString(R.string.ref_script_page_default_format, base)
    }
    private fun timing(default: String) {
        val times = listOf(0, 1, 2, 4)
        val index = times.indexOf(model.runAt).let { if (it < 0) 0 else it }
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_script_page_run_at)).singleChoice(listOf(default, "document-start", "document-end", "document-idle"), index) {
            model.selectRunAt(times[it])
        })
    }
    private fun scope(entry: ScriptScope) {
        val dialog = BrowserDialog(requireActivity(), getString(R.string.ref_search_page_edit), entry.pattern.takeUnless { entry.editable })
        val input = if (entry.editable) dialog.field(entry.pattern, getString(if (entry.match) R.string.ref_script_page_matches else R.string.ref_script_page_excludes)) else null
        dialog.neutral(getString(R.string.ref_search_page_delete)) { model.remove(entry); true }
        if (input != null) dialog.negative()
        dialogs.show(dialog.positive(getString(android.R.string.ok)) { if (input != null) model.replace(entry, input.text.toString()); true })
    }
    private fun addScope(matching: Boolean) {
        val title = getString(if (matching) R.string.ref_script_page_add_match else R.string.ref_script_page_add_exclude)
        val dialog = BrowserDialog(requireActivity(), title); val field = dialog.field("https://*/*", getString(if (matching) R.string.ref_script_page_matches else R.string.ref_script_page_excludes))
        dialogs.show(dialog.negative().positive(getString(android.R.string.ok)) { model.add(field.text.toString(), matching); true })
    }
    private fun information(script: ScriptDetailsMetadata) {
        fun date(value: Long) = if (((System.currentTimeMillis() - value) / 1000 / 60).toInt() < 1) getString(R.string.ref_script_page_just_now)
            else DateUtils.getRelativeTimeSpanString(value, System.currentTimeMillis(), 60000, 65552).toString()
        val values = listOfNotNull(R.string.ref_script_page_info_name to script.name, R.string.ref_script_page_info_version to script.version,
            script.homepageUrl?.takeIf(String::isNotEmpty)?.let { R.string.ref_script_page_info_homepage to it }, script.supportUrl?.takeIf(String::isNotEmpty)?.let { R.string.ref_script_page_info_support to it },
            script.downloadUrl?.takeIf(String::isNotEmpty)?.let { R.string.ref_script_page_info_download to it }, R.string.ref_script_page_info_updated to date(script.updated),
            R.string.ref_script_page_info_created to date(script.created), model.sourceLength.takeIf { it != 0 }?.let { R.string.ref_script_page_info_size to size(it) })
        val html = values.joinToString("<br />") { (label, text) -> "<b>${getString(label)}</b><br />${text.orEmpty()}" }
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_script_page_information), Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)).positive(getString(android.R.string.ok)))
    }
    private fun size(length: Int): String {
        var value = length.toDouble()
        val unit = when { value >= 8.589934592E8 -> { value /= 1024.0 * 1024 * 1024; "GB" }; value >= 838860.8 -> { value /= 1024.0 * 1024; "MB" }; value >= 819.2 -> { value /= 1024.0; "KB" }; else -> "B" }
        return String.format(Locale.ROOT, "%.1f %s", value, unit)
    }
    private fun editor() {
        val id = model.metadata?.id ?: return
        parentFragmentManager.setFragmentResultListener(ScriptEditorFragment.RESULT, this) { _, result ->
            if (result.containsKey("id")) {
                val editedId = result.getInt("id")
                model.load(editedId)
                parentFragmentManager.setFragmentResult(RESULT, Bundle().apply { putInt("id", editedId) })
            }
            parentFragmentManager.clearFragmentResultListener(ScriptEditorFragment.RESULT)
        }
        RecordPageScope.of(this)?.push(ScriptEditorFragment::class.java, Bundle().apply { putInt("id", id) })
    }
    private fun runName(value: Int) = when (value) { 1 -> "document-start"; 2 -> "document-end"; 4 -> "document-idle"; 8 -> "context-menu"; else -> "" }
    override fun onDestroyView() { resourcesUi?.close(); resourcesUi = null; list?.dispose(); list?.adapter = null; list = null; adapter = null; page = null; super.onDestroyView() }
    companion object { const val RESULT = "editor_result" }
}
