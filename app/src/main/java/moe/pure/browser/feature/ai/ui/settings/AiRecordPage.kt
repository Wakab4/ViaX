package moe.pure.browser.feature.ai.ui.settings

import android.os.Bundle
import android.view.View
import moe.pure.browser.feature.ai.data.AiProvider
import moe.pure.browser.feature.settings.pages.SettingsRecordPage
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.message.BrowserMessages
import moe.pure.browser.ui.tasks.NativeLifecycleJobs

/** Database work belongs to the real page view, using the original x7.g0 WARN callback. */
abstract class AiRecordPage : SettingsRecordPage() {
    private val provider by lazy { AiProvider.get(requireContext().applicationContext) }
    protected val repository get() = provider.repository
    protected val preferences get() = provider.preferences
    private var viewJobs: NativeLifecycleJobs? = null
    private var viewDialogs: DialogScope? = null
    protected val dialogs get() = requireNotNull(viewDialogs)
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); provider }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); viewJobs = NativeLifecycleJobs { viewLifecycleOwner.lifecycle }; viewDialogs = DialogScope() }
    protected fun <T> database(work: () -> T, failed: ((Throwable) -> Unit)? = null, result: (T) -> Unit) {
        val jobs = viewJobs ?: return
        if (failed == null) jobs.launch(work, delivered = result) else jobs.launch(work, failed = failed, delivered = result)
    }
    protected fun message(text: String) = BrowserMessages.show(requireActivity(), text)
    override fun onDestroyView() { viewJobs?.close(); viewJobs = null; viewDialogs?.dispose(); viewDialogs = null; super.onDestroyView() }
}
