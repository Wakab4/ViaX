package moe.via.browser.feature.library.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import moe.via.browser.feature.library.LibraryTab
import moe.via.browser.feature.library.ui.recycler.LibraryProcessState
import moe.via.browser.ui.components.recycler.RecyclerScrollState
import moe.via.browser.ui.tasks.NativeViewJobs

abstract class LibraryChildFragment : Fragment(), LibraryPageView.Actions {
    protected abstract val tab: LibraryTab
    private var jobs: NativeViewJobs? = null
    protected val parent get() = parentFragment as LibraryFragment
    internal val model get() = parent.libraryModel
    protected val repository get() = model.repository
    protected var page: LibraryPageView? = null
    protected var query = ""
    private var settingQuery = false
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        LibraryPageView(requireActivity(), tab, this).also { page = it }.root
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState); jobs = NativeViewJobs()
        query = LibraryProcessState.read(tab)?.query.orEmpty(); setQueryText(query)
    }
    protected fun setQueryText(value: String) { settingQuery = true; query = value; page?.search?.setText(value); settingQuery = false }
    final override fun queryChanged(value: String) { if (!settingQuery) { query = value; search(value) } }
    protected abstract fun search(value: String)
    protected fun <T> work(action: () -> T, result: (T) -> Unit) { jobs?.launch(action, delivered = result) }
    internal open fun back(): Boolean = if (query.isNotEmpty()) { setQueryText(""); search(""); true } else false
    internal open fun canSwipeBack() = query.isEmpty()
    internal fun scrollToTop() { page?.list?.smoothScrollToPosition(0) }
    protected open fun folderState() = ""
    internal open fun pagingAllowed() = true
    override fun onResume() { super.onResume(); parent.paging(this, pagingAllowed()) }
    override fun onPause() {
        page?.let { LibraryProcessState.save(tab, folderState(), query, RecyclerScrollState.capture(it.list), it.list.scrollY) }
        super.onPause()
    }
    override fun onDestroyView() { jobs?.close(); jobs = null; page?.close(); page = null; super.onDestroyView() }
}

