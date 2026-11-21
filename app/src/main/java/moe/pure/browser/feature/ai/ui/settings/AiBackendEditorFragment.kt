package moe.pure.browser.feature.ai.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import moe.pure.browser.BrowserActivity
import moe.pure.browser.R
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.core.text.NativeStringFingerprint
import moe.pure.browser.feature.ai.model.AiBackend
import moe.pure.browser.feature.ai.prompt.AiTemplates
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.ThemedActivity

/** a9.r0 owns its model list and save guard; configuration fields are not written into retained buffers. */
class AiBackendEditorFragment : AiRecordPage() {
    private var page: PageScaffold? = null
    private var fields: AiBackendFields? = null
    private var modelAdapter: AiModelAdapter? = null
    private var original: AiBackend? = null
    private val models = ArrayList<String>()
    private var modelsChanged = false
    private var baseline: String? = null
    private var saving = false
    private var closing = false
    private val client get() = moe.pure.browser.feature.ai.data.AiProvider.get(requireContext()).client
    private var verification = 0
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val input = AiBackendFields(activity, ::addModel, ::openManagement).also { fields = it }
        modelAdapter = AiModelAdapter(activity, models) { position -> modelsChanged = true; models.removeAt(position); modelAdapter!!.notifyItemRemoved(position) }.also { input.models.adapter = it }
        return PageScaffold(activity, getString(R.string.ref_search_page_new), ::closePage, appliesWindowInsets).also {
            page = it; it.addTextAction(getString(R.string.ref_ai_page_verify), ::verify); it.addTextAction(getString(R.string.ref_text_editor_save), ::save); it.showContent(input.root)
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState); original = null; NativeTypography.tree(view)
        val id = arguments?.getString("id")
        if (!id.isNullOrEmpty()) database({
            val template = AiTemplates.get(id)
            if (template != null) template.copy(id = "") else repository.backends.get(id) ?: throw NullPointerException("The callable returned a null value.")
        }) { bind(it) }
    }
    private fun bind(backend: AiBackend) {
        original = backend
        if (backend.id.isNotEmpty()) page!!.titleView.setText(R.string.ref_search_page_edit)
        fields!!.title.editor.setText(backend.name); fields!!.url.editor.setText(backend.baseUrl); fields!!.key.setText(backend.token)
        fields!!.management.visibility = if (AiTemplates.keyManagementUrl(backend.baseUrl).isNullOrEmpty()) View.GONE else View.VISIBLE
        models.clear(); models.addAll(backend.models); modelAdapter!!.notifyDataSetChanged()
        baseline = NativeStringFingerprint.of(backend.name, backend.baseUrl, backend.token, ""); modelsChanged = false
    }
    private fun unchanged(): Boolean { val fingerprint = NativeStringFingerprint.of(*fields!!.values().toTypedArray()); return (baseline == null && fingerprint.isEmpty() || fingerprint == baseline) && !modelsChanged }
    internal override fun canNavigateBack() = fields != null && unchanged()
    internal override fun consumeBack(): Boolean {
        if (closing || fields == null) return false
        if (saving) return true
        if (unchanged()) return false
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_text_editor_notice), getString(R.string.ref_text_editor_unsaved))
            .positive(getString(R.string.ref_text_editor_save_exit)) { save(); true }.negative(getString(R.string.ref_text_editor_exit)) { finishEditor(); true })
        return true
    }
    private fun addModel() {
        val value = fields!!.pendingModel.editor.text.toString().trim { it <= ' ' }
        if (value.isEmpty()) return
        fields!!.pendingModel.editor.setText("")
        if (value in models) return
        modelsChanged = true; val position = models.size; models.add(value); modelAdapter!!.notifyItemInserted(position)
    }
    private fun openManagement() {
        val url = AiTemplates.keyManagementUrl(fields!!.url.editor.text.toString()) ?: return
        startActivity(Intent(requireActivity(), BrowserActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse(url)))
    }
    private fun save() {
        if (saving) return
        if (unchanged()) { finishEditor(); return }
        val values = fields!!.values()
        for (index in values.indices) if (values[index].isEmpty() && (index != 3 || models.isEmpty())) { AiEditorFeedback.required(fields!!.editors()[index]); return }
        saving = true
        val new = original?.id.isNullOrEmpty()
        val record = if (new) AiBackend() else AiBackend(id = original!!.id, flags = original!!.flags, createdAt = original!!.createdAt)
        val pending = values[3]
        if (pending !in models) models.add(pending)
        val saved = record.copy(name = values[0], baseUrl = AiTemplates.normalizeBaseUrl(values[1]), token = values[2], models = models.toList(), updatedAt = System.currentTimeMillis())
        if (new) original = saved
        database({ if (new) repository.backends.insert(saved) else repository.backends.update(saved) }, failed = { NativeLog.warn(null, it); saving = false }) { success ->
            if (success) { parentFragmentManager.setFragmentResult(RESULT, Bundle().apply { putString("id", original!!.id) }); finishEditor() }
            else if (new) original = null
            saving = false
        }
    }
    private fun verify() {
        val values = fields!!.values()
        for (index in 0..2) if (values[index].isEmpty()) { AiEditorFeedback.required(fields!!.editors()[index]); return }
        if (models.isEmpty()) { AiEditorFeedback.required(fields!!.pendingModel.editor, shake = false); return }
        val backend = AiBackend(name = values[0], baseUrl = values[1], token = values[2], models = models.toList())
        message(getString(R.string.ref_ai_page_wait)); val current = ++verification
        client.validate(backend) { result ->
            if (current != verification || !isVisible || isRemoving) return@validate
            if (result.isSuccess) dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_ai_page_verify), getString(R.string.ref_ai_page_verified)).positive(getString(android.R.string.ok)))
            else dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_ai_page_failed), errorText(result.exceptionOrNull())).positive(getString(android.R.string.ok)))
        }
    }
    private fun errorText(error: Throwable?): String {
        return moe.pure.browser.feature.ai.network.AiErrorText.format(requireContext(), error)
    }
    private fun finishEditor() { closing = true; closePage(); closing = false }
    override fun onPause() { fields?.title?.editor?.let { it.context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(it.windowToken, 0) }; super.onPause() }
    override fun onDestroyView() { fields?.close(); fields = null; page = null; modelAdapter = null; super.onDestroyView() }
    companion object { const val RESULT = "result" }
}
