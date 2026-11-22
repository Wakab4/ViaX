package moe.via.browser.feature.scripts.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.view.animation.PathInterpolator
import moe.via.browser.BrowserActivity
import moe.via.browser.R
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.core.text.NativeStringFingerprint
import moe.via.browser.feature.scripts.model.ScriptParser
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.editor.NativeTextPrecomputer
import moe.via.browser.ui.theme.ThemedActivity
import java.lang.ref.WeakReference

/** sa.o keeps source and navigation actions separate; no draft files or manual cursor restoration. */
class ScriptEditorFragment : ScriptRecordPage() {
    private var content: ScriptEditorView? = null
    private var id = -1
    private var baseline: String? = null
    private var closing = false
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ScriptEditorView(requireActivity() as ThemedActivity, appliesWindowInsets, ::closePage, ::help, ::save).also { content = it }.root
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState); dragLayout?.gestures(false)
        id = arguments?.getInt("id", -1) ?: -1
        val page = content!!
        if (id > 0) {
            page.page.titleView.setText(R.string.ref_search_page_edit); page.input.isEnabled = false; page.input.alpha = .25f; page.progress.update(30)
            database({ repository.get(id.toLong())?.content }, failed = { NativeLog.error(null, it) }) { code -> if (code != null) {
                baseline = NativeStringFingerprint.of(code)
                val reference = WeakReference(page.input)
                NativeTextPrecomputer.set(page.input, code) { reference.get()?.let { page.progress.update(100); it.alpha = 1f; it.isEnabled = true } }
            } }
        } else {
            page.page.titleView.setText(R.string.ref_script_page_add)
            val template = ScriptEditorTemplate.create(arguments?.getString("url")); page.input.setText(template); baseline = NativeStringFingerprint.of(template)
        }
    }
    private fun unchanged(): Boolean { val input = content!!.input; val text = input.text.toString(); return !input.isEnabled || text.isEmpty() || NativeStringFingerprint.of(text) == baseline }
    internal override fun canNavigateBack() = content != null && unchanged()
    internal override fun consumeBack(): Boolean {
        if (closing || content == null || unchanged()) return false
        navigate(::finishEditor); return true
    }
    private fun navigate(action: () -> Unit) {
        if (unchanged()) { action(); return }
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_text_editor_notice), getString(R.string.ref_text_editor_unsaved))
            .positive(getString(R.string.ref_text_editor_save_exit)) { persist(action); true }.negative(getString(R.string.ref_text_editor_exit)) { action(); true })
    }
    private fun save() {
        if (!content!!.input.isEnabled && unchanged()) finishEditor()
        persist(::finishEditor)
    }
    private fun persist(action: () -> Unit) {
        val page = content!!
        if (!page.input.isEnabled) { action(); return }
        val current = repository.get(id.toLong())
        val parsed = ScriptParser.parse(page.input.text.toString(), current?.downloadUrl)
        if (parsed == null) {
            dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_script_page_metadata_title), getString(R.string.ref_script_page_metadata_message))
                .positive(getString(android.R.string.ok)) {
                    page.input.text.insert(0, ScriptEditorTemplate.HEADER); page.input.clearFocus(); page.scroll.scrollTo(0, 0)
                    page.scroll.alpha = 0f; page.scroll.visibility = View.VISIBLE; page.scroll.animate().alpha(1f).setDuration(250).setInterpolator(PathInterpolator(.2f, .2f, .8f, .8f)).start(); true
                }.negative())
            return
        }
        database({ repository.save(if (current == null) parsed else parsed.copy(enabled = current.enabled), current?.id).toInt() }) { result ->
            if (result != 0) { id = result; parentFragmentManager.setFragmentResult(RESULT, Bundle().apply { putInt("id", result) }); action() }
        }
    }
    private fun help() {
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_script_page_help), Html.fromHtml(getString(R.string.ref_script_page_help_content), Html.FROM_HTML_MODE_LEGACY))
            .positive(getString(android.R.string.ok)).neutral(getString(R.string.ref_script_page_more)) {
                navigate { startActivity(Intent(requireActivity(), BrowserActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse("https://www.tampermonkey.net/documentation.php"))) }; true
            })
    }
    private fun finishEditor() { closing = true; closePage(); closing = false }
    override fun onPause() { content?.input?.let { it.context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(it.windowToken, 0) }; super.onPause() }
    override fun onDestroyView() { content?.close(); content = null; super.onDestroyView() }
    companion object { const val RESULT = "script_result_v2" }
}
