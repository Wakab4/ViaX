package moe.pure.browser.ui.tabs.closed

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.R
import moe.pure.browser.browser.ClosedTab
import moe.pure.browser.browser.TabRepository
import moe.pure.browser.browser.persistence.SessionTabPersistence
import moe.pure.browser.feature.library.ui.recycler.RecyclerScrollState
import moe.pure.browser.feature.settings.pages.SettingsRecordPage
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.message.BrowserMessages
import moe.pure.browser.ui.pages.RecordPageScope
import moe.pure.browser.ui.tasks.NativeViewJobs
import moe.pure.browser.ui.theme.ThemedActivity

/** c8.pc keeps pending IDs on the Fragment and publishes them unconditionally on destruction. */
class ClosedTabsFragment : SettingsRecordPage(), ClosedTabsPage.Callbacks {
    private val queued = ArrayList<String>()
    private var selected: String? = null
    private var records = mutableListOf<ClosedTab>()
    private var page: ClosedTabsPage? = null
    private var repository: TabRepository? = null
    private var jobs: NativeViewJobs? = null
    private var dialogs: DialogScope? = null
    private var restoredAnchor = false
    private var queryText = ""
    private val main = Handler(Looper.getMainLooper())
    private val search = Runnable { load(queryText) }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); repository = TabRepository(requireContext().applicationContext) }
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ClosedTabsPage(requireActivity() as ThemedActivity, appliesWindowInsets, this).also { page = it }.root
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState); jobs = NativeViewJobs(); dialogs = DialogScope()
        page!!.editor.setText(ClosedTabsProcessState.read().query)
        load("")
    }
    override fun query(value: String) { queryText = value; main.removeCallbacks(search); main.postDelayed(search, 100) }
    private fun load(value: String) {
        val data = repository!!
        jobs?.launch({ (if (value.isEmpty()) data.closed() else data.search(value, 1024)).map(SessionTabPersistence::closed).sortedByDescending { it.closedAt } }) { result ->
            records = result.toMutableList(); page?.render(records)
            if (!restoredAnchor) { restoredAnchor = true; page?.let { ClosedTabsProcessState.read().scroll.restore(it.list) } }
        }
    }
    override fun open(entry: ClosedTab) { if (entry.id !in queued) queued.add(entry.id); selected = entry.id; closePage() }
    override fun context(anchor: View, entry: ClosedTab) {
        val position = records.indexOf(entry)
        val actions = listOf(R.string.ref_closed_tabs_background, R.string.ref_closed_tabs_foreground, R.string.ref_closed_tabs_delete, R.string.ref_closed_tabs_copy_link, R.string.ref_closed_tabs_share).map(::getString)
        dialogs?.show(BrowserDialog(requireActivity()).items(actions) { action -> when (action) {
            0, 1 -> {
                queued.remove(entry.id); queued.add(entry.id)
                if (action == 0) BrowserMessages.show(requireActivity(), getString(R.string.ref_closed_tabs_opened_background)) else { selected = entry.id; closePage() }
            }
            2 -> { val data = repository!!; jobs?.launch({ data.delete(entry.id) }) { deleted -> if (deleted) { records.removeAt(position); page?.render(records, entry.id) } } }
            3 -> { requireActivity().getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(null, entry.tab.url)); BrowserMessages.show(requireActivity(), getString(R.string.ref_closed_tabs_copied)) }
            4 -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, entry.tab.url) }, null))
        } }.anchoredTo(anchor))
    }
    override fun clear() {
        dialogs?.show(BrowserDialog(requireActivity(), getString(R.string.ref_closed_tabs_clear), getString(R.string.ref_closed_tabs_confirm_clear))
            .negative(getString(android.R.string.cancel)).positive(getString(android.R.string.ok)) {
                val data = repository!!; jobs?.launch(data::clearClosed) { cleared -> if (cleared) load("") }; true
            })
    }
    override fun settings() { RecordPageScope.of(this)?.push(ClosedTabsSettingsFragment::class.java, null) }
    override fun close() = closePage()
    override fun onPause() { page?.let { ClosedTabsProcessState.save(ClosedTabsState(it.editor.text.toString(), RecyclerScrollState.capture(it.list))) }; super.onPause() }
    override fun onDestroyView() {
        main.removeCallbacksAndMessages(null); jobs?.close(); jobs = null; dialogs?.dispose(); dialogs = null; page?.close(); page = null; super.onDestroyView()
    }
    override fun onDestroy() {
        if (queued.isNotEmpty()) parentFragmentManager.setFragmentResult(ClosedTabsNavigation.RESULT, Bundle().apply {
            putStringArray("tabs", queued.toTypedArray()); putString("selected", selected)
        })
        repository?.close(); repository = null; super.onDestroy()
    }
}
