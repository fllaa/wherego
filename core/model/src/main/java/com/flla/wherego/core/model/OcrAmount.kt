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
     * Lines that name the amount the transaction is *for*.
     *
     * `total` outranks the rest deliberately: on a transfer slip the total is the nominal plus the
     * admin fee, and the total is what actually left the account.
     */
    private val transactionHint = Regex(
        """\b(total|subtotal|jumlah|nominal|amount|tagihan|bayar|harga|price)\b""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Lines carrying a currency token but nothing saying *which* amount they are. Bare codes count:
     * BCA's QRIS slip prints `IDR 10,000.00` with no `Total` anywhere on it.
     */
    private val moneyHint = Regex(
        """\b(idr|rp|usd|eur|sgd|myr)\b|[${'$'}]""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * A pot, not a movement. This is the line that made a Rp 125.000 transfer read as Rp 1.847.300:
     * a bank slip prints the remaining balance in the same column, same `Rp`, an order of magnitude
     * larger, so "largest anchored number wins" picked it every time.
     */
    private val balanceHint = Regex(
        """\b(saldo|sisa|balance|limit|tersedia|available)\b""",
        RegexOption.IGNORE_CASE,
    )

    /** Charged on top of the spend rather than being it. */
    private val feeHint = Regex(
        """\b(biaya|admin|fee|pajak|ppn|ongkir|charge)\b""",
        RegexOption.IGNORE_CASE,
    )

    /** Handed over and handed back at a till: neither is the spend. */
    private val tenderHint = Regex(
        """\b(tunai|cash|kembali|kembalian|change)\b""",
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

    private enum class Role {
        TRANSACTION,
        MONEY,
        BALANCE,
        FEE,
        TENDER,
        REF,
        NEUTRAL,
        ;

        /**
         * Whether the word on the line settles the matter. A bare currency token does not — it says
         * "this is money", not which money — so a strong label on the line above may still claim it.
         */
        val decisive: Boolean get() = this != MONEY && this != NEUTRAL
    }

    /** First match wins, so an explicit label outranks a bare currency token on the same line. */
    private fun roleOf(line: String): Role = when {
        transactionHint.containsMatchIn(line) -> Role.TRANSACTION
        balanceHint.containsMatchIn(line) -> Role.BALANCE
        feeHint.containsMatchIn(line) -> Role.FEE
        tenderHint.containsMatchIn(line) -> Role.TENDER
        refHint.containsMatchIn(line) -> Role.REF
        moneyHint.containsMatchIn(line) -> Role.MONEY
        else -> Role.NEUTRAL
    }

    fun parse(text: String, currency: String): OcrAmount? {
        val scale = CurrencyScale.scale(currency)
        val labelled = mutableListOf<Long>()
        val money = mutableListOf<Long>()
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
            // A bare `Rp 10.000` under a `Total` is that total, so a decisive label above beats a
            // mere currency token here.
            val role = when {
                own.decisive -> own
                inherited.decisive -> inherited
                else -> own
            }
            inherited = if (hasDigits) Role.NEUTRAL else own

            if (!hasDigits) continue
            when (role) {
                Role.TRANSACTION -> collect(line, scale, labelled)
                Role.MONEY -> collect(line, scale, money)
                Role.NEUTRAL -> collect(line, scale, loose)
                // A balance, a fee, change owed, an invoice number: none of them is ever the spend,
                // so they are dropped outright rather than merely outranked.
                Role.BALANCE, Role.FEE, Role.TENDER, Role.REF -> Unit
            }
        }

        // Tiers, not one big pile: a number the slip actually labelled as the transaction amount
        // beats an unlabelled one however large, which is the difference between reading the
        // transfer and reading the balance printed beneath it.
        val pool = when {
            labelled.isNotEmpty() -> labelled
            money.isNotEmpty() -> money
            else -> loose
        }
        val nonYears = pool.filterNot { isCalendarYear(it, scale) }
        val best = nonYears.ifEmpty { pool }.maxOrNull() ?: return null
        return OcrAmount(minor = best, anchored = labelled.isNotEmpty() || money.isNotEmpty())
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
