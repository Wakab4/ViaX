package moe.via.browser.feature.home.render

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer

/** i6.c: opening can throw; append errors are result values that i6.o deliberately ignores. */
internal class NativeHomeFileWriter(private val file: File) : Appendable {
    private var writer: Writer? = null

    fun replace() {
        close()
        writer = BufferedWriter(OutputStreamWriter(FileOutputStream(file, false)))
    }

    private fun write(value: String): Boolean {
        if (value.isEmpty()) return true
        if (writer == null) writer = BufferedWriter(OutputStreamWriter(FileOutputStream(file, true)))
        return try {
            writer?.write(value)
            true
        } catch (_: IOException) { false }
    }

    override fun append(value: CharSequence?): Appendable {
        write(value?.toString() ?: "null")
        return this
    }

    override fun append(value: CharSequence?, start: Int, end: Int): Appendable =
        append((value ?: "null").subSequence(start, end))

    override fun append(value: Char): Appendable = append(value.toString())

    fun close() {
        val current = writer ?: return
        try { current.flush() } catch (_: IOException) { }
        try {
            current.close()
            writer = null
        } catch (_: IOException) { }
    }
}
