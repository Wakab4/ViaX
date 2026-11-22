package moe.via.browser.feature.library

import android.app.Activity
import android.content.Context
import android.webkit.WebView
import moe.via.browser.feature.offline.document.SavedArchiveFiles
import moe.via.browser.feature.offline.save.SavedPageSaveController

class SavePageController(activity: Activity, currentView: () -> WebView?, openSavedPages: () -> Unit) : AutoCloseable {
    private val delegate = SavedPageSaveController(activity, currentView, openSavedPages)
    fun show(view: WebView?) = delegate.show(view)
    override fun close() = delegate.close()

    companion object {
        fun isOwnedArchive(activity: Context, url: String): Boolean {
            return SavedArchiveFiles.containsArchive(activity, url)
        }
    }
}
