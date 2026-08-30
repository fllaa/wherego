package com.flla.wherego.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {
    @Test
    fun idrZero() {
        assertEquals("Rp 0", MoneyFormatter.format(0L, "IDR"))
    }

    @Test
    fun idrEighteenThousand() {
        assertEquals("Rp 18.000", MoneyFormatter.format(18_000L, "IDR"))
    }

    @Test
    fun idrOnePointTwoFiveMillion() {
        assertEquals("Rp 1.250.000", MoneyFormatter.format(1_250_000L, "IDR"))
    }

    @Test
    fun scaleIdrZeroUsdTwo() {
        assertEquals(0, CurrencyScale.scale("IDR"))
        assertEquals(0, CurrencyScale.scale("JPY"))
        assertEquals(0, CurrencyScale.scale("KRW"))
        assertEquals(0, CurrencyScale.scale("VND"))
        assertEquals(2, CurrencyScale.scale("USD"))
        assertEquals(2, CurrencyScale.scale("SGD"))
    }

    @Test
    fun usdOneNinetyNineWaitsButScaleWorks() {
        assertEquals(2, CurrencyScale.scale("USD"))
        assertEquals("$1.99", MoneyFormatter.format(199L, "USD"))
    }
}
