package moe.via.browser.feature.library.pages

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import moe.via.browser.R
import moe.via.browser.browser.ui.host.BrowserRootFragment
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.feature.library.LibraryTab
import moe.via.browser.feature.library.navigation.LibraryOpenResult
import moe.via.browser.feature.library.navigation.LibraryOpenWhere
import moe.via.browser.ui.components.NativeToolbarTap
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.pages.RecordPageFragment
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.browser.ui.tabs.closed.ClosedTabsNavigation
import moe.via.browser.ui.theme.palette

/** bb.i owns the pager and shared open queue. */
class LibraryFragment : RecordPageFragment() {
    internal lateinit var libraryModel: LibraryModel
        private set
    private val model get() = libraryModel
    private var pager: ViewPager2? = null
    private var page: PageScaffold? = null
    private val tabs = ArrayList<TextView>()
    private var softInput = 0
    private var forceClose = false
    private var resultDelivered = false
    override fun onAttach(context: Context) {
        super.onAttach(context); val window = requireActivity().window
        softInput = window.attributes.softInputMode; window.setSoftInputMode(48)
    }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); libraryModel = ViewModelProvider(this)[LibraryModel::class.java] }
    override fun draggable() = arguments?.getBoolean("draggable", true) != false
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val pages = ViewPager2(activity).apply {
            id = R.id.library_host_pager; orientation = ViewPager2.ORIENTATION_HORIZONTAL
            adapter = object : FragmentStateAdapter(this@LibraryFragment) {
                override fun getItemCount() = 3
                override fun createFragment(position: Int): Fragment = when (position) { 0 -> BookmarksPageFragment(); 1 -> HistoryPageFragment(); else -> OfflinePageFragment() }
            }
        }
        pager = pages
        return PageScaffold(activity, "", ::exit, appliesWindowInsets).also { scaffold ->
            page = scaffold; scaffold.titleView.visibility = View.GONE
            val strip = LinearLayout(activity).apply { id = R.id.library_host_tabs; gravity = Gravity.CENTER_VERTICAL }
            val labels = intArrayOf(R.string.ref_library_host_bookmarks, R.string.ref_library_host_history, R.string.ref_library_host_offline)
            tabs.clear()
            labels.forEachIndexed { index, label ->
                val title = NativeTypography.text(activity).apply {
                    setText(label); setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.page_info_title_size).toFloat())
                    gravity = Gravity.CENTER; setPadding(dp(12), 0, dp(12), 0); background = activity.touchFeedback()
                    setOnClickListener { pages.setCurrentItem(index, true) }
                }
                tabs.add(title); strip.addView(title, LinearLayout.LayoutParams(-2, -1))
            }
            scaffold.toolbar.addView(strip, 1, LinearLayout.LayoutParams(-2, -1)); scaffold.showContent(pages, bottomInsets = pages)
            NativeToolbarTap.bind(scaffold.toolbar) { current()?.scrollToTop() }
            pages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) { selection(position) }
            })
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val requested = arguments?.getInt("page", 0) ?: 0
        if (requested in 1..3) pager!!.setCurrentItem(requested - 1, false)
        selection(pager!!.currentItem)
        model.events.observe(viewLifecycleOwner) { event -> if (event != null) {
            model.events.value = null
            when (event) {
                LibraryModel.Event.CLOSE -> exit()
                LibraryModel.Event.BACKGROUND -> BrowserMessages.show(requireActivity(), getString(R.string.ref_library_opened_background))
                LibraryModel.Event.RESTORE_TABS -> ClosedTabsNavigation.open(this) { result ->
                    result.getStringArray("tabs")?.takeIf { it.isNotEmpty() }?.let { ids ->
                        model.open(ClosedTabsNavigation.restoreLink(ids, result.getString("selected")), LibraryOpenWhere.CURRENT)
                    }
                }
            }
        } }
        model.paging.observe(viewLifecycleOwner) { enabled -> pager?.isUserInputEnabled = enabled; dragLayout?.gestures(draggable() && enabled && pager?.currentItem == 0) }
    }
    private fun selection(position: Int) {
        tabs.forEachIndexed { index, title -> title.setTextColor(if (index == position) requireContext().palette.text else 0xFFAAAAAA.toInt()) }
        dragLayout?.gestures(draggable() && model.paging.value != false && position == 0)
        current()?.let { model.paging.value = it.pagingAllowed() }
    }
    private fun current() = pager?.let { childFragmentManager.findFragmentByTag("f${it.currentItem}") as? LibraryChildFragment }
    internal fun paging(owner: LibraryChildFragment, enabled: Boolean) { if (current() === owner) model.paging.value = enabled }
    internal override fun canNavigateBack() = current()?.canSwipeBack() != false
    internal override fun consumeBack() = !forceClose && current()?.back() == true
    private fun exit() {
        forceClose = true
        closePage()
        deliverResult()
        forceClose = false
    }
    private fun deliverResult() {
        if (resultDelivered) return
        resultDelivered = true
        val requests = model.queue.snapshot()
        if (requests.isNotEmpty()) BrowserRootFragment.find(requireActivity())?.libraryResult(LibraryOpenResult.write(requests))
    }
    override fun onDestroyView() {
        pager?.adapter = null; pager = null; page = null; tabs.clear()
        if (softInput != 0) requireActivity().window.setSoftInputMode(softInput)
        super.onDestroyView()
    }
    override fun onDestroy() {
        deliverResult()
        super.onDestroy()
    }
    companion object {
        internal fun args(tab: LibraryTab) = Bundle().apply { putBoolean("draggable", true); putInt("page", tab.ordinal + 1) }
    }
}
