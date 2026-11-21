package moe.pure.browser.core.files

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import moe.pure.browser.core.logging.NativeLog

/** c1.O returns the accumulated, line-normalized text even after a read Exception. */
internal object NativeTextFiles {
    /** c1.Q keeps a successful write result even if closing the writer later fails. */
    fun write(file: File, text: String): Boolean {
        var writer: OutputStreamWriter? = null
        try {
            writer = OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8)
            writer.write(text)
            return true
        } catch (error: Exception) { NativeLog.debug(null, null, error); return false }
        finally {
            try { writer?.close() } catch (error: Exception) { NativeLog.debug(null, null, error) }
        }
    }

    fun read(file: File?): String? {
        if (file == null || !file.isFile) return null
        val text = StringBuilder()
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(FileReader(file))
            NativeStreams.appendLines(reader, text)
        } catch (error: Exception) { error.printStackTrace() }
        finally { NativeStreams.close(reader) }
        return text.toString().trim { it <= ' ' }
    }
}
