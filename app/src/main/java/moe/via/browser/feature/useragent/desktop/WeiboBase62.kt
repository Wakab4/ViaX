package moe.via.browser.feature.useragent.desktop

import kotlin.math.pow

/** GP i6.h.a/b/c/d uses decimal groups of seven and base-62 groups of four. */
internal object WeiboBase62 {
    private const val ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    fun decimalToShort(value: String): String {
        if (value.isEmpty()) return ""
        val groups = fromRight(value, 7)
        return buildString {
            try {
                groups.forEachIndexed { index, group ->
                    val encoded = encode(group.toLong())
                    if (index > 0 && encoded.length < 4) append("0".repeat(4 - encoded.length))
                    append(encoded)
                }
            } catch (_: NumberFormatException) {
                // DEX i6.h.c returns this builder's already accumulated groups.
            }
        }
    }

    fun shortToDecimal(value: String): String {
        if (value.isEmpty()) return value
        val groups = fromRight(value, 4)
        return buildString {
            groups.forEachIndexed { index, group ->
                val decoded = decode(group).toString()
                if (index > 0 && decoded.length < 7) append("0".repeat(7 - decoded.length))
                append(decoded)
            }
        }
    }

    private fun fromRight(value: String, size: Int): List<String> {
        val groups = ArrayList<String>()
        var end = value.length
        while (end > 0) {
            val start = end - size
            groups.add(0, value.substring(maxOf(0, start), end))
            end = start
        }
        return groups
    }

    private fun encode(input: Long): String {
        if (input == 0L) return "0"
        var value = input
        return buildString {
            while (value > 0) {
                insert(0, ALPHABET[(value % 62L).toInt()])
                value /= 62L
            }
        }
    }

    private fun decode(value: String): Long {
        var decoded = 0L
        value.forEachIndexed { index, character ->
            decoded += (ALPHABET.indexOf(character).toDouble() * 62.0.pow(value.length - index - 1)).toLong()
        }
        return decoded
    }
}
