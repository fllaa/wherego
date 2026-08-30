package com.flla.wherego.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OcrAmountTest {
    @Test
    fun largestIdrFromReceiptBlob() {
        val text = """
            INDOMARET
            12 Aug 2026
            Indomie 8.500
            Total Rp 28.000
            Tunai 50.000
            Kembali 22.000
        """.trimIndent()
        assertEquals(50_000L, OcrAmountParser.parseLargest(text, "IDR"))
    }

    @Test
    fun skipsYearWhenOtherAmountsExist() {
        val text = "Warteg 2026\nRp 18.000"
        assertEquals(18_000L, OcrAmountParser.parseLargest(text, "IDR"))
    }

    @Test
    fun millionDotGrouped() {
        assertEquals(1_250_000L, OcrAmountParser.parseLargest("Rp 1.250.000", "IDR"))
    }

    @Test
    fun emptyIsNull() {
        assertNull(OcrAmountParser.parseLargest("hello", "IDR"))
    }

    @Test
    fun usdKeepsScale() {
        assertEquals(199L, OcrAmountParser.parseLargest("Total 1.99", "USD"))
    }

    @Test
    fun scaledSizeCapsLongEdge() {
        assertEquals(1080 to 810, ReceiptImage.scaledSize(1920, 1440, 1080))
        assertEquals(800 to 600, ReceiptImage.scaledSize(800, 600, 1080))
    }
}
