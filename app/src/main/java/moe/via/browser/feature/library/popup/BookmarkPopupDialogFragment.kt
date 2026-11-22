package moe.via.browser.feature.library.popup

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import moe.via.browser.R
import moe.via.browser.feature.library.popup.ui.BookmarkPopupEditorPage
import moe.via.browser.feature.library.popup.ui.BookmarkPopupFoldersPage
import moe.via.browser.ui.dialog.SettingsWindowEffects
import moe.via.browser.ui.components.dp

/** b8.n/k8.a: a restored shell waits for the browser's live-page gate before loading data. */
class BookmarkPopupDialogFragment : DialogFragment() {
    interface Host { fun completed(event: BookmarkPopupEvent) }
    private var host: Host? = null
    private var pager: ViewPager2? = null
    private var savedModel: Bundle? = null
    private var effects: AutoCloseable? = null
    private val model get() = ViewModelProvider(this)[BookmarkPopupModel::class.java]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_ViaBrowser_Dialog)
        savedModel = savedInstanceState?.getBundle(STATE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ViewPager2(requireContext()).apply {
            id = R.id.bookmark_popup_pager
            layoutParams = ViewGroup.LayoutParams(-1, requireContext().dp(300))
            isUserInputEnabled = false
            offscreenPageLimit = 1
            adapter = object : FragmentStateAdapter(this@BookmarkPopupDialogFragment) {
                override fun getItemCount() = 2
                override fun createFragment(position: Int): Fragment =
                    if (position == 0) BookmarkPopupEditorPage() else BookmarkPopupFoldersPage()
            }
        }.also { pager = it }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.state.observe(viewLifecycleOwner) { state ->
            pager?.let { pages ->
                if (state.height > 0 && pages.layoutParams.height != state.height) {
                    pages.layoutParams = pages.layoutParams.apply { height = state.height }
                }
                if (pages.currentItem != state.page) pages.setCurrentItem(state.page, true)
            }
            dispatch()
        }
        if (host != null) initialize()
    }

    private fun initialize() {
        val args = arguments ?: return
        model.initialize(BookmarkPopupRequest(args.getString(ID), args.getString(URL), args.getString(TITLE),
            args.getInt(POSITION, -1)), savedModel)
        savedModel = null
    }

    internal fun bind(value: Host) { host = value; if (view != null) { initialize(); dispatch() } }
    internal fun release(value: Host) { if (host === value) host = null }

    private fun dispatch() {
        if (!isAdded || parentFragmentManager.isStateSaved) return
        val currentHost = host ?: return
        val event = model.state.value?.event ?: return
        model.consumeEvent(event.id)
        if (event.kind == BookmarkPopupEventKind.FAILED) return
        currentHost.completed(event)
        if (event.kind != BookmarkPopupEventKind.CLOSE) parentFragmentManager.setFragmentResult(RESULT, Bundle().apply {
            putString("id", event.entry?.id); putInt("position", event.position)
        })
        dismiss()
    }

    override fun onStart() {
        super.onStart()
        val metrics = resources.displayMetrics
        dialog?.window?.apply {
            setLayout(minOf(requireContext().dp(384), minOf(metrics.widthPixels, metrics.heightPixels) - requireContext().dp(72)).coerceAtLeast(1), -2)
            setGravity(Gravity.CENTER)
            attributes = attributes.apply { dimAmount = .4f; windowAnimations = R.style.Animation_ViaBrowser_Dialog }
        }
        dialog?.setCanceledOnTouchOutside(true)
        effects?.close(); effects = dialog?.let(SettingsWindowEffects::attachDialog)
    }
    override fun onResume() { super.onResume(); dispatch() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBundle(STATE, if (model.isInitialized) Bundle().also(model::saveState) else savedModel)
        super.onSaveInstanceState(outState)
    }
    override fun onStop() { effects?.close(); effects = null; super.onStop() }
    override fun onDestroyView() {
        host = null; pager?.adapter = null; pager = null
        effects?.close(); effects = null
        super.onDestroyView()
    }
    companion object {
        const val TAG = "native_bookmark_editor"
        const val RESULT = "bookmarkDialogResult2"
        private const val ID = "id"
        private const val URL = "url"
        private const val TITLE = "title"
        private const val POSITION = "position"
        private const val STATE = "bookmark_popup_view_state"
        fun create(request: BookmarkPopupRequest) = BookmarkPopupDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ID, request.id); putString(URL, request.url); putString(TITLE, request.title)
                putInt(POSITION, request.position)
            }
        }
    }
}
