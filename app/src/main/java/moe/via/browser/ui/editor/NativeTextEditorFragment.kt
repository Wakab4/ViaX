package moe.via.browser.ui.editor

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import moe.via.browser.R
import moe.via.browser.core.text.NativeStringFingerprint
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.pages.RecordPageFragment

/** hb.i1 returns raw text to its caller; it neither persists feature data nor writes a draft cache. */
class NativeTextEditorFragment : RecordPageFragment() {
    private var page: NativeTextEditorPage? = null
    private var dialogs: DialogScope? = null
    private var baseline = ""
    private var exiting = false
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val args = arguments
        baseline = NativeStringFingerprint.of(args?.getString(TEXT))
        dialogs = DialogScope()
        return NativeTextEditorPage(requireActivity(), if (args == null) getString(R.string.ref_text_editor_untitled) else args.getString(TITLE), args?.getString(TEXT),
            args?.getString(PLACEHOLDER), args?.getBoolean(CODE, false) == true, appliesWindowInsets,
            ::closePage, { finishEditor(publish = true) }).also { page = it }.root
    }
    private fun unchanged() = NativeStringFingerprint.of(page?.input?.text?.toString()) == baseline
    internal override fun canNavigateBack() = unchanged()
    internal override fun consumeBack(): Boolean {
        if (exiting || unchanged()) return false
        dialogs?.show(BrowserDialog(requireActivity(), getString(R.string.ref_text_editor_notice), getString(R.string.ref_text_editor_unsaved))
            .positive(getString(R.string.ref_text_editor_save_exit)) { finishEditor(publish = true); true }
            .negative(getString(R.string.ref_text_editor_exit)) { finishEditor(publish = false); true })
        return true
    }
    private fun finishEditor(publish: Boolean) {
        if (publish) parentFragmentManager.setFragmentResult(RESULT, Bundle().apply { putString(TEXT, page!!.input.text.toString()) })
        exiting = true
        closePage()
        exiting = false
    }
    override fun onPause() {
        page?.input?.let { (it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(it.windowToken, 0) }
        super.onPause()
    }
    override fun onDestroyView() {
        dialogs?.dispose(); dialogs = null; page?.close(); page = null
        super.onDestroyView()
    }
    companion object {
        const val RESULT = "edit_text_result"
        const val TEXT = "text"
        private const val TITLE = "title"
        private const val PLACEHOLDER = "placeholder"
        private const val CODE = "code"
        internal fun arguments(title: String?, text: String?, placeholder: String?, code: Boolean) = Bundle().apply {
            if (!title.isNullOrEmpty()) putString(TITLE, title)
            if (!text.isNullOrEmpty()) putString(TEXT, text)
            if (!placeholder.isNullOrEmpty()) putString(PLACEHOLDER, placeholder)
            if (code) putBoolean(CODE, true)
        }
    }
}
