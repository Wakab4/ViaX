package moe.via.browser.feature.library.pages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import moe.via.browser.feature.library.data.LibraryRepository
import moe.via.browser.feature.library.navigation.LibraryOpenQueue
import moe.via.browser.feature.library.navigation.LibraryOpenRequest
import moe.via.browser.feature.library.navigation.LibraryOpenWhere

internal class LibraryModel(app: Application) : AndroidViewModel(app) {
    enum class Event { CLOSE, BACKGROUND, RESTORE_TABS }
    val repository = LibraryRepository(app)
    val queue = LibraryOpenQueue()
    val events = MutableLiveData<Event?>()
    val paging = MutableLiveData(true)
    fun open(url: String?, where: LibraryOpenWhere) {
        if (url == null || !queue.add(LibraryOpenRequest(url, where))) return
        events.value = if (where == LibraryOpenWhere.BACKGROUND) Event.BACKGROUND else Event.CLOSE
    }
    fun open(values: List<LibraryOpenRequest>) {
        if (values.isEmpty()) return
        values.forEach { queue.add(it) }
        events.value = if (values.any { it.where == LibraryOpenWhere.CURRENT || it.where == LibraryOpenWhere.FOREGROUND }) Event.CLOSE else Event.BACKGROUND
    }
    override fun onCleared() { repository.close() }
}
