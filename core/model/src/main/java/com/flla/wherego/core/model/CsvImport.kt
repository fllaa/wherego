package com.flla.wherego.core.model

data class CsvMapping(
    val date: Int = 0,
    val kind: Int = 1,
    val amount: Int = 2,
    val currency: Int = 3,
    val category: Int = 4,
    val note: Int = 5,
)

object CsvImport {
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }
                inQuotes && c == '"' -> inQuotes = false
                !inQuotes && c == '"' -> inQuotes = true
                !inQuotes && c == ',' -> {
                    row.add(field.toString())
                    field.clear()
                }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(field.toString())
                    field.clear()
                    if (row.any { it.isNotEmpty() }) rows.add(row.toList())
                    row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            if (row.any { it.isNotEmpty() }) rows.add(row)
        }
        return rows
    }

    fun guessMapping(header: List<String>): CsvMapping {
        fun idx(vararg names: String): Int {
            val lower = header.map { it.trim().lowercase() }
            names.forEach { name ->
                val at = lower.indexOf(name)
                if (at >= 0) return at
            }
            return -1
        }
        return CsvMapping(
            date = idx("date", "occurredon").takeIf { it >= 0 } ?: 0,
            kind = idx("kind", "type").takeIf { it >= 0 } ?: 1,
            amount = idx("amount", "amountminor").takeIf { it >= 0 } ?: 2,
            currency = idx("currency").takeIf { it >= 0 } ?: 3,
            category = idx("category").takeIf { it >= 0 } ?: 4,
            note = idx("note").takeIf { it >= 0 } ?: 5,
        )
    }

    fun apply(rows: List<List<String>>, mapping: CsvMapping, skipHeader: Boolean): List<CsvRow> {
        val data = if (skipHeader) rows.drop(1) else rows
        return data.mapNotNull { cells ->
            fun col(i: Int): String = cells.getOrNull(i)?.trim().orEmpty()
            val date = col(mapping.date)
            val amount = col(mapping.amount)
            if (date.isEmpty() && amount.isEmpty()) return@mapNotNull null
            CsvRow(
                date = date,
                kind = col(mapping.kind).ifBlank { TransactionKind.EXPENSE },
                amount = amount,
                currency = col(mapping.currency).ifBlank { UserProfile.DEFAULT_CURRENCY },
                category = col(mapping.category),
                note = col(mapping.note),
            )
        }
    }

    fun preview(rows: List<List<String>>, mapping: CsvMapping, skipHeader: Boolean, limit: Int = 5): List<CsvRow> =
        apply(rows, mapping, skipHeader).take(limit)
}

object MonthPdf {
    fun lines(
        title: String,
        totalLabel: String,
        bars: List<Pair<String, String>>,
        txs: List<String>,
    ): List<String> {
        val out = mutableListOf("Wherego · $title", "Spent $totalLabel", "")
        if (bars.isEmpty()) {
            out += "No category bars."
        } else {
            bars.forEach { (name, amount) -> out += "$name    $amount" }
        }
        out += ""
        if (txs.isEmpty()) {
            out += "No transactions."
        } else {
            txs.forEach { out += it }
        }
        return out
    }
}
