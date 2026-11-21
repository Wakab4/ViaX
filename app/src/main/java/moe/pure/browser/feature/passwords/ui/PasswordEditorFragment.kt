package moe.pure.browser.feature.passwords.ui

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import moe.pure.browser.R
import moe.pure.browser.core.text.NativeStringFingerprint
import moe.pure.browser.feature.passwords.model.PasswordCredential
import moe.pure.browser.feature.passwords.model.PasswordEntry
import moe.pure.browser.feature.passwords.model.PasswordSites
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.ThemedActivity

/** va.s owns the four live fields and its result; storage/decryption stay behind the repository API. */
class PasswordEditorFragment : PasswordRecordPage() {
    private var fields: PasswordEditorFields? = null
    private var page: PageScaffold? = null
    private var entry = PasswordEntry(id = "", name = "", username = "")
    private var submittedPassword = ""
    private var baseline: String? = null
    private var stopped = false
    private var closing = false
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val input = PasswordEditorFields(activity).also { fields = it }
        return PageScaffold(activity, getString(R.string.ref_search_page_new), ::closePage, appliesWindowInsets).also {
            page = it; it.addTextAction(getString(R.string.ref_text_editor_save), ::save); it.showContent(input.root)
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        entry = PasswordEntry(id = "", name = "", username = ""); baseline = null; submittedPassword = ""
        arguments?.getString("id")?.takeIf(String::isNotEmpty)?.let(::load)
    }
    private fun load(id: String) { database({ repository.decrypt(id) }) { bind(it) } }
    private fun bind(credential: PasswordCredential?) {
        val input = fields!!
        if (credential == null) {
            entry = PasswordEntry(id = "", name = "", username = "")
            input.editors().forEach { it.setText(null as CharSequence?) }
            baseline = NativeStringFingerprint.of(null, null, null, "")
        } else {
            entry = credential.entry; submittedPassword = credential.password
            if (entry.id.isNotEmpty()) page!!.titleView.setText(R.string.ref_search_page_edit)
            input.site.setText(entry.name); input.username.setText(entry.username)
            input.password.setText(credential.password); input.note.setText(entry.note)
            baseline = NativeStringFingerprint.of(entry.name, entry.username, credential.password, entry.note)
        }
    }
    private fun values() = fields!!.values().toMutableList().apply { this[0] = PasswordSites.normalizeEditor(this[0]) }
    private fun unchanged(): Boolean {
        val fingerprint = NativeStringFingerprint.of(*values().toTypedArray())
        return baseline == null && fingerprint.isEmpty() || fingerprint == baseline
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
        val values = values()
        for (index in 0..2) if (values[index].isEmpty()) { required(fields!!.editors()[index]); return }
        val duplicate = repository.find(values[0], values[1])
        if (duplicate != null && duplicate != entry.id) {
            dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_password_page_duplicate), getString(R.string.ref_password_page_duplicate_message, values[0]))
                .positive(getString(android.R.string.ok)) {
                    load(duplicate)
                    ObjectAnimator.ofFloat(fields!!.password, "alpha", 1f, 0f, 1f, 0f, 1f).apply { duration = 1200; start() }
                    true
                }.negative())
            return
        }
        entry = entry.copy(name = values[0], username = values[1], note = values[3]); submittedPassword = values[2]
        database({ repository.save(entry, submittedPassword).also { entry = it } }) { saved ->
            parentFragmentManager.setFragmentResult(RESULT, Bundle().apply { putString(RESULT, saved.id) }); finishEditor()
        }
    }
    private fun required(editor: EditText) {
        moe.pure.browser.ui.editor.NativeFieldFeedback.required(editor)
    }
    private fun finishEditor() { closing = true; closePage(); closing = false }
    override fun onResume() {
        super.onResume(); requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (stopped) {
            fields!!.root.visibility = View.GONE
            authentication.authenticate(getString(R.string.ref_password_page_view), getString(R.string.ref_password_page_view_description),
                { fields?.root?.visibility = View.VISIBLE }, { finishEditor() })
        }
    }
    override fun onPause() {
        fields?.note?.let { it.context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(it.windowToken, 0) }
        super.onPause()
    }
    override fun onStop() { requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE); stopped = true; super.onStop() }
    override fun onDestroyView() { fields = null; page = null; super.onDestroyView() }
    companion object { const val RESULT = "result" }
}
