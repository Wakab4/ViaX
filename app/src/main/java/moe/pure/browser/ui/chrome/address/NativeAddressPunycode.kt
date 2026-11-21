package moe.pure.browser.ui.chrome.address

/** i6.v.c/b decodes UTF-16 characters, including its null-label and Int arithmetic behavior. */
internal object NativeAddressPunycode {
    fun decode(value: String): String {
        var cursor = value.indexOf("xn--")
        if (cursor < 0) return value
        val limit = value.indexOf('/', cursor).takeIf { it >= 0 } ?: value.length
        val output = StringBuilder(value.substring(0, cursor))
        var end: Int
        while (true) {
            val start = cursor + 4
            end = minOf(value.indexOf('.', start), limit)
            if (end < 0) end = minOf(value.indexOf(':', start), limit)
            if (end < 0) end = limit
            output.append(label(value.substring(cursor, end)))
            cursor = value.indexOf("xn--", start)
            if (cursor < 0 || cursor >= limit) break
            output.append(value.substring(end, cursor))
        }
        return output.append(value.substring(end)).toString()
    }

    private fun label(value: String): String? {
        if (value.isEmpty() || !value.startsWith("xn--")) return value
        val encoded = value.substring(4)
        val delimiter = encoded.lastIndexOf('-')
        val output: MutableList<Char> = if (delimiter >= 0) encoded.substring(0, delimiter).toMutableList() else mutableListOf()
        var cursor = (delimiter + 1).coerceAtMost(encoded.length)
        var next = 128
        var bias = 72
        var delta = 0
        while (cursor < encoded.length) {
            val previous = delta
            var weight = 1
            var step = 36
            while (true) {
                if (cursor >= encoded.length) return null
                val digit = digit(encoded[cursor++]) ?: return null
                delta += digit * weight
                val threshold = when { step <= bias -> 1; step >= bias + 26 -> 26; else -> step - bias }
                if (digit < threshold) break
                weight *= 36 - threshold
                step += 36
            }
            bias = adapt(delta - previous, output.size + 1, previous == 0)
            next += delta / (output.size + 1)
            val position = delta % (output.size + 1)
            output.add(position, next.toChar())
            delta = position + 1
        }
        return output.joinToString("")
    }

    private fun digit(value: Char): Int? = when (value) {
        in 'a'..'z' -> value - 'a'
        in 'A'..'Z' -> value - 'A'
        in '0'..'9' -> value.code - 22
        else -> null
    }

    private fun adapt(delta: Int, count: Int, first: Boolean): Int {
        var value = if (first) delta / 700 else delta / 2
        value += value / count
        var shift = 0
        while (value > 455) { value /= 35; shift += 36 }
        return shift + value * 36 / (value + 38)
    }
}
