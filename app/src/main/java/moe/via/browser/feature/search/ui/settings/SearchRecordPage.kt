package moe.via.browser.feature.search.ui.settings

import android.os.Bundle
import android.view.View
import moe.via.browser.feature.search.data.SearchEngineRepository
import moe.via.browser.feature.search.data.SearchPreferences
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.tasks.NativeViewJobs

/** x7.g0 is WARN; page-view jobs do not add operation epochs or generic failure messages. */
abstract class SearchRecordPage : SettingsRecordPage() {
    protected lateinit var repository: SearchEngineRepository
    protected lateinit var preferences: SearchPreferences
    private var viewJobs: NativeViewJobs? = null
    private var viewDialogs: DialogScope? = null
    protected val dialogs get() = requireNotNull(viewDialogs)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = SearchEngineRepository(requireContext())
        preferences = SearchPreferences(requireContext())
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewJobs = NativeViewJobs(); viewDialogs = DialogScope()
    }
    protected fun <T> database(work: () -> T, result: (T) -> Unit) { viewJobs?.launch(work, delivered = result) }
    protected fun message(text: String) = BrowserMessages.show(requireActivity(), text)
    override fun onDestroyView() {
        viewJobs?.close(); viewJobs = null; viewDialogs?.dispose(); viewDialogs = null
        super.onDestroyView()
    }
    override fun onDestroy() { repository.close(); super.onDestroy() }
}
