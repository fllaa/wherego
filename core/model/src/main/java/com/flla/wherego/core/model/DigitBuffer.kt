package com.flla.wherego.core.model

object DigitBuffer {
    const val MAX_DIGITS = 12

    fun append(raw: String, chunk: String): String {
        if (chunk.isEmpty()) return raw
        val room = MAX_DIGITS - raw.length
        if (room <= 0) return raw
        return raw + chunk.take(room)
    }

    fun backspace(raw: String): String = raw.dropLast(1)

    fun replace(amountMinor: Long): String =
        if (amountMinor <= 0L) "" else amountMinor.toString().take(MAX_DIGITS)

    fun amountMinor(raw: String): Long = raw.toLongOrNull() ?: 0L
}
