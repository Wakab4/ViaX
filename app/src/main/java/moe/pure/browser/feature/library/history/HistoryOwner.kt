package moe.pure.browser.feature.library.history

import android.content.Context
import androidx.lifecycle.ViewModel
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.library.LibraryRepository

/** The real history child owns its model; framework ViewModel retention replaces Activity retention. */
internal class HistoryOwner(context: Context) : ViewModel() {
    private val repository = LibraryRepository(context.applicationContext)
    val pager = HistoryPager(repository, BrowserExecutors.io)
    var opened = false
    override fun onCleared() { pager.close(); repository.close() }
}
