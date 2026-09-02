package com.flla.wherego.core.model

import java.time.LocalDate

/**
 * What a slip yielded.
 *
 * Every field is independent and every one may be null, because they are not equally knowable. The
 * amount carries its own trust flag ([OcrAmount.anchored]); the rest are filled only when the slip
 * said them plainly, and left null otherwise so the caller keeps whatever the user already had.
 */
data class SlipRead(
    val amount: OcrAmount? = null,
    /** [TransactionKind.EXPENSE] or [TransactionKind.INCOME]; null when the slip does not say. */
    val kind: String? = null,
    /** The slip's own date rather than today's; null when absent or implausible. */
    val occurredOn: LocalDate? = null,
    /** Who the money went to or came from, for the note. */
    val counterparty: String? = null,
)

/**
 * Reads a payment slip — a bank or e-wallet success screen, or a till receipt.
 *
 * Runs on [OcrText.rows] rather than raw recognizer output: on a two-column slip the labels and the
 * values arrive in separate blocks, and every rule here depends on a label sitting next to the value
 * it describes.
 */
object SlipParser {
    /**
     * How far back a slip's own date is believed. A slip being logged is recent; a date beyond this
     * is a misread digit, not a genuinely old receipt, and silently backdating a row hides it from
     * the month the user is looking at.
     */
    private const val MAX_BACKDATE_DAYS = 400L

    private val incoming = Regex(
        """\b(masuk|diterima|kredit|refund|cashback|penerimaan|received|incoming)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val outgoing = Regex(
        """\b(transfer|pembayaran|pembelian|belanja|tarik|debit|keluar|payment|purchase|withdrawal)\b""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Labels naming the other party. `tujuan` is included but usually holds an account number, so
     * validation drops it and the search continues to the row that holds a name.
     */
    private val nameLabel = Regex(
        """\b(nama|penerima|merchant|toko|pedagang|tujuan|kepada)\b|payment to|bayar ke|transfer to""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Words that mark a line as a slip's own field label rather than a value.
     *
     * Needed for the label-alone-then-value-beneath layout: without geometry, the line after
     * `Nama Penerima` is whatever the recognizer happened to emit next — on a column-first read
     * that is the *next label*, `No. Ref`, which otherwise passes for a name.
     */
    private val labelish = Regex(
        """\b(no|nomor|ref|refno|referensi|rrn|trace|trx|id|waktu|tanggal|date|time|jam|status|""" +
            """sumber|dana|metode|method|biaya|admin|fee|total|subtotal|nominal|jumlah|saldo|sisa|""" +
            """rekening|norek|kartu|card|nama|penerima|tujuan|kepada|catatan|note|keterangan|""" +
            """berhasil|sukses|success|failed|gagal)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val monthNames = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "mei" to 5, "may" to 5,
        "jun" to 6, "jul" to 7, "agu" to 8, "aug" to 8, "sep" to 9,
        "okt" to 10, "oct" to 10, "nov" to 11, "des" to 12, "dec" to 12,
    )

    private val dayMonthName = Regex("""\b(\d{1,2})[ /-]([A-Za-z]{3,9})[ /-](\d{4})\b""")
    private val isoDate = Regex("""\b(\d{4})-(\d{1,2})-(\d{1,2})\b""")
    private val dayMonthNumeric = Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{4})\b""")

    private val columnGap = Regex("""\s{2,}""")

    fun parse(text: OcrText, currency: String, today: LocalDate): SlipRead {
        val rows = text.rows()
        if (rows.isBlank()) return SlipRead()
        val lines = rows.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return SlipRead(
            amount = OcrAmountParser.parse(rows, currency),
            kind = kindOf(rows),
            occurredOn = dateOf(rows, today),
            counterparty = counterpartyOf(lines),
        )
    }

    /**
     * `null` unless exactly one direction is named. `Transfer` appears on both the screen that sent
     * money and the one that received it, so a slip saying both ways is a slip that has not said.
     */
    private fun kindOf(rows: String): String? {
        val inbound = incoming.containsMatchIn(rows)
        val outbound = outgoing.containsMatchIn(rows)
        return when {
            inbound && !outbound -> TransactionKind.INCOME
            outbound && !inbound -> TransactionKind.EXPENSE
            else -> null
        }
    }

    private fun dateOf(rows: String, today: LocalDate): LocalDate? {
        val found = namedMonthDate(rows) ?: isoOrNumericDate(rows) ?: return null
        val earliest = today.minusDays(MAX_BACKDATE_DAYS)
        // A slip cannot have been printed tomorrow; a future date is a misread, and so is one from
        // before the user plausibly kept this receipt.
        return found.takeIf { !it.isAfter(today) && !it.isBefore(earliest) }
    }

    private fun namedMonthDate(rows: String): LocalDate? =
        dayMonthName.findAll(rows).firstNotNullOfOrNull { match ->
            val (day, name, year) = match.destructured
            val month = monthNames[name.lowercase().take(3)] ?: return@firstNotNullOfOrNull null
            dateOrNull(year.toInt(), month, day.toInt())
        }

    private fun isoOrNumericDate(rows: String): LocalDate? {
        isoDate.find(rows)?.destructured?.let { (year, month, day) ->
            dateOrNull(year.toInt(), month.toInt(), day.toInt())?.let { return it }
        }
        return dayMonthNumeric.findAll(rows).firstNotNullOfOrNull { match ->
            val (day, month, year) = match.destructured
            dateOrNull(year.toInt(), month.toInt(), day.toInt())
        }
    }

    private fun dateOrNull(year: Int, month: Int, day: Int): LocalDate? =
        runCatching { LocalDate.of(year, month, day) }.getOrNull()

    /**
     * The first labelled value that reads like a name.
     *
     * Two layouts are covered: `Nama Penerima   BUDI SANTOSO`, where [OcrText.rows] has already put
     * the value beside its label, and a label alone on its line with the value beneath — how QRIS
     * slips print `Payment to`.
     */
    private fun counterpartyOf(lines: List<String>): String? {
        lines.forEachIndexed { index, line ->
            if (!nameLabel.containsMatchIn(line)) return@forEachIndexed
            val beside = columnGap.split(line).takeIf { it.size > 1 }?.last()?.trim()
            val stripped = nameLabel.replace(line, "").trim().trim(':', '-', '.').trim()
            val below = lines.getOrNull(index + 1)
            val candidate = listOfNotNull(beside, stripped.takeIf { it.isNotEmpty() }, below)
                .firstOrNull { looksLikeName(it) }
            if (candidate != null) return candidate
        }
        return null
    }

    /**
     * Rejects the account numbers, reference codes and stray field labels that sit under the same
     * labels. A name is mostly letters; `BCA 1234567890` is mostly digits, `Rp 125.000` is not a
     * name at all, and `No. Ref` is the slip talking about itself.
     */
    private fun looksLikeName(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.length !in 2..40) return false
        if (trimmed.none(Char::isLetter)) return false
        if (labelish.containsMatchIn(trimmed)) return false
        val digits = trimmed.count(Char::isDigit)
        return digits * 2 < trimmed.length
    }
}
