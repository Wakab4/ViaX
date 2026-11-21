package moe.pure.browser.feature.internalpages

import android.content.Context
import moe.pure.browser.core.logging.NativeLog as Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

/** Shared raw owned-file identity and c1.Q's ordinary UTF-8 write result. */
object GeneratedPageFiles {
    fun file(context: Context, filename: String): File {
        require(filename.isNotEmpty() && filename != "." && filename != ".." && '/' !in filename && '\\' !in filename)
        return File(context.filesDir, filename)
    }

    fun url(context: Context, filename: String): String = "file://${file(context, filename).path}"
    fun exists(context: Context, filename: String): Boolean = context.filesDir.exists() && file(context, filename).exists()

    fun isPage(context: Context, url: String?, filename: String): Boolean {
        if (url == null) return false
        val owned = GeneratedPageFiles.url(context, filename)
        if (!url.startsWith(owned)) return false
        return url.length == owned.length || url[owned.length] == '?' || url[owned.length] == '#'
    }

    /** c1.Q returns true after a successful write even when close itself reports an Exception. */
    fun writeUtf8(file: File, value: String): Boolean {
        var writer: OutputStreamWriter? = null
        var written = false
        try {
            writer = OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8)
            writer.write(value)
            written = true
        } catch (error: Exception) { Log.warn("GeneratedPage", "Write generated page", error) }
        finally {
            try { writer?.close() }
            catch (error: Exception) { Log.warn("GeneratedPage", "Close generated page", error) }
        }
        return written
    }
}
