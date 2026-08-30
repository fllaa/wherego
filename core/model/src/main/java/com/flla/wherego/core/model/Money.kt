package com.flla.wherego.core.model

data class Money(
    val amountMinor: Long,
    val currency: String,
)

object CurrencyScale {
    fun scale(code: String): Int = when (code) {
        "IDR", "JPY", "KRW", "VND" -> 0
        else -> 2
    }
}
