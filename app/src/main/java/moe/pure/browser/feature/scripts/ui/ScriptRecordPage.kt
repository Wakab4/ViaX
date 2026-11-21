package moe.pure.browser.feature.scripts.ui

import android.os.Bundle
import android.view.View
import moe.pure.browser.feature.scripts.data.ScriptProvider
import moe.pure.browser.feature.settings.pages.SettingsRecordPage
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.message.BrowserMessages
import moe.pure.browser.ui.tasks.NativeLifecycleJobs

abstract class ScriptRecordPage : SettingsRecordPage() {
    protected val repository by lazy { ScriptProvider.get(requireContext()).repository }
    private var viewJobs: NativeLifecycleJobs? = null
    private var viewDialogs: DialogScope? = null
    protected val dialogs get() = requireNotNull(viewDialogs)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); viewJobs = NativeLifecycleJobs { viewLifecycleOwner.lifecycle }; viewDialogs = DialogScope() }
    protected fun <T> database(work: () -> T, failed: ((Throwable) -> Unit)? = null, result: (T) -> Unit) {
        val jobs = viewJobs ?: return
        if (failed == null) jobs.launch(work, delivered = result) else jobs.launch(work, failed = failed, delivered = result)
    }
    protected fun message(value: String) = BrowserMessages.show(requireActivity(), value)
    override fun onDestroyView() { viewJobs?.close(); viewJobs = null; viewDialogs?.dispose(); viewDialogs = null; super.onDestroyView() }
}
