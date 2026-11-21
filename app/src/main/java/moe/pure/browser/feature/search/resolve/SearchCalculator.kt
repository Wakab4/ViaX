package moe.pure.browser.feature.search.resolve

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.*

object SearchCalculator {
    fun evaluate(text: String): String? {
        if (text.isBlank() || text.length > 256) return null
        return runCatching {
            val parser = Parser(text.lowercase(Locale.ROOT))
            val result = parser.expression()
            parser.space()
            if (parser.position != text.length || !parser.operated || !result.isFinite()) return null
            val decimal = BigDecimal(result, MathContext(14, RoundingMode.HALF_UP)).stripTrailingZeros()
            if (decimal.compareTo(BigDecimal.ZERO) == 0) "0" else decimal.toPlainString()
        }.getOrNull()
    }

    private class Parser(private val text: String) {
        var position = 0
        var operated = false
        fun space() { while (position < text.length && Character.isWhitespace(text[position])) position++ }
        private fun take(char: Char): Boolean { space(); return if (text.getOrNull(position) == char) { position++; true } else false }
        fun expression(): Double {
            var value = product()
            while (true) value = when { take('+') -> { operated = true; value + product() }; take('-') -> { operated = true; value - product() }; else -> return value }
        }
        private fun product(): Double {
            var value = unary()
            while (true) {
                when {
                    take('*') || take('×') -> { operated = true; value *= unary() }
                    take('/') || take('÷') -> { operated = true; val divisor = unary(); require(divisor != 0.0); value /= divisor }
                    else -> {
                        space()
                        val next = text.getOrNull(position)
                        if (next == null || !(next == '(' || next == '.' || next.isLetterOrDigit())) return value
                        require(next != '.' && !next.isDigit())
                        operated = true
                        value *= unary()
                    }
                }
            }
        }
        private fun unary(): Double = when { take('+') -> unary(); take('-') -> { operated = true; -unary() }; else -> power() }
        private fun power(): Double {
            var value = primary()
            if (take('^')) { operated = true; value = value.pow(unary()) }
            while (take('%')) { operated = true; value /= 100.0 }
            return value
        }
        private fun primary(): Double {
            space()
            if (take('(')) return expression().also { require(take(')')) }
            if (text.getOrNull(position)?.let { it.isDigit() || it == '.' } == true) {
                val start = position
                var dot = false
                while (position < text.length) {
                    val char = text[position]
                    if (char.isDigit()) position++ else if (char == '.' && !dot) { dot = true; position++ } else break
                }
                return text.substring(start, position).toDouble()
            }
            val start = position
            while (text.getOrNull(position)?.isLetter() == true) position++
            return when (val name = text.substring(start, position)) {
                "pi" -> Math.PI; "e" -> Math.E
                else -> {
                    require(name.isNotEmpty() && take('('))
                    val value = expression()
                    require(take(')'))
                    operated = true
                    when (name) {
                        "ln" -> { require(value > 0); ln(value) }; "log" -> { require(value > 0); log10(value) }
                        "sqrt" -> { require(value >= 0); sqrt(value) }; "abs" -> abs(value)
                        "sin" -> sin(value); "cos" -> cos(value); "tan" -> tan(value)
                        else -> throw IllegalArgumentException()
                    }
                }
            }
        }
    }
}
