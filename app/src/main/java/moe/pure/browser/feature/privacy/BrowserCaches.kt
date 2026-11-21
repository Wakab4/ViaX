package moe.pure.browser.feature.privacy

import android.content.Context
import java.io.File
import java.nio.file.Files

/** Removes owned cache contents without following a cache entry's symlink outside its root. */
object BrowserCaches {
    fun clearApp(context: Context) {
        val roots = listOfNotNull(context.cacheDir, context.externalCacheDir, context.codeCacheDir,
            File(context.filesDir, "chunks"), context.getExternalFilesDir("chunks"))
        roots.forEach(::clearContents)
    }
    fun clearWebEngineExtras(context: Context) {
        listOf("app_webview/Default/Service Worker/CacheStorage", "app_webview/Default/Service Worker/ScriptCache",
            "app_webview/Default/GPUCache", "app_webview/BrowserMetrics").forEach { clearContents(File(context.dataDir, it)) }
    }
    private fun clearContents(directory: File) {
        if (!directory.isDirectory) return
        val root = directory.canonicalFile.toPath()
        fun remove(file: File) {
            if (Files.isSymbolicLink(file.toPath())) { file.delete(); return }
            if (!file.canonicalFile.toPath().startsWith(root)) return
            if (file.isDirectory) file.listFiles()?.forEach(::remove)
            file.delete()
        }
        directory.listFiles()?.forEach(::remove)
    }
}
