package moe.via.browser.feature.sync.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import moe.via.browser.R
import moe.via.browser.feature.sync.data.SyncRepository
import moe.via.browser.feature.sync.model.SyncCall
import moe.via.browser.feature.sync.sync.SyncManager
import moe.via.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.via.browser.ui.components.recycler.NativeRecyclerView
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.tasks.NativeViewJobs

abstract class SyncChildFragment : Fragment() {
    protected val repository by lazy { SyncRepository(requireContext().applicationContext) }
    private val managerDelegate = lazy { SyncManager(requireContext().applicationContext) }
    protected val manager by managerDelegate
    protected var list: NativeRecyclerView? = null
    private var jobs: NativeViewJobs? = null
    private var scope: DialogScope? = null
    protected val dialogs get() = requireNotNull(scope)
    private val calls = HashSet<SyncCall>()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        NativeRecyclerView(requireContext()).apply {
            id = R.id.settings_page_list; layoutManager = LinearLayoutManager(context); clipToPadding = false
            NativeRecyclerEdgeEffect.install(this)
        }.also { list = it }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState); jobs = NativeViewJobs(); scope = DialogScope()
        (parentFragment as SyncSettingsFragment).bottom(requireNotNull(list))
    }
    protected fun <T> local(work: () -> T, result: (T) -> Unit) { jobs?.launch(work, delivered = result) }
    protected fun <T> request(start: ((T) -> Unit) -> SyncCall, result: (T) -> Unit) {
        var call: SyncCall? = null
        var completed = false
        val created = start { value ->
            completed = true; call?.let { calls.remove(it) }
            if (scope != null) result(value)
        }
        call = created
        if (!completed) calls.add(created)
    }
    protected fun message(id: Int) = BrowserMessages.show(requireActivity(), getString(id))
    protected fun confirm(title: Int, text: Int, action: () -> Unit) {
        dialogs.show(BrowserDialog(requireActivity(), getString(title), getString(text))
            .negative(getString(android.R.string.cancel)).positive(getString(android.R.string.ok)) { action(); true })
    }
    internal fun scrollToTop() { list?.smoothScrollToPosition(0) }
    override fun onDestroyView() {
        val currentScope = scope; scope = null
        val pending = calls.toList(); calls.clear(); pending.forEach(SyncCall::cancel); jobs?.close(); jobs = null
        currentScope?.dispose(); list?.dispose(); list?.adapter = null; list = null; super.onDestroyView()
    }
    override fun onDestroy() { if (managerDelegate.isInitialized()) manager.close(); super.onDestroy() }
}
