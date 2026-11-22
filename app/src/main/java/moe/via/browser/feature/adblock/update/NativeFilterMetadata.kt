package moe.via.browser.feature.adblock.update

import moe.via.browser.feature.adblock.FilterSubscription
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.LineNumberReader

/** b5.c.c parses the first twenty physical lines, then stores LineNumberReader's count plus one. */
internal object NativeFilterMetadata {
    fun read(item: FilterSubscription) {
        val path = item.filePath ?: return
        val file = File(path)
        if (!file.exists()) return
        var reader: LineNumberReader? = null
        try {
            reader = LineNumberReader(FileReader(file))
            val names = arrayOf("Title:", "Homepage:", "License:")
            var remaining = 20
            while (remaining-- > 0) {
                var line = reader.readLine() ?: break
                if (line.isNotEmpty() && line[0] == '!') for (index in names.indices) {
                    val at = line.indexOf(names[index])
                    if (at >= 0) {
                        line = line.substring(at + names[index].length).trim { it <= ' ' }
                        when (index) { 0 -> item.title = line; 1 -> item.homepage = line; 2 -> item.license = line }
                    }
                }
            }
            reader.skip(Long.MAX_VALUE)
            item.count = reader.lineNumber + 1
        } catch (_: IOException) { }
        finally { try { reader?.close() } catch (_: IOException) { } }
    }
}
