package moe.via.browser.feature.about.diagnostics

import moe.via.browser.core.archive.NativeZipWriter
import moe.via.browser.core.logging.NativeLog
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Locale

internal object DiagnosticsExport {
    fun target(logs: List<File>) = File(logs[0].parentFile,
        "via-log-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(System.currentTimeMillis()) + ".zip")

    fun create(target: File, info: String, logs: List<File>): Boolean {
        val file = File(logs[0].parentFile, "info.txt")
        writeInfo(file, info)
        return NativeZipWriter.write(target, ArrayList(logs).apply { add(file) })
    }

    private fun writeInfo(file: File, info: String) {
        var writer: OutputStreamWriter? = null
        try {
            writer = OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8)
            writer.write(info)
        } catch (error: Exception) { NativeLog.debug(null, null, error) }
        finally { try { writer?.close() } catch (error: Exception) { NativeLog.debug(null, null, error) } }
    }
}
