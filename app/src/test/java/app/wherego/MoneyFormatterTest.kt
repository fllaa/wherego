package app.wherego

import app.wherego.core.model.CurrencyScale
import app.wherego.core.model.MoneyFormatter
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
    fun scaleHelper() {
        assertEquals(0, CurrencyScale.scale("IDR"))
        assertEquals(2, CurrencyScale.scale("USD"))
    }
}
