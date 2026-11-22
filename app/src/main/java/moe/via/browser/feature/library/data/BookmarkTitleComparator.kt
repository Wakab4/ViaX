package moe.via.browser.feature.library.data

import android.content.Context
import java.io.DataInputStream
import java.util.Locale

/** Native z8.a2 compares each UTF-16 character after the APK's compact z0 pinyin lookup. */
internal class BookmarkTitleComparator(context: Context) : Comparator<String> {
    private val flags: ByteArray
    private val indices: ByteArray
    private val syllables: List<String>
    init {
        DataInputStream(context.assets.open("library/native-pinyin.bin")).use { input ->
            flags = ByteArray(input.readInt()).also(input::readFully)
            indices = ByteArray(input.readInt()).also(input::readFully)
            syllables = List(input.readInt()) { input.readUTF() }
        }
    }
    override fun compare(left: String, right: String): Int {
        for (index in 0 until minOf(left.length, right.length)) {
            val compared = roman(left[index]).uppercase(Locale.ROOT).compareTo(roman(right[index]).uppercase(Locale.ROOT))
            if (compared != 0) return compared
        }
        return left.length - right.length
    }
    private fun roman(char: Char): String {
        if (char == '\u3007') return "LING"
        val index = char.code - 19968
        if (char.code !in 19968..40869 || index !in indices.indices) return char.toString()
        val high = if (flags[index / 8].toInt() and (1 shl (index % 8)) != 0) 256 else 0
        val value = (indices[index].toInt() and 255) or high
        return syllables.getOrNull(value)?.takeIf(String::isNotEmpty) ?: char.toString()
    }
}
