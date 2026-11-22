package moe.via.browser.feature.adblock.ui

import android.os.Bundle
import android.view.View
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.tasks.NativeViewJobs

abstract class AdblockRecordPage : SettingsRecordPage() {
    private var jobs: NativeViewJobs? = null
    private var scope: DialogScope? = null
    protected val dialogs get() = requireNotNull(scope)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState); jobs = NativeViewJobs(); scope = DialogScope()
    }
    protected fun <T> work(action: () -> T, error: (Throwable) -> Unit = { NativeLog.warn(null, it) }, result: (T) -> Unit) {
        jobs?.launch(action, error, result)
    }
    override fun onDestroyView() {
        jobs?.close(); jobs = null; scope?.dispose(); scope = null; super.onDestroyView()
    }
}
