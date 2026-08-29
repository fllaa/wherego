package app.wherego.core.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {
    fun format(money: Money): String = format(money.amountMinor, money.currency)

    fun format(amountMinor: Long, currency: String): String {
        val scale = CurrencyScale.scale(currency)
        val locale = if (currency == "IDR") Locale("id", "ID") else Locale.US
        val symbols = DecimalFormatSymbols(locale)
        val pattern = if (scale == 0) "#,###" else "#,##0." + "0".repeat(scale)
        val formatter = DecimalFormat(pattern, symbols)
        formatter.minimumFractionDigits = scale
        formatter.maximumFractionDigits = scale
        val major = amountMinor.toBigDecimal().movePointLeft(scale)
        val number = formatter.format(major)
        return when (currency) {
            "IDR" -> "Rp $number"
            "USD" -> "$$number"
            else -> "$currency $number"
        }
    }
}
