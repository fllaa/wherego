package com.flla.wherego.core.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object MoneyFormatter {
    /**
     * What every displayed amount reads as while `Me → Hide amounts` is on.
     *
     * A fixed run of bullets rather than a digit-for-digit mask on purpose: `Rp •.•••.•••` still
     * hands a shoulder-surfer the magnitude, which is the one thing the setting exists to withhold.
     */
    const val HIDDEN: String = "••••••"

    fun format(money: Money): String = format(money.amountMinor, money.currency)

    fun format(amountMinor: Long, currency: String): String = when (currency) {
        "IDR" -> "Rp " + number(amountMinor, currency)
        "USD" -> "$" + number(amountMinor, currency)
        else -> "$currency " + number(amountMinor, currency)
    }

    /** The grouped number without the currency prefix, e.g. `4.250.000` for IDR. */
    fun number(amountMinor: Long, currency: String): String {
        val scale = CurrencyScale.scale(currency)
        val locale = if (currency == "IDR") Locale("id", "ID") else Locale.US
        val symbols = DecimalFormatSymbols(locale)
        val pattern = if (scale == 0) "#,###" else "#,##0." + "0".repeat(scale)
        val formatter = DecimalFormat(pattern, symbols)
        formatter.minimumFractionDigits = scale
        formatter.maximumFractionDigits = scale
        return formatter.format(amountMinor.toBigDecimal().movePointLeft(scale))
    }

    fun compact(amountMinor: Long, currency: String): String {
        if (currency != "IDR") return format(amountMinor, currency)
        val sign = if (amountMinor < 0) "-" else ""
        val magnitude = abs(amountMinor)
        return when {
            magnitude >= 1_000_000L -> "${sign}Rp ${magnitude / 1_000_000}jt"
            magnitude >= 1_000L -> "${sign}Rp ${magnitude / 1_000}rb"
            else -> format(amountMinor, currency)
        }
    }
}
