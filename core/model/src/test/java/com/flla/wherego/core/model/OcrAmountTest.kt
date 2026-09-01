package com.flla.wherego.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OcrAmountTest {
    /**
     * A BCA QRIS success screen, status bar included, as ML Kit concatenates its blocks. The
     * `RRN` is nine digits and no calendar year, so "largest number wins" read it as a
     * Rp 347.260.430 lunch until the reference lines were dropped.
     */
    private val bcaQris = """
        16:59
        7.16 KB/s
        94
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
        View Details
        Done
    """.trimIndent()

    @Test
    fun qrisSlipReadsTotalNotReferenceNumber() {
        assertEquals(OcrAmount(10_000L, anchored = true), OcrAmountParser.parse(bcaQris, "IDR"))
    }

    @Test
    fun totalBeatsCashTenderedAndChange() {
        // `Tunai` is what the customer handed over and `Kembali` is the change; neither is the
        // spend. The `Total` line anchors, and a line carrying its own digits does not lend its
        // role to the line beneath it, so 50.000 stays unanchored.
        val text = """
            INDOMARET
            12 Aug 2026
            Indomie 8.500
            Total Rp 28.000
            Tunai 50.000
            Kembali 22.000
        """.trimIndent()
        assertEquals(OcrAmount(28_000L, anchored = true), OcrAmountParser.parse(text, "IDR"))
    }

    @Test
    fun labelOnItsOwnLineOwnsTheValueBelow() {
        assertEquals(
            OcrAmount(10_000L, anchored = true),
            OcrAmountParser.parse("Total\nRp 10.000", "IDR"),
        )
    }

    @Test
    fun referenceLabelOnItsOwnLineDisownsTheValueBelow() {
        // Dropped outright, not merely outranked: filling nothing beats filling an invoice number.
        assertNull(OcrAmountParser.parse("No. Ref\n347260430", "IDR"))
        assertNull(OcrAmountParser.parse("RRN 347260430\nTrace 998877", "IDR"))
    }

    @Test
    fun unlabelledNumberIsOnlyAGuess() {
        assertEquals(OcrAmount(25_000L, anchored = false), OcrAmountParser.parse("25.000", "IDR"))
    }

    @Test
    fun skipsYearWhenOtherAmountsExist() {
        assertEquals(
            OcrAmount(18_000L, anchored = true),
            OcrAmountParser.parse("Warteg 2026\nRp 18.000", "IDR"),
        )
    }

    @Test
    fun millionDotGrouped() {
        assertEquals(
            OcrAmount(1_250_000L, anchored = true),
            OcrAmountParser.parse("Rp 1.250.000", "IDR"),
        )
    }

    @Test
    fun emptyIsNull() {
        assertNull(OcrAmountParser.parse("hello", "IDR"))
    }

    @Test
    fun usdKeepsScale() {
        assertEquals(OcrAmount(199L, anchored = true), OcrAmountParser.parse("Total 1.99", "USD"))
    }

    @Test
    fun scaledSizeCapsLongEdge() {
        assertEquals(1080 to 810, ReceiptImage.scaledSize(1920, 1440, 1080))
        assertEquals(800 to 600, ReceiptImage.scaledSize(800, 600, 1080))
    }
}
