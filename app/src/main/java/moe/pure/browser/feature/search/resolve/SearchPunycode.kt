package moe.pure.browser.feature.search.resolve

/** GP i6.v's RFC3492 label encoder, including its UTF-16 character iteration. */
internal object SearchPunycode {
    fun label(value: String): String? = encode(value)

    fun host(value: String): String {
        val colon = value.indexOf(':').takeIf { it >= 0 } ?: value.length
        return value.substring(0, colon).split('.').joinToString(".") { label -> encode(label) ?: label } + value.substring(colon)
    }
    private fun encode(value: String): String? {
        val text = value.replace("\u200b", "")
        val basic = text.filter { it.code < 128 }
        if (basic.length == text.length) return text
        val output = StringBuilder(basic)
        if (basic.isNotEmpty()) output.append('-')
        var handled = basic.length
        var next = 128
        var delta = 0
        var bias = 72
        while (handled < text.length) {
            val minimum = text.filter { it.code >= next }.minOfOrNull { it.code } ?: return null
            delta += (minimum - next) * (handled + 1)
            text.forEach { char ->
                if (char.code < minimum) delta++
                else if (char.code == minimum) {
                    var number = delta
                    var weight = 36
                    while (true) {
                        val threshold = when { weight <= bias -> 1; weight >= bias + 26 -> 26; else -> weight - bias }
                        if (number < threshold) break
                        output.append(digit(threshold + (number - threshold) % (36 - threshold)) ?: return null)
                        number = (number - threshold) / (36 - threshold)
                        weight += 36
                    }
                    output.append(digit(number) ?: return null)
                    handled++
                    bias = adapt(delta, handled, handled - 1 == basic.length)
                    delta = 0
                }
            }
            delta++
            next = minimum + 1
        }
        return "xn--$output"
    }
    private fun digit(number: Int): Char? = when (number) { in 0..25 -> ('a'.code + number).toChar(); in 26..35 -> (number + 22).toChar(); else -> null }
    private fun adapt(delta: Int, count: Int, first: Boolean): Int {
        var number = if (first) delta / 700 else delta / 2
        number += number / count
        var shift = 0
        while (number > 455) { number /= 35; shift += 36 }
        return shift + number * 36 / (number + 38)
    }
}
