package com.flla.wherego.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DigitBufferTest {
    @Test
    fun appendCapsAtTwelve() {
        val raw = "123456789012"
        assertEquals(raw, DigitBuffer.append(raw, "9"))
        assertEquals("123456789012", DigitBuffer.append("12345678901", "29"))
    }

    @Test
    fun backspaceAndParse() {
        assertEquals("18", DigitBuffer.backspace("180"))
        assertEquals(0L, DigitBuffer.amountMinor(""))
        assertEquals(18000L, DigitBuffer.amountMinor("18000"))
    }
}
