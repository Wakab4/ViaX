package moe.pure.browser.feature.passwords.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.R
import moe.pure.browser.feature.passwords.model.PasswordEntry
import moe.pure.browser.ui.components.recycler.NativeRangeSelection
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.pages.RecordPageScope
import moe.pure.browser.ui.theme.ThemedActivity

class PasswordListFragment : PasswordRecordPage() {
    private var content: PasswordRecordsView? = null
    private var adapter: PasswordRecordAdapter? = null
    private var range: NativeRangeSelection? = null
    private val main = Handler(Looper.getMainLooper())
    private val search = Runnable { val query = content!!.search.text.toString(); load(query); mode(false) }
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val rows = PasswordRecordAdapter(activity, ::clicked, ::held).also { adapter = it }
        return PasswordRecordsView(activity, appliesWindowInsets, ::closePage, { edit(null) }, ::selectAll, ::deleteSelected, { mode(!adapter!!.selecting) }).also {
            content = it; it.list.adapter = rows
            it.search.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(value: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(value: CharSequence?, start: Int, before: Int, count: Int) { main.removeCallbacks(search); main.postDelayed(search, 100) }
                override fun afterTextChanged(value: Editable?) = Unit
            })
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        range = NativeRangeSelection(content!!.list, object : NativeRangeSelection.Actions {
            override fun selectable(position: Int) = adapter!!.selecting && adapter!!.entry(position) != null
            override fun selected(position: Int) = position in adapter!!.selected
            override fun select(position: Int, value: Boolean) { adapter!!.select(position, value); footer() }
        })
        load("")
    }
    private fun load(query: String) { database({ repository.all(query) }) { items -> adapter!!.submit(items); content!!.empty(items.isEmpty()); footer() } }
    private fun mode(value: Boolean) { val rows = adapter!!; if (rows.selecting == value) return; rows.selecting = value; if (!value) rows.selected.clear(); rows.notifyDataSetChanged(); footer() }
    private fun footer() { val rows = adapter!!; content!!.footer.render(rows.selecting, rows.selected.size, rows.entryPositions().size) }
    private fun clicked(position: Int) {
        val rows = adapter!!; val entry = rows.entry(position) ?: return
        if (rows.selecting) { rows.select(position, position !in rows.selected); footer() }
        else authentication.authenticate(getString(R.string.ref_password_page_view), getString(R.string.ref_password_page_view_description), { edit(entry.id) })
    }
    private fun held(anchor: View, position: Int): Boolean {
        val rows = adapter!!; val entry = rows.entry(position) ?: return false
        if (rows.selecting) { range?.start(position); return true }
        dialogs.show(BrowserDialog(requireActivity()).items(listOf(getString(R.string.ref_password_page_copy_username), getString(R.string.ref_password_page_copy_password), getString(R.string.ref_search_page_delete))) { action ->
            when (action) {
                0 -> { PasswordClipboard.copy(requireContext(), entry.username, false); message(getString(R.string.ref_password_page_copied)) }
                1 -> authentication.authenticate(getString(R.string.ref_password_page_copy_password), getString(R.string.ref_password_page_copy_description), {
                    database({ repository.decrypt(entry.id) }) { credential -> if (credential != null) { PasswordClipboard.copy(requireContext(), credential.password, true); message(getString(R.string.ref_password_page_copied)) } }
                })
                2 -> dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_search_page_delete), getString(R.string.ref_password_page_delete_one, entry.username))
                    .positive(getString(android.R.string.ok)) { if (repository.delete(entry.id)) load(""); true }.negative())
            }
        }.anchoredTo(anchor))
        return false
    }
    private fun selectAll() {
        val rows = adapter!!; val positions = rows.entryPositions()
        if (positions.isEmpty()) return
        if (positions.size == rows.selected.size) rows.selected.removeAll(positions.toSet()) else rows.selected.addAll(positions)
        rows.notifyDataSetChanged(); footer()
    }
    private fun deleteSelected() {
        val rows = adapter!!; val positions = rows.selected.toSet()
        if (!rows.selecting || positions.isEmpty()) return
        val text = if (positions.size == 1) getString(R.string.ref_password_page_delete_one, rows.entry(positions.first())!!.username)
            else getString(R.string.ref_password_page_delete_many, positions.size)
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_search_page_delete), text).positive(getString(android.R.string.ok)) {
            val remaining = ArrayList<PasswordEntry>(); var deleted = 0
            rows.rows.forEachIndexed { position, row -> if (row is PasswordRecordAdapter.Row.Entry) {
                if (position !in positions) remaining.add(row.value) else if (repository.delete(row.value.id)) deleted++
            } }
            if (deleted > 0) { rows.submit(remaining); content!!.empty(remaining.isEmpty()) }
            mode(false); true
        }.negative())
    }
    private fun edit(id: String?) {
        parentFragmentManager.setFragmentResultListener(PasswordEditorFragment.RESULT, this) { _, result ->
            if (result.getString(PasswordEditorFragment.RESULT) != null) load("")
            parentFragmentManager.clearFragmentResultListener(PasswordEditorFragment.RESULT)
        }
        RecordPageScope.of(this)?.push(PasswordEditorFragment::class.java, id?.takeIf(String::isNotEmpty)?.let { Bundle().apply { putString("id", it) } })
    }
    override fun onDestroyView() {
        main.removeCallbacksAndMessages(null); range?.close(); range = null; content?.close(); content = null; adapter = null
        super.onDestroyView()
    }
}
