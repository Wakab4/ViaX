package moe.via.browser.feature.adblock.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import moe.via.browser.BrowserActivity
import moe.via.browser.R
import moe.via.browser.core.files.NativeTextFiles
import moe.via.browser.core.text.NativeStringFingerprint
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.adblock.AdblockEngine
import moe.via.browser.feature.adblock.rules.CustomRuleFiles
import moe.via.browser.feature.adblock.rules.FilterRuleSet
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.theme.ThemedActivity

/** z7.v reads the actual custom file, and reloads the engine after every write result. */
class FilterEditorFragment : AdblockRecordPage() {
    private var content: FilterEditorView? = null
    private var baseline = ""
    private var closing = false
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FilterEditorView(requireActivity() as ThemedActivity, appliesWindowInsets, ::closePage, ::help, ::save).also { content = it }.root
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val file = CustomRuleFiles.file(requireContext())
        work({ NativeTextFiles.read(file) }) { text ->
            if (text == null) baseline = "" else { baseline = NativeStringFingerprint.of(text); content?.input?.setText(text) }
        }
    }
    private fun unchanged() = NativeStringFingerprint.of(content!!.input.text.toString()) == baseline
    internal override fun canNavigateBack() = content != null && unchanged()
    internal override fun consumeBack(): Boolean {
        if (closing || content == null || unchanged()) return false
        navigate(::finishEditor); return true
    }
    private fun navigate(action: () -> Unit) {
        if (unchanged()) action() else dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_adblock_page_notice), getString(R.string.ref_adblock_page_unsaved))
            .positive(getString(R.string.ref_adblock_page_save_exit)) { persist(action); true }
            .negative(getString(R.string.ref_adblock_page_exit)) { action(); true })
    }
    private fun persist(action: () -> Unit) {
        val input = content!!.input
        val app = requireContext().applicationContext
        val file = CustomRuleFiles.file(app)
        work({
            val text = input.text.toString()
            val invalid = if (NativeTextFiles.write(file, text)) text.split('\n').filter {
                it.isNotEmpty() && it[0] != '!' && !FilterRuleSet.valid(it)
            }.toMutableList() else null
            if (!invalid.isNullOrEmpty()) invalid.add(0, app.getString(R.string.ref_adblock_page_invalid_rules))
            BrowserExecutors.io.execute { AdblockEngine.get(app).reload() }
            invalid
        }) { invalid ->
            if (invalid.isNullOrEmpty()) action() else {
                val warning = BrowserDialog(requireActivity(), getString(R.string.ref_adblock_page_warning), invalid.joinToString("\n"))
                    .positive(getString(android.R.string.ok)) { action(); true }
                warning.setCancelable(false); warning.setCanceledOnTouchOutside(false); dialogs.show(warning)
            }
        }
    }
    private fun save() { if (unchanged()) finishEditor() else persist(::finishEditor) }
    private fun help() {
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_adblock_page_help), getString(R.string.ref_adblock_page_help_content))
            .positive(getString(android.R.string.ok)).neutral(getString(R.string.ref_adblock_page_more)) {
                navigate { startActivity(Intent(requireActivity(), BrowserActivity::class.java).setAction(Intent.ACTION_VIEW)
                    .setData(Uri.parse("https://help.eyeo.com/en/adblockplus/how-to-write-filters"))) }; true
            })
    }
    private fun finishEditor() { closing = true; closePage(); closing = false }
    override fun onPause() {
        content?.input?.let { it.context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(it.windowToken, 0) }
        super.onPause()
    }
    override fun onDestroyView() { content?.close(); content = null; super.onDestroyView() }
}
