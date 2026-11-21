package moe.pure.browser.core.text

import android.util.Base64
import moe.pure.browser.core.files.NativeStreams
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** d1's gzip/Base64 text format preserves line normalization and close ordering. */
internal object NativeCompressedText {
    fun encode(text: String): String {
        val bytes = ByteArrayOutputStream()
        val gzip = GZIPOutputStream(bytes)
        gzip.write(text.toByteArray(Charset.defaultCharset()))
        NativeStreams.close(gzip)
        return Base64.encodeToString(bytes.toByteArray(), Base64.DEFAULT).trim { it <= ' ' }
    }

    fun decode(text: String?): String {
        if (text.isNullOrEmpty()) throw IOException("Compressed string is empty")
        var gzip: GZIPInputStream? = null
        var input: Reader? = null
        var reader: BufferedReader? = null
        try {
            gzip = GZIPInputStream(ByteArrayInputStream(Base64.decode(text, Base64.DEFAULT)))
            input = InputStreamReader(gzip)
            reader = BufferedReader(input)
            val result = StringBuilder()
            NativeStreams.appendLines(reader, result)
            return result.toString().trim { it <= ' ' }
        } finally {
            NativeStreams.close(gzip)
            NativeStreams.close(input)
            NativeStreams.close(reader)
        }
    }
}
