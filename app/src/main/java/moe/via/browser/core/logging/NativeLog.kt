package moe.via.browser.core.logging

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** Application logging and the diagnostics page share the registered file sinks. */
internal object NativeLog {
    private val files = ArrayList<NativeFileLog>()

    fun initialize(context: Context): File? {
        val directory = context.getExternalFilesDir("logs") ?: File(context.filesDir, "logs")
        if (!directory.exists() && !directory.mkdirs()) return null
        val file = File(directory, "log.txt")
        synchronized(files) { files.add(NativeFileLog(file, 5)) }
        return file
    }

    fun files(): List<File> = synchronized(files) { files.map { it.file } }

    private fun record(priority: Int, tag: String?, message: String?, error: Throwable?): Int {
        var text = message?.takeIf(String::isNotEmpty)
        if (error != null) {
            val output = StringWriter(256)
            val printer = PrintWriter(output, false)
            error.printStackTrace(printer); printer.flush()
            text = if (text == null) output.toString() else "$text\n$output"
        }
        val value = text ?: return 0
        val sinks = synchronized(files) { files.toList() }
        sinks.forEach { it.record(priority, tag, value) }
        return Log.println(priority, tag, value)
    }
    fun debug(tag: String?, message: String?, error: Throwable? = null) = record(3, tag, message, error)

    fun warn(tag: String?, message: String?, error: Throwable? = null) = record(5, tag, message, error)
    fun warn(tag: String?, error: Throwable) = record(5, tag, null, error)

    fun error(tag: String?, message: String?, error: Throwable? = null) = record(6, tag, message, error)
    fun error(tag: String?, error: Throwable) = record(6, tag, null, error)
}
