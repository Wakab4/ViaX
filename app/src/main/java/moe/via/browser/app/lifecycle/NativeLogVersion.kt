package moe.via.browser.app.lifecycle

import android.content.Context
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.core.files.NativeDirectories
import moe.via.browser.core.concurrent.BrowserExecutors
import java.io.File

internal object NativeLogVersion {
    fun clearAfterUpgrade(context: Context, log: File) {
        val version = NativeVersionMarker(context)
        if (version.previous >= version.installed) return
        BrowserExecutors.io.execute {
            try {
                val directory = log.parentFile ?: return@execute
                NativeDirectories.delete(directory)
                NativeDirectories.ensure(directory)
            } catch (error: Exception) { NativeLog.warn(null, error) }
        }
    }
}
