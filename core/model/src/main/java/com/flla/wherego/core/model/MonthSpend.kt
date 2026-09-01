package com.flla.wherego.core.model

import java.time.YearMonth

/**
 * The one month-spend rule: expenses whose `occurredOn` lands inside the month, summed in base
 * currency.
 *
 * Home's hero, Stories' total, Plan's cap card and the budget bars each shape this differently,
 * but they must never disagree on what "spent" counts — so the predicate lives here alone.
 * `TransactionDao.sumExpenses` is the same rule in SQL; soft-deleted rows are already excluded by
 * the `observeActive`/`listActive` queries that feed this.
 */
object MonthSpend {
    /** Spend per `categoryId`, first-seen order. Categories with nothing spent are absent. */
    fun byCategory(txs: List<Transaction>, month: YearMonth): Map<String, Long> {
        val sums = LinkedHashMap<String, Long>()
        forEachIn(txs, month) { tx ->
            sums[tx.categoryId] = (sums[tx.categoryId] ?: 0L) + tx.amountBaseMinor
        }
        return sums
    }

    /** Month total — the number Home's hero shows. */
    fun total(txs: List<Transaction>, month: YearMonth): Long {
        var sum = 0L
        forEachIn(txs, month) { sum += it.amountBaseMinor }
        return sum
    }

    private inline fun forEachIn(
        txs: List<Transaction>,
        month: YearMonth,
        body: (Transaction) -> Unit,
    ) {
        val start = month.atDay(1).toString()
        val end = month.atEndOfMonth().toString()
        for (tx in txs) {
            if (tx.kind != TransactionKind.EXPENSE) continue
            if (tx.occurredOn < start || tx.occurredOn > end) continue
            body(tx)
        }
    }
}
