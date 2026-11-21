package moe.pure.browser.feature.library.popup.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import moe.pure.browser.R
import moe.pure.browser.feature.library.popup.BookmarkPopupFolderRow
import moe.pure.browser.feature.library.popup.BookmarkPopupModel
import moe.pure.browser.feature.library.ui.recycler.NativeRecyclerEffects
import moe.pure.browser.ui.components.BrowserMessage
import moe.pure.browser.ui.components.dp

/** b8.n0: a second real Fragment, with mutually exclusive search and folder creation modes. */
class BookmarkPopupFoldersPage : Fragment() {
    private val model by lazy { ViewModelProvider(requireParentFragment())[BookmarkPopupModel::class.java] }
    private val main = Handler(Looper.getMainLooper())
    private var content: BookmarkPopupFoldersView? = null
    private var adapter: BookmarkPopupFolderAdapter? = null
    private var rows: List<BookmarkPopupFolderRow>? = null
    private var pendingQuery = ""
    private val searchJob = Runnable { if (content != null) model.search(pendingQuery) }
    private var watcher: TextWatcher? = null
    private var shake: ObjectAnimator? = null
    private var messages: BrowserMessage? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        BookmarkPopupFoldersView(inflater.context).also { content = it }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val page = content ?: return
        messages = BrowserMessage(requireActivity())
        NativeRecyclerEffects.install(page.list)
        adapter = BookmarkPopupFolderAdapter(page.context, model::chooseFolder).also { page.list.adapter = it }
        page.up.setOnClickListener { returnToEditor() }
        page.add.setOnClickListener {
            when {
                page.searchInput.visibility == View.VISIBLE -> search(false)
                page.createRow.visibility == View.GONE -> create(true)
                else -> create(false)
            }
        }
        var searchClick: Long? = null
        page.search.setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            val previous = searchClick
            if (previous == null || kotlin.math.abs(now - previous) > 300L) { searchClick = now; search(page.searchInput.visibility != View.VISIBLE) }
        }
        page.confirm.setOnClickListener { createFolder() }
        page.newFolder.setOnKeyListener { _, code, _ ->
            if (code == KeyEvent.KEYCODE_ENTER) { createFolder(); true } else false
        }
        page.searchInput.setText(model.state.value?.query.orEmpty())
        watcher = object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                pendingQuery = text?.toString().orEmpty()
                main.removeCallbacks(searchJob); main.postDelayed(searchJob, 100L)
            }
            override fun afterTextChanged(text: Editable?) = Unit
        }.also(page.searchInput::addTextChangedListener)
        pendingQuery = page.searchInput.text.toString()
        main.postDelayed(searchJob, 100L)
        restoreModes(savedInstanceState)
        model.state.observe(viewLifecycleOwner) { state ->
            if (content !== page || !state.loaded || rows === state.folderRows) return@observe
            rows = state.folderRows
            adapter?.submit(state.folderRows)
            val empty = state.folderRows.isEmpty()
            page.empty.visibility = if (empty) View.VISIBLE else View.GONE
            page.list.visibility = if (empty) View.GONE else View.VISIBLE
            val selected = state.folderRows.indexOfFirst { it.selected }
            if (selected >= 0) page.list.post { if (content === page) page.list.scrollToPosition(selected) }
        }
    }

    private fun create(show: Boolean) {
        val page = content ?: return
        page.add.animate().rotation(if (show) 45f else 0f).setDuration(80L).start()
        if (show) {
            page.createRow.alpha = 0f; page.createRow.visibility = View.VISIBLE
            page.list.translationY = -page.dp(48).toFloat()
            page.createRow.animate().alpha(1f).setDuration(150L).setInterpolator(motion()).start()
            page.list.animate().translationY(0f).setDuration(150L).setInterpolator(motion()).start()
        } else {
            page.createRow.animate().alpha(0f).setDuration(150L).setInterpolator(motion())
                .withEndAction { page.createRow.visibility = View.GONE }.start()
            page.list.animate().translationY(-page.dp(48).toFloat()).setDuration(150L).setInterpolator(motion())
                .withEndAction { page.list.translationY = 0f }.start()
        }
    }

    private fun search(show: Boolean) {
        val page = content ?: return
        if (page.createRow.visibility == View.VISIBLE) create(false)
        if (show) {
            page.title.animate().alpha(0f).setDuration(100L).withEndAction { page.title.visibility = View.GONE }.start()
            page.search.animate().alpha(0f).setDuration(100L).withEndAction { page.search.visibility = View.GONE }.start()
            page.searchInput.alpha = 0f; page.searchInput.visibility = View.VISIBLE
            page.searchInput.animate().alpha(1f).setDuration(100L).start()
            page.add.animate().rotation(45f).setDuration(80L).start()
            page.searchInput.requestFocus(); keyboard(page.searchInput, true)
        } else {
            page.searchInput.setText(""); keyboard(page.searchInput, false)
            page.title.visibility = View.VISIBLE; page.title.animate().alpha(1f).setDuration(100L).start()
            page.search.visibility = View.VISIBLE; page.search.animate().alpha(1f).setDuration(100L).start()
            page.searchInput.animate().alpha(0f).setDuration(100L).withEndAction { page.searchInput.visibility = View.GONE }.start()
            page.add.animate().rotation(0f).setDuration(80L).start()
        }
    }

    private fun createFolder() {
        val page = content ?: return
        keyboard(page.add, false)
        val raw = page.newFolder.text.toString()
        if (raw.isEmpty()) {
            shake?.cancel()
            shake = ObjectAnimator.ofFloat(page.newFolder, "translationX", 0f, page.dp(24).toFloat(), 0f)
                .apply { duration = 280L; repeatCount = 2; repeatMode = android.animation.ValueAnimator.RESTART; interpolator = motion(); start() }
            messages?.show(getString(R.string.ref_bookmark_form_required, page.newFolder.hint))
            return
        }
        model.createFolder(raw)
        page.newFolder.setText("")
        create(false)
    }

    fun returnToEditor() {
        content?.takeIf { it.createRow.visibility == View.VISIBLE }?.let { create(false) }
        model.page(0)
    }

    private fun keyboard(view: View, show: Boolean) {
        val manager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
        if (show) manager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        else manager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun motion() = PathInterpolator(.2f, .2f, .8f, .8f)

    private fun restoreModes(saved: Bundle?) {
        val page = content ?: return
        if (saved?.getBoolean(SEARCH) == true) {
            page.title.visibility = View.GONE; page.search.visibility = View.GONE
            page.searchInput.visibility = View.VISIBLE; page.add.rotation = 45f
        } else if (saved?.getBoolean(CREATE) == true) {
            page.createRow.visibility = View.VISIBLE; page.add.rotation = 45f
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        content?.let { outState.putBoolean(SEARCH, it.searchInput.visibility == View.VISIBLE); outState.putBoolean(CREATE, it.createRow.visibility == View.VISIBLE) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        main.removeCallbacks(searchJob); shake?.cancel(); shake = null
        val page = content
        watcher?.let { page?.searchInput?.removeTextChangedListener(it) }; watcher = null
        page?.newFolder?.setOnKeyListener(null)
        page?.list?.adapter = null
        adapter?.close(); adapter = null; rows = null
        messages?.close(); messages = null
        page?.close(); content = null
        super.onDestroyView()
    }

    companion object {
        private const val SEARCH = "bookmark.popup.folders.search"
        private const val CREATE = "bookmark.popup.folders.create"
    }
}
