package moe.pure.browser.core.logging

import android.content.Context
import moe.pure.browser.core.app.NativeVersionMarker
import moe.pure.browser.core.files.NativeDirectories
import moe.pure.browser.data.BrowserExecutors
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
