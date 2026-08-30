package com.flla.wherego.core.model

object ReceiptImage {
    const val MAX_EDGE = 1080
    const val JPEG_QUALITY = 70

    fun scaledSize(width: Int, height: Int, maxEdge: Int = MAX_EDGE): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return 1 to 1
        val edge = maxOf(width, height)
        if (edge <= maxEdge) return width to height
        val scale = maxEdge.toDouble() / edge.toDouble()
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }
}

object OcrAmountParser {
    private val groupedDot = Regex("""\d{1,3}(?:\.\d{3})+""")
    private val groupedComma = Regex("""\d{1,3}(?:,\d{3})+""")
    private val decimalDot = Regex("""\d+\.\d{2}(?!\d)""")
    private val decimalComma = Regex("""\d+,\d{2}(?!\d)""")
    private val plain = Regex("""\d{3,12}""")

    fun parseLargest(text: String, currency: String): Long? {
        val scale = CurrencyScale.scale(currency)
        val found = mutableListOf<Long>()
        val consumed = BooleanArray(text.length)

        fun mark(range: IntRange) {
            for (i in range) if (i in consumed.indices) consumed[i] = true
        }

        fun take(regex: Regex, decode: (String) -> Long?) {
            for (match in regex.findAll(text)) {
                if (match.range.any { it in consumed.indices && consumed[it] }) continue
                val value = decode(match.value) ?: continue
                if (value <= 0L) continue
                found += value
                mark(match.range)
            }
        }

        if (scale == 0) {
            take(groupedDot) { it.replace(".", "").toLongOrNull() }
            take(groupedComma) { it.replace(",", "").toLongOrNull() }
            take(plain) { it.toLongOrNull() }
        } else {
            take(groupedComma) { groupedToMinor(it, ',', scale) }
            take(decimalDot) { decimalToMinor(it, '.', scale) }
            take(decimalComma) { decimalToMinor(it, ',', scale) }
            take(plain) { digits ->
                digits.toLongOrNull()?.let { it * pow10(scale) }
            }
        }

        val nonYears = found.filterNot { isCalendarYear(it, scale) }
        val pool = nonYears.ifEmpty { found }
        return pool.maxOrNull()
    }

    private fun isCalendarYear(amountMinor: Long, scale: Int): Boolean {
        val major = if (scale == 0) amountMinor else amountMinor / pow10(scale)
        return major in 1900L..2099L
    }

    private fun groupedToMinor(raw: String, sep: Char, scale: Int): Long? {
        val digits = raw.replace(sep.toString(), "")
        val n = digits.toLongOrNull() ?: return null
        return n * pow10(scale)
    }

    private fun decimalToMinor(raw: String, sep: Char, scale: Int): Long? {
        val parts = raw.split(sep)
        if (parts.size != 2) return null
        val whole = parts[0].toLongOrNull() ?: return null
        val frac = parts[1].padEnd(scale, '0').take(scale)
        val fracN = frac.toLongOrNull() ?: return null
        return whole * pow10(scale) + fracN
    }

    private fun pow10(n: Int): Long {
        var v = 1L
        repeat(n) { v *= 10L }
        return v
    }
}
