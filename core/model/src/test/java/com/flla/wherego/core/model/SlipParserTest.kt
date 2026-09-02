package com.flla.wherego.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SlipParserTest {
    private val today: LocalDate = LocalDate.of(2026, 9, 2)

    private fun slip(vararg rows: Pair<String, String>): OcrText {
        val lines = mutableListOf<OcrLine>()
        var top = 300
        // Labels first, then values — the column-first order ML Kit actually emits.
        rows.forEach { (label, _) ->
            lines += OcrLine(label, left = 40, top = top, right = 400, bottom = top + 50)
            top += 100
        }
        top = 300
        rows.forEach { (_, value) ->
            lines += OcrLine(value, left = 700, top = top, right = 1040, bottom = top + 55)
            top += 100
        }
        return OcrText(lines)
    }

    private val gopayTransfer = slip(
        "Nominal Transfer" to "Rp 125.000",
        "Biaya Admin" to "Rp 2.500",
        "Total" to "Rp 127.500",
        "Rekening Tujuan" to "BCA 1234567890",
        "Nama Penerima" to "BUDI SANTOSO",
        "No. Ref" to "347260430118",
        "Waktu" to "02 Sep 2026 07:41:22",
        "Sumber Dana" to "Saldo GoPay",
        "Sisa Saldo" to "Rp 1.847.300",
    )

    /**
     * The read that started this. Measured on-device before the fix: Rp 1.847.300, the remaining
     * balance, anchored and filled in silently — 14.8x the actual transfer, one tap from the ledger.
     */
    @Test
    fun theBalanceIsNotTheSpend() {
        val read = SlipParser.parse(gopayTransfer, "IDR", today)

        assertEquals(OcrAmount(127_500L, anchored = true), read.amount)
    }

    /**
     * The case the tier ordering alone does not cover. Plenty of e-wallets print the amount large
     * and unlabelled with the remaining balance beneath it, so there is no `Total` to outrank the
     * balance — it has to be thrown out on its own account.
     */
    @Test
    fun theBalanceIsNotTheSpendWithNoTotalToOutrankIt() {
        val read = SlipParser.parse(
            slip("Pembayaran" to "Rp 50.000", "Sisa Saldo" to "Rp 2.000.000"),
            "IDR",
            today,
        )

        assertEquals(50_000L, read.amount?.minor)
    }

    /**
     * Where the tier ordering earns its keep. A till receipt's barcode digits sit on their own line
     * with no label to catch them by, and they are longer than any amount printed above. A number
     * the slip put a currency on outranks an unlabelled one however large it is.
     */
    @Test
    fun anUnlabelledLongCodeDoesNotOutrankTheAmount() {
        val read = SlipParser.parse(
            OcrText.of("INDOMARET\nRp 15.000\n123456789012"),
            "IDR",
            today,
        )

        assertEquals(15_000L, read.amount?.minor)
    }

    /**
     * `Total` over `Nominal Transfer`, because the total is the nominal plus the admin fee and the
     * total is what actually left the account.
     */
    @Test
    fun theTotalBeatsTheNominalAndTheFeeIsNeverTheSpend() {
        val read = SlipParser.parse(
            slip("Nominal" to "Rp 125.000", "Biaya Admin" to "Rp 2.500", "Total" to "Rp 127.500"),
            "IDR",
            today,
        )

        assertEquals(127_500L, read.amount?.minor)
    }

    @Test
    fun anOutgoingTransferIsAnExpense() {
        assertEquals(TransactionKind.EXPENSE, SlipParser.parse(gopayTransfer, "IDR", today).kind)
    }

    @Test
    fun moneyArrivingIsIncome() {
        val incoming = slip(
            "Uang Masuk" to "Rp 250.000",
            "Nama Pengirim" to "SITI AMINAH",
            "Waktu" to "01 Sep 2026 09:00:00",
        )

        val read = SlipParser.parse(incoming, "IDR", today)

        assertEquals(TransactionKind.INCOME, read.kind)
        assertEquals(250_000L, read.amount?.minor)
        assertEquals("SITI AMINAH", read.counterparty)
    }

    /** `Transfer` prints on the screen that sent money and the one that received it. */
    @Test
    fun aSlipNamingBothDirectionsHasNotSaid() {
        val read = SlipParser.parse(
            slip("Transfer Diterima" to "Rp 90.000"),
            "IDR",
            today,
        )

        assertNull(read.kind)
    }

    @Test
    fun theSlipsOwnDateIsUsed() {
        assertEquals(LocalDate.of(2026, 9, 2), SlipParser.parse(gopayTransfer, "IDR", today).occurredOn)
    }

    @Test
    fun indonesianAndNumericMonthsBothRead() {
        val named = slip("Tanggal" to "17 Agustus 2026")
        val numeric = slip("Tanggal" to "17/08/2026")
        val iso = slip("Tanggal" to "2026-08-17")

        assertEquals(LocalDate.of(2026, 8, 17), SlipParser.parse(named, "IDR", today).occurredOn)
        assertEquals(LocalDate.of(2026, 8, 17), SlipParser.parse(numeric, "IDR", today).occurredOn)
        assertEquals(LocalDate.of(2026, 8, 17), SlipParser.parse(iso, "IDR", today).occurredOn)
    }

    /**
     * A date the slip cannot have carried is a misread digit, and backdating a row silently hides it
     * from the month the user is looking at.
     */
    @Test
    fun anImplausibleDateIsRefused() {
        assertNull(SlipParser.parse(slip("Waktu" to "02 Sep 2027"), "IDR", today).occurredOn)
        assertNull(SlipParser.parse(slip("Waktu" to "02 Sep 1998"), "IDR", today).occurredOn)
    }

    /** `Rekening Tujuan` holds an account number, so the search goes on to the row with a name. */
    @Test
    fun theAccountNumberIsNotTheName() {
        assertEquals("BUDI SANTOSO", SlipParser.parse(gopayTransfer, "IDR", today).counterparty)
    }

    /**
     * A QRIS slip prints `Payment to` on its own line with the merchant beneath, and carries no
     * `Total` at all — the amount is anchored only by its currency code.
     */
    @Test
    fun aQrisSlipReadsTheMerchantBeneathItsLabel() {
        val qris = OcrText.of(
            """
            BCA
            QRIS Payment Successful
            01 Sep 2026 12:15:20
            IDR 10,000.00
            Payment to
            Batagor cilok, CGK
            Acquirer
            GOPAY
            RRN
            347260430
            """.trimIndent(),
        )

        val read = SlipParser.parse(qris, "IDR", today)

        assertEquals(OcrAmount(10_000L, anchored = true), read.amount)
        assertEquals(TransactionKind.EXPENSE, read.kind)
        assertEquals(LocalDate.of(2026, 9, 1), read.occurredOn)
        assertEquals("Batagor cilok, CGK", read.counterparty)
    }

    /**
     * Without geometry the line after `Nama Penerima` is the *next label*, not the name, because the
     * recognizer read the label column straight through. Nothing is better than `No. Ref` in a note.
     */
    @Test
    fun aFieldLabelIsNeverMistakenForAName() {
        val columnFirstFlat = OcrText.of("Nama Penerima\nNo. Ref\nWaktu\nBUDI SANTOSO\n347260430118")

        assertNull(SlipParser.parse(columnFirstFlat, "IDR", today).counterparty)
    }

    @Test
    fun aPhotoOfNothingYieldsNothing() {
        val read = SlipParser.parse(OcrText.EMPTY, "IDR", today)

        assertNull(read.amount)
        assertNull(read.kind)
        assertNull(read.occurredOn)
        assertNull(read.counterparty)
    }
}
