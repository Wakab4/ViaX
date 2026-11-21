package moe.pure.browser.feature.about.diagnostics

import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.core.files.NativeDirectories
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

/** n0 uses registered file sinks, then a character cursor across 128-line batches. */
internal class DiagnosticsRepository {
    fun files(): List<File> = NativeLog.files().filter { it.exists() && it.isFile && it.length() > 0 }

    fun read(files: List<File>, cursor: DiagnosticsLogCursor): CharSequence {
        val text = StringBuilder()
        val file = files[cursor.file]
        if (!file.exists()) { cursor.file++; cursor.skipped = 0; return text }
        try {
            InputStreamReader(FileInputStream(file), "UTF-8").use { input ->
                BufferedReader(input).use { reader ->
                    cursor.skipped = reader.skip(cursor.skipped)
                    var line: String? = null
                    var count = 0
                    while (count < 128) {
                        line = reader.readLine()
                        if (line == null) break
                        cursor.skipped += (line.length + 1).toLong()
                        text.append(line).append('\n')
                        count++
                    }
                    if (line == null) { cursor.file++; cursor.skipped = 0 }
                }
            }
        } catch (error: IOException) { error.printStackTrace() }
        return text.toString()
    }

    fun delete(files: List<File>) = files.count { NativeDirectories.delete(it) }
}
