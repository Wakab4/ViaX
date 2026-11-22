package moe.via.browser.feature.ai.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import moe.via.browser.R
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.core.text.NativeStringFingerprint
import moe.via.browser.feature.ai.model.AiPrompt
import moe.via.browser.feature.ai.prompt.AiPromptVariables
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.theme.ThemedActivity
import java.util.UUID

/** a9.f1 returns sug_result; even a normal false save follows its original result/close path. */
class AiPromptEditorFragment : AiRecordPage() {
    private var page: PageScaffold? = null
    private var fields: AiPromptFields? = null
    private var variables: AiVariableAdapter? = null
    private var prompt = AiPrompt(id = "")
    private var baseline: String? = null
    private var closing = false
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val input = AiPromptFields(activity).also { fields = it }
        variables = AiVariableAdapter(activity).also { input.variables.adapter = it }
        return PageScaffold(activity, getString(R.string.ref_search_page_new), ::closePage, appliesWindowInsets).also {
            page = it; it.addTextAction(getString(R.string.ref_text_editor_save), ::save); it.showContent(input.root)
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val id = arguments?.getString("id")
        if (id != null) database({ repository.prompts.get(id) }, failed = { NativeLog.error(null, it) }) { loaded ->
            if (loaded != null) {
                prompt = loaded; fields!!.title.editor.setText(loaded.name); fields!!.content.setText(loaded.content)
                baseline = NativeStringFingerprint.of(loaded.name, loaded.content); page!!.titleView.setText(R.string.ref_search_page_edit)
            } else prompt = AiPrompt(id = "")
            variables!!.type(prompt.type)
        } else {
            prompt = AiPrompt(id = "", type = if (arguments?.getInt("type", 1) == 2) 2 else 1)
            variables!!.type(prompt.type)
        }
    }
    private fun values() = AiPromptVariables.normalizeName(fields!!.title.editor.text.toString()) to AiPromptVariables.normalizeContent(fields!!.content.text.toString())
    private fun unchanged(): Boolean { val values = values(); val current = NativeStringFingerprint.of(values.first, values.second); return baseline == null && current.isEmpty() || current == baseline }
    internal override fun canNavigateBack() = fields != null && unchanged()
    internal override fun consumeBack(): Boolean {
        if (closing || fields == null || unchanged()) return false
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_text_editor_notice), getString(R.string.ref_text_editor_unsaved))
            .positive(getString(R.string.ref_text_editor_save_exit)) { save(); true }.negative(getString(R.string.ref_text_editor_exit)) { finishEditor(); true })
        return true
    }
    private fun save() {
        if (unchanged()) { finishEditor(); return }
        val values = values()
        if (values.first.isEmpty()) { AiEditorFeedback.required(fields!!.title.editor); return }
        if (values.second.isEmpty()) { AiEditorFeedback.required(fields!!.content); return }
        prompt = AiPromptVariables.prepare(prompt.copy(name = values.first, content = values.second))
        database({
            val new = prompt.id.isEmpty(); val now = System.currentTimeMillis()
            prompt = prompt.copy(id = if (new) UUID.randomUUID().toString() else prompt.id, createdAt = if (new) now else prompt.createdAt, updatedAt = now)
            val saved = if (new) repository.prompts.insert(prompt) else repository.prompts.update(prompt)
            if (new && !saved) prompt = prompt.copy(id = "")
            saved
        }) {
            parentFragmentManager.setFragmentResult(RESULT, Bundle().apply { putString(RESULT, prompt.id.takeIf(String::isNotEmpty)) }); finishEditor()
        }
    }
    private fun finishEditor() { closing = true; closePage(); closing = false }
    override fun onPause() { fields?.content?.let { it.context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(it.windowToken, 0) }; super.onPause() }
    override fun onDestroyView() { fields?.close(); fields = null; page = null; variables = null; super.onDestroyView() }
    companion object { const val RESULT = "sug_result" }
}
