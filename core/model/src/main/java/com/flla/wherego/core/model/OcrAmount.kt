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

/**
 * A total read off a receipt.
 *
 * [anchored] is the trust gate. `true` means the number sat on — or directly under — a line that
 * named money (`Total`, `IDR`, `Rp`), so a caller may fill it in on the user's behalf. `false`
 * means it is merely the largest number still standing once the identifiers were thrown out:
 * offer it, never apply it.
 */
data class OcrAmount(val minor: Long, val anchored: Boolean)

object OcrAmountParser {
    private val groupedDot = Regex("""\d{1,3}(?:\.\d{3})+""")
    private val groupedComma = Regex("""\d{1,3}(?:,\d{3})+""")
    private val decimalDot = Regex("""\d+\.\d{2}(?!\d)""")
    private val decimalComma = Regex("""\d+,\d{2}(?!\d)""")
    private val plain = Regex("""\d{3,12}""")

    /**
     * Lines that name money. Bare currency codes count: BCA's QRIS slip prints `IDR 10,000.00`
     * with no `Total` anywhere on it.
     */
    private val amountHint = Regex(
        """\b(total|subtotal|jumlah|nominal|amount|tagihan|bayar|harga|price|idr|rp|usd|eur|sgd|myr)\b|[${'$'}]""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Lines that name an identifier. A QRIS slip's `RRN 347260430` dwarfs its `IDR 10,000.00` by
     * four orders of magnitude, so "largest number wins" reads the reference number as the spend
     * until these are dropped. Every Indonesian bank and e-wallet slip carries one.
     */
    private val refHint = Regex(
        """\b(rrn|ref|refno|reference|referensi|trace|trx|tx|struk|invoice|inv|va|npwp|nik|batch|approval|auth|mid|tid|terminal|serial|sn|id|no|nomor|kartu|card|rekening|norek|telp|hp|phone|order)\b\.?""",
        RegexOption.IGNORE_CASE,
    )

    private enum class Role { AMOUNT, REF, NEUTRAL }

    /** An explicit amount word outranks an identifier word when one line carries both. */
    private fun roleOf(line: String): Role = when {
        amountHint.containsMatchIn(line) -> Role.AMOUNT
        refHint.containsMatchIn(line) -> Role.REF
        else -> Role.NEUTRAL
    }

    fun parse(text: String, currency: String): OcrAmount? {
        val scale = CurrencyScale.scale(currency)
        val anchored = mutableListOf<Long>()
        val loose = mutableListOf<Long>()

        // A label alone on its line owns the value beneath it — receipts stack `RRN` over its
        // digits the same way they stack `Total` over its own. A line that carries digits is a
        // value, not a dangling label, so it never lends its role onward: otherwise
        // `Total Rp 28.000` would bless the `Tunai 50.000` printed under it.
        var inherited = Role.NEUTRAL

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val own = roleOf(line)
            val hasDigits = line.any(Char::isDigit)
            val role = if (own == Role.NEUTRAL) inherited else own
            inherited = if (hasDigits) Role.NEUTRAL else own

            if (!hasDigits || role == Role.REF) continue
            collect(line, scale, if (role == Role.AMOUNT) anchored else loose)
        }

        val pool = anchored.ifEmpty { loose }
        val nonYears = pool.filterNot { isCalendarYear(it, scale) }
        val best = nonYears.ifEmpty { pool }.maxOrNull() ?: return null
        return OcrAmount(minor = best, anchored = anchored.isNotEmpty())
    }

    private fun collect(line: String, scale: Int, into: MutableList<Long>) {
        val consumed = BooleanArray(line.length)

        fun take(regex: Regex, decode: (String) -> Long?) {
            for (match in regex.findAll(line)) {
                if (match.range.any { consumed[it] }) continue
                val value = decode(match.value) ?: continue
                if (value <= 0L) continue
                into += value
                for (i in match.range) consumed[i] = true
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
            take(plain) { digits -> digits.toLongOrNull()?.let { it * pow10(scale) } }
        }
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
