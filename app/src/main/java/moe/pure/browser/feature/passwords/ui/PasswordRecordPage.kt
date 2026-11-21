package moe.pure.browser.feature.passwords.ui

import android.os.Bundle
import android.view.View
import moe.pure.browser.feature.passwords.auth.DeviceAuthentication
import moe.pure.browser.feature.passwords.data.PasswordRepository
import moe.pure.browser.feature.settings.pages.SettingsRecordPage
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.message.BrowserMessages
import moe.pure.browser.ui.tasks.NativeViewJobs

/** Password pages own their actual Fragment result registration and view-scoped WARN observers. */
abstract class PasswordRecordPage : SettingsRecordPage() {
    private val repositoryDelegate = lazy { PasswordRepository(requireContext().applicationContext) }
    protected val repository by repositoryDelegate
    protected lateinit var authentication: DeviceAuthentication
    private var viewJobs: NativeViewJobs? = null
    private var viewDialogs: DialogScope? = null
    protected val dialogs get() = requireNotNull(viewDialogs)
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); authentication = DeviceAuthentication(this) }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState); viewJobs = NativeViewJobs(); viewDialogs = DialogScope()
    }
    protected fun <T> database(work: () -> T, done: (T) -> Unit) { viewJobs?.launch(work, delivered = done) }
    protected fun message(text: String) = BrowserMessages.show(requireActivity(), text)
    override fun onDestroyView() { viewJobs?.close(); viewJobs = null; viewDialogs?.dispose(); viewDialogs = null; super.onDestroyView() }
    override fun onDestroy() { authentication.dispose(); if (repositoryDelegate.isInitialized()) repository.close(); super.onDestroy() }
}
