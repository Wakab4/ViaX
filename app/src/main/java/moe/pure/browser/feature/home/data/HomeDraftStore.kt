package moe.pure.browser.feature.home.data

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.OutputStreamWriter
import java.security.MessageDigest
import moe.pure.browser.core.files.NativeDirectories
import moe.pure.browser.core.files.NativeStreams
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.feature.home.model.HomeDraftKind

/** w9.c stores the last explicitly saved logo values, separately from an editor's live text. */
internal class HomeDraftStore(context: Context) {
    private val directory = requireNotNull(NativeDirectories.appFiles(context, "settings"))
    private fun file(kind: HomeDraftKind): File {
        val name = try {
            MessageDigest.getInstance("MD5").digest(kind.fileName.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        } catch (error: Exception) { error.printStackTrace(); kind.fileName }
        return File(directory, name)
    }
    fun read(kind: HomeDraftKind): String {
        val file = file(kind)
        if (!file.isFile) return ""
        val text = StringBuilder()
        var reader: BufferedReader? = null
        try { reader = BufferedReader(FileReader(file)); NativeStreams.appendLines(reader, text) }
        catch (error: Exception) { NativeLog.debug(null, null, error) }
        finally { NativeStreams.close(reader) }
        return text.toString()
    }
    fun write(kind: HomeDraftKind, text: String) {
        val file = file(kind)
        if (!NativeDirectories.ensure(file.parentFile)) return
        var writer: OutputStreamWriter? = null
        try { writer = OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8); writer.write(text) }
        catch (error: Exception) { NativeLog.debug(null, null, error) }
        finally { NativeStreams.close(writer) }
    }
}
