package com.flla.wherego.core.model

import java.time.LocalDate
data class Budget(
    val id: String,
    val categoryId: String?,
    val amountMinor: Long,
    val currency: String,
    val yearMonth: String,
    val rollover: Boolean,
    val updatedAt: Long,
)

data class RecurringRule(
    val id: String,
    val kind: String,
    val amountMinor: Long,
    val currency: String,
    val categoryId: String,
    val note: String,
    val freq: String,
    val interval: Int,
    val dayOfMonth: Int?,
    val weekday: Int?,
    val startOn: String,
    val endOn: String?,
    val nextOn: String,
    val remindDaysBefore: Int,
    val autoPost: Boolean,
    val updatedAt: Long,
)

object Recurrence {
    const val WEEKLY = "weekly"
    const val MONTHLY = "monthly"

    fun isDue(nextOn: String, today: String, endOn: String?): Boolean {
        if (nextOn > today) return false
        if (endOn != null && nextOn > endOn) return false
        return true
    }

    fun advance(nextOn: LocalDate, freq: String, interval: Int, dayOfMonth: Int?): LocalDate {
        val step = interval.coerceAtLeast(1)
        return when (freq) {
            WEEKLY -> nextOn.plusWeeks(step.toLong())
            else -> {
                val target = nextOn.plusMonths(step.toLong())
                val dom = (dayOfMonth ?: nextOn.dayOfMonth).coerceAtMost(target.lengthOfMonth())
                target.withDayOfMonth(dom)
            }
        }
    }
}

data class BudgetBar(
    val categoryId: String?,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val spentMinor: Long,
    val capMinor: Long,
) {
    val over: Boolean get() = spentMinor > capMinor
    val remainingMinor: Long get() = capMinor - spentMinor
}

object CsvExport {
    fun table(rows: List<CsvRow>): String {
        val header = "date,kind,amount,currency,category,note"
        if (rows.isEmpty()) return header
        return buildString {
            append(header)
            rows.forEach { row ->
                append('\n')
                append(listOf(row.date, row.kind, row.amount, row.currency, row.category, escape(row.note)).joinToString(","))
            }
        }
    }

    private fun escape(value: String): String {
        if (value.none { it == ',' || it == '"' || it == '\n' }) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}

data class CsvRow(
    val date: String,
    val kind: String,
    val amount: String,
    val currency: String,
    val category: String,
    val note: String,
)

