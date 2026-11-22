package moe.via.browser.feature.library.data

import android.content.Context
import java.io.File
import java.nio.file.Files

/** The global history-data clear removes these two owned directories, then discards process cache entries. */
internal object FaviconHistoryAssets {
    fun clear(context: Context) {
        try {
            listOf("favicons", "opened").forEach { name -> removeOwnedRoot(NativeFaviconPaths.directory(context, name)) }
        } finally { NativeFaviconCache.evictAll() }
    }

    private fun removeOwnedRoot(directory: File) {
        if (Files.isSymbolicLink(directory.toPath())) { directory.delete(); return }
        if (!directory.exists()) return
        val root = directory.canonicalFile.toPath()
        fun remove(file: File) {
            if (Files.isSymbolicLink(file.toPath())) { file.delete(); return }
            if (!file.canonicalFile.toPath().startsWith(root)) return
            if (file.isDirectory) file.listFiles()?.forEach(::remove)
            file.delete()
        }
        remove(directory)
    }
}
