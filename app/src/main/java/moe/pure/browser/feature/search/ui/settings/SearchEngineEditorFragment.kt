package moe.pure.browser.feature.search.ui.settings

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import moe.pure.browser.R
import moe.pure.browser.data.settings.NativeSettingsRow
import moe.pure.browser.core.text.NativeStringFingerprint
import moe.pure.browser.feature.search.resolve.SearchEngineEditorRules
import moe.pure.browser.feature.search.resolve.SearchShortcuts
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.ThemedActivity

/** hb.c1: argument identity and raw database record stay separate; no editor file/token restoration. */
class SearchEngineEditorFragment : SearchRecordPage() {
    private var fields: SearchEngineEditorFields? = null
    private var engineId = 0
    private var record = NativeSettingsRow()
    private var baseline: String? = null
    private var shortcuts = mutableMapOf<Int, String>()
    private var closing = false
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); engineId = arguments?.getInt("id", 0) ?: 0 }
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val input = SearchEngineEditorFields(activity, engineId < 0).also { fields = it }
        val page = PageScaffold(activity, getString(if (engineId == 0) R.string.ref_search_page_new else R.string.ref_search_page_edit), ::closePage, appliesWindowInsets)
        page.addTextAction(getString(R.string.ref_text_editor_save), ::save)
        page.showContent(input.root)
        input.address.setOnKeyListener { _, key, event ->
            if (key != KeyEvent.KEYCODE_ENTER) false else { if (event.action == KeyEvent.ACTION_UP) save(); true }
        }
        input.shortcut.setOnEditorActionListener { _, action, _ -> if (action == EditorInfo.IME_ACTION_DONE) { save(); true } else false }
        return page.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        record = NativeSettingsRow(); shortcuts = preferences.shortcuts().toMutableMap(); baseline = null
        database({ repository.editorRecord(engineId) }) { loaded -> if (loaded != null) {
            record = loaded
            fields!!.title.setText(loaded.title); fields!!.address.setText(loaded.content)
            val shortcut = shortcuts[loaded.id].orEmpty(); fields!!.shortcut.setText(shortcut)
            baseline = NativeStringFingerprint.of(loaded.title, loaded.content, shortcut)
        } }
    }
    private fun title() = SearchEngineEditorRules.cleanTitle(fields!!.title.text.toString())
    private fun address(): String? = if (engineId < 0) null else SearchEngineEditorRules.normalizeTemplate(fields!!.address.text.toString())
    private fun shortcut() = SearchShortcuts.normalize(fields!!.shortcut.text.toString())
    private fun unchanged(): Boolean {
        val value = NativeStringFingerprint.of(title(), address(), shortcut())
        return baseline == null && value.isEmpty() || value == baseline
    }
    internal override fun canNavigateBack() = fields != null && unchanged()
    internal override fun consumeBack(): Boolean {
        if (closing || fields == null || unchanged()) return false
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_text_editor_notice), getString(R.string.ref_text_editor_unsaved))
            .positive(getString(R.string.ref_text_editor_save_exit)) { save(); true }
            .negative(getString(R.string.ref_text_editor_exit)) { finishEditor(); true })
        return true
    }
    private fun save() {
        if (unchanged()) { finishEditor(); return }
        val word = shortcut()
        val valid = SearchShortcuts.validate(word, engineId, shortcuts)
        if (!valid.valid) {
            shake(fields!!.shortcut)
            message(getString(if (word.codePointCount(0, word.length) > 16 || word.any(Character::isWhitespace))
                R.string.ref_search_page_invalid_shortcut else R.string.ref_search_page_duplicate_shortcut)); return
        }
        if (engineId < 0) { saved(engineId, word); return }
        val title = title(); val address = address()
        if (title.isEmpty()) { message(getString(R.string.ref_search_page_empty_title)); return }
        if (address.isNullOrEmpty()) { message(getString(R.string.ref_search_page_required, fields!!.address.hint)); return }
        if (SearchEngineEditorRules.isRecursive(address)) { message(getString(R.string.ref_search_page_invalid_url)); return }
        record = record.copy(title = title, content = address)
        database({ record = repository.saveEditor(record) }) { saved(record.id, word) }
    }
    private fun saved(id: Int, word: String) {
        shortcuts[id] = word; preferences.writeShortcuts(shortcuts)
        parentFragmentManager.setFragmentResult(RESULT, Bundle().apply { putInt(RESULT, id) })
        finishEditor()
    }
    private fun finishEditor() { closing = true; closePage(); closing = false }
    private fun shake(field: EditText) {
        ObjectAnimator.ofFloat(field, "translationX", 0f, field.dp(24).toFloat(), 0f).apply {
            repeatCount = 2; repeatMode = ObjectAnimator.RESTART; duration = 280
            interpolator = PathInterpolator(.2f, .2f, .8f, .8f); start()
        }
    }
    override fun onPause() {
        fields?.let { val input = if (it.shortcut.hasFocus()) it.shortcut else it.address
            input.context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(input.windowToken, 0)
        }
        super.onPause()
    }
    override fun onDestroyView() { fields = null; super.onDestroyView() }
    companion object { const val RESULT = "engine_result" }
}
