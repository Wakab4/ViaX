package moe.via.browser.feature.library.forms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import moe.via.browser.R
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.ui.components.NativeToolbarTap
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.components.recycler.NativeRecyclerView
import moe.via.browser.ui.editor.NativeFieldFeedback
import moe.via.browser.ui.theme.ThemedActivity

abstract class BookmarkRecordForm : SettingsRecordPage() {
    internal abstract val kind: BookmarkFormKind
    private var model: BookmarkFormModel? = null
    private var page: PageScaffold? = null
    private var fields: BookmarkFormView? = null
    private var folderRows: BookmarkFolderAdapter? = null
    private var folderList: NativeRecyclerView? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val request = BookmarkFormRequest.read(kind, arguments)
        val value = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = BookmarkFormModel(activity.applicationContext, request) as T
        })[BookmarkFormModel::class.java].also { model = it }
        return PageScaffold(activity, caption(value), ::closePage, appliesWindowInsets).also { scaffold ->
            page = scaffold; scaffold.addTextAction(getString(R.string.ref_bookmark_form_done), ::save)
            if (kind == BookmarkFormKind.MOVE) {
                val adapter = BookmarkFolderAdapter(activity, false, value::select, ::createFolder)
                val list = NativeRecyclerView(activity).apply { id = android.R.id.list; layoutManager = LinearLayoutManager(activity); this.adapter = adapter }
                folderRows = adapter; folderList = list; scaffold.showContent(list, bottomInsets = list)
                NativeToolbarTap.bind(scaffold.toolbar) { list.smoothScrollToPosition(0) }
            } else {
                val input = BookmarkFormView(activity, value, value::select, ::createFolder)
                fields = input; scaffold.showContent(input.root)
            }
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState); model!!.updates.observe(viewLifecycleOwner) { render() }; render()
    }
    private fun caption(value: BookmarkFormModel) = getString(when (kind) {
        BookmarkFormKind.MOVE -> R.string.ref_bookmark_form_select_folder
        BookmarkFormKind.BOOKMARK -> if (value.ready && value.isNew) R.string.ref_bookmark_form_add_bookmark else R.string.ref_bookmark_form_edit_bookmark
        BookmarkFormKind.FOLDER -> if (value.ready && value.isNew) R.string.ref_bookmark_form_new_folder else R.string.ref_bookmark_form_edit_folder
    })
    private fun render() {
        val value = model ?: return
        page?.titleView?.text = caption(value); fields?.render(); folderRows?.submit(value.folders, value.selected, value.request.creationEnabled)
        value.result?.let { id ->
            parentFragmentManager.setFragmentResult(key(), Bundle().apply { putString(if (kind == BookmarkFormKind.MOVE) "result_id" else "id", id) })
            closePage()
        }
    }
    private fun save() {
        val value = model ?: return
        if (!value.ready) return
        if (kind != BookmarkFormKind.MOVE && value.title.isEmpty()) { fields?.title?.let { NativeFieldFeedback.required(it) }; return }
        if (kind == BookmarkFormKind.BOOKMARK && value.url.trim { it <= ' ' }.isEmpty()) { fields?.address?.let { NativeFieldFeedback.required(it) }; return }
        value.save()
    }
    private fun createFolder() {
        val value = model ?: return
        BookmarkFormNavigation.folder(this, viewLifecycleOwner, null, value.selected, true, value.request.showHidden) { id -> id?.let(value::child) }
    }
    private fun key() = when (kind) { BookmarkFormKind.BOOKMARK -> BookmarkFormNavigation.BOOKMARK; BookmarkFormKind.FOLDER -> BookmarkFormNavigation.FOLDER; BookmarkFormKind.MOVE -> BookmarkFormNavigation.PICKER }
    override fun onDestroyView() {
        fields?.close(); fields = null; folderList?.dispose(); folderList?.adapter = null; folderList = null; folderRows = null; page = null; super.onDestroyView()
    }
    override fun onDestroy() {
        if (kind == BookmarkFormKind.BOOKMARK && model?.folderCreated == true)
            parentFragmentManager.setFragmentResult(BookmarkFormNavigation.BOOKMARK, Bundle().apply { putBoolean("folder_created", true) })
        model = null; super.onDestroy()
    }
}
