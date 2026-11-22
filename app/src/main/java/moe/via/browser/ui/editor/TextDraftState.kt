package moe.via.browser.ui.editor

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.util.UUID

/** Keeps large editor buffers out of Activity Binder state. */
class TextDraftState(context: Context, restoredToken: String?) {
    val token = restoredToken?.takeIf { Regex("[0-9a-fA-F-]{36}").matches(it) } ?: UUID.randomUUID().toString()
    private val directory = File(context.cacheDir, "text-drafts")
    private val file = AtomicFile(File(directory, "$token.txt"))
    fun read(): String? = runCatching { file.openRead().bufferedReader().use { it.readText() } }.getOrNull()
    fun write(text: String) {
        check(directory.mkdirs() || directory.isDirectory)
        val output = file.startWrite()
        try { output.write(text.toByteArray()); file.finishWrite(output) }
        catch (error: Exception) { file.failWrite(output); throw error }
    }
    fun remove() = file.delete()
}
