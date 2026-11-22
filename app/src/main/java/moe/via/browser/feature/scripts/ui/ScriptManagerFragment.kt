package moe.via.browser.feature.scripts.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.via.browser.R
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.feature.scripts.data.ScriptInstaller
import moe.via.browser.feature.scripts.data.ScriptUpdater
import moe.via.browser.feature.scripts.data.update.NativeScriptUpdatePreferences
import moe.via.browser.feature.scripts.model.UserScript
import moe.via.browser.feature.scripts.runtime.UserScriptEngine
import moe.via.browser.feature.scripts.ui.manager.ScriptManagerAdapter
import moe.via.browser.feature.scripts.ui.manager.ScriptManagerList
import moe.via.browser.storage.preferences.BrowserPreferences
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.documents.NativeDocumentPicker
import moe.via.browser.ui.pages.RecordPageScope
import moe.via.browser.ui.theme.ThemedActivity

class ScriptManagerFragment : ScriptRecordPage() {
    private val picker = NativeDocumentPicker(this)
    private val updatePreferences by lazy { NativeScriptUpdatePreferences(requireContext()) }
    private var page: PageScaffold? = null
    private var updateAction: View? = null
    private var list: ScriptManagerList? = null
    private var installer: ScriptInstallDialogs? = null
    private var scripts = emptyList<UserScript>()
    private var updating = false
    private var markConsumed = false
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val page = PageScaffold(activity, getString(R.string.ref_script_popup_title), ::closePage, appliesWindowInsets).also { this.page = it }
        val add = page.addIconAction(R.drawable.ref_ic_add, getString(R.string.ref_script_popup_add)) {}
        add.setOnClickListener { add(add) }; page.addTextAction(getString(R.string.ref_script_popup_update)) { update() }; updateAction = page.toolbar.getChildAt(page.toolbar.childCount - 1)
        val selected = arguments?.getInt("id", 0)?.takeIf { it != 0 }?.toLong()
        val content = ScriptManagerList(activity, object : ScriptManagerAdapter.Host {
            override fun global(enabled: Boolean) { BrowserPreferences(activity.getSharedPreferences("browser", Context.MODE_PRIVATE)).scriptsEnabled = enabled; present() }
            override fun interval() = this@ScriptManagerFragment.interval()
            override fun open(script: UserScript) = details(script)
            override fun menu(script: UserScript, anchor: View) = this@ScriptManagerFragment.menu(script, anchor)
            override fun toggle(script: UserScript, enabled: Boolean) {
                scripts = scripts.map { if (it.id == script.id) it.copy(enabled = enabled) else it }; present()
                database({ repository.enable(script.id, enabled) }) {}
            }
        }, selected, markConsumed).also { list = it }
        page.showContent(content.view, content.insetTarget); return page.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        installer = ScriptInstallDialogs(requireActivity() as ThemedActivity, repository, dialogs, { this.view != null }) { load() }
        load()
    }
    private fun load() { database({ repository.all() }) { scripts = it; present() } }
    private fun present() {
        if (scripts.isNotEmpty()) updateAction?.visibility = if (scripts.any { it.downloadUrl != null }) View.VISIBLE else View.GONE
        list?.submit(scripts, UserScriptEngine.get(requireContext()).enabled, intervalNames()[intervalIndex()])
    }
    private fun intervalIndex() = NativeScriptUpdatePreferences.INTERVALS.indexOfLast { updatePreferences.interval >= it }.coerceAtLeast(0)
    private fun intervalNames() = resources.getStringArray(R.array.ref_script_popup_intervals).toList()
    private fun interval() { dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_script_popup_auto_update)).singleChoice(intervalNames(), intervalIndex()) { updatePreferences.interval = NativeScriptUpdatePreferences.INTERVALS[it]; present() }) }
    private fun changedScript(id: Int) { database({ repository.get(id.toLong()) }) { script -> if (script != null) {
        val position = scripts.indexOfFirst { it.id == id.toLong() }
        if (position < 0) scripts = listOf(script) + scripts else scripts = scripts.toMutableList().apply { this[position] = script }
        present(); if (position < 0) list?.revealTop()
    } } }
    private fun editor(id: Int) {
        parentFragmentManager.setFragmentResultListener(ScriptEditorFragment.RESULT, this) { _, result ->
            if (result.containsKey("id")) changedScript(result.getInt("id")); parentFragmentManager.clearFragmentResultListener(ScriptEditorFragment.RESULT)
        }
        RecordPageScope.of(this)?.push(ScriptEditorFragment::class.java, Bundle().apply { putInt("id", id) })
    }
    private fun details(script: UserScript) {
        parentFragmentManager.setFragmentResultListener(ScriptDetailsFragment.RESULT, this) { _, result ->
            if (result.containsKey("id")) changedScript(result.getInt("id")); parentFragmentManager.clearFragmentResultListener(ScriptDetailsFragment.RESULT)
        }
        RecordPageScope.of(this)?.push(ScriptDetailsFragment::class.java, Bundle().apply { putInt("id", script.id.toInt()) })
    }
    private fun add(anchor: View) {
        dialogs.show(BrowserDialog(requireActivity()).items(listOf(getString(R.string.ref_script_page_add), getString(R.string.ref_script_page_download), getString(R.string.ref_script_page_import))) {
            when (it) {
                0 -> editor(-1)
                1 -> installer!!.download()
                2 -> picker.open(arrayOf("text/plain", "application/javascript", "text/javascript")) { uri -> if (uri != null) database({ requireContext().contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() } }) { installer?.review(it, null) } }
            }
        }.anchoredTo(anchor))
    }
    private fun menu(script: UserScript, anchor: View) {
        val actions = mutableListOf<Pair<String, () -> Unit>>(getString(R.string.ref_search_page_edit) to { details(script) })
        if (!script.downloadUrl.isNullOrEmpty()) actions += getString(R.string.ref_script_popup_update) to { installer!!.request(script.downloadUrl, targetId = script.id) }
        if (!script.homepageUrl.isNullOrEmpty()) actions += getString(R.string.ref_script_page_homepage) to { startActivity(Intent(requireActivity(), moe.via.browser.BrowserActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse(script.homepageUrl))) }
        actions += getString(R.string.ref_script_page_copy_link) to { requireContext().getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(null, script.downloadUrl.orEmpty())) }
        actions += getString(R.string.ref_search_page_delete) to { dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_search_page_delete), getString(R.string.ref_script_page_delete_message, script.name))
            .positive(getString(android.R.string.ok)) { database({ repository.remove(script.id) }) { load() }; true }.negative()) }
        dialogs.show(BrowserDialog(requireActivity()).items(actions.map { it.first }) { actions[it].second() }.anchoredTo(anchor))
    }
    private fun update() {
        if (updating) return
        val candidates = scripts.filter { it.enabled && (it.downloadUrl?.startsWith("http://", true) == true || it.downloadUrl?.startsWith("https://", true) == true) }
        if (candidates.isEmpty()) return
        val dialog = BrowserDialog(requireActivity(), getString(R.string.ref_script_popup_update), if (candidates.size == 1) getString(R.string.ref_script_page_update_one, candidates.single().name) else null)
        if (candidates.size > 1) dialog.multipleChoice(candidates.map(UserScript::name), candidates.indices.toSet())
        dialogs.show(dialog.negative().positive(getString(android.R.string.ok)) { current ->
            val selected = if (candidates.size == 1) candidates else current.selectedIndices.map(candidates::get)
            if (selected.isNotEmpty()) updateSelected(selected); true
        })
    }
    private fun updateSelected(scripts: List<UserScript>) {
        updating = true; message(getString(R.string.ref_script_update_started)); var remaining = scripts.size; var aborted = false
        val app = requireContext().applicationContext
        scripts.forEach { script -> ScriptUpdater.get(app).update(script) { result ->
            if (view == null || aborted) return@update
            if (result.isFailure) { aborted = true; NativeLog.warn(null, result.exceptionOrNull()!!); return@update }
            remaining--; if (remaining == 0) updating = false
            if (result.getOrThrow()) changedScript(script.id.toInt())
            if (remaining == 0) { message(getString(R.string.ref_script_update_completed)); ScriptInstaller.prepareAllDependencies(app) }
        } }
    }
    override fun onDestroyView() { list?.let { markConsumed = it.markConsumed; it.close() }; list = null; installer = null; page = null; updateAction = null; super.onDestroyView() }
}
