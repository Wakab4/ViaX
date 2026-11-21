package moe.pure.browser.feature.adblock.ui

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.IOException

/** c1.t preserves every nonempty line, including whitespace and comments. */
internal object FilterPreviewLines {
    fun read(path: String?): List<String> {
        val lines = ArrayList<String>()
        if (!path.isNullOrEmpty()) {
            val file = File(path)
            if (file.exists() && !file.isDirectory) try {
                val input = FileInputStream(file)
                val decoder = InputStreamReader(input)
                val reader = BufferedReader(decoder)
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isNotEmpty()) lines.add(line)
                }
                reader.close(); decoder.close(); input.close()
            } catch (_: IOException) { }
        }
        return lines
    }
}
