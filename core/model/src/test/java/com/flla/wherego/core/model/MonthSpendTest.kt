package com.flla.wherego.core.model

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthSpendTest {
    private val august = YearMonth.of(2026, 8)

    @Test
    fun monthBoundariesAreInclusive() {
        val txs = listOf(
            tx("2026-07-31", 1),
            tx("2026-08-01", 10),
            tx("2026-08-31", 100),
            tx("2026-09-01", 1_000),
        )
        assertEquals(110L, MonthSpend.total(txs, august))
    }

    @Test
    fun onlyExpensesCount() {
        val txs = listOf(
            tx("2026-08-12", 10),
            tx("2026-08-12", 500, kind = TransactionKind.INCOME),
            tx("2026-08-12", 700, kind = TransactionKind.ADJUSTMENT),
        )
        assertEquals(10L, MonthSpend.total(txs, august))
        assertEquals(mapOf("cat_food_out" to 10L), MonthSpend.byCategory(txs, august))
    }

    /** Foreign-currency logs must count in base, or each screen would show its own number. */
    @Test
    fun sumsBaseCurrencyNotEnteredAmount() {
        val txs = listOf(tx("2026-08-12", amountBaseMinor = 150_000, amountMinor = 10))
        assertEquals(150_000L, MonthSpend.total(txs, august))
    }

    @Test
    fun byCategoryGroupsAndAgreesWithTotal() {
        val txs = listOf(
            tx("2026-08-02", 10_000, categoryId = "cat_food_out"),
            tx("2026-08-09", 5_000, categoryId = "cat_food_out"),
            tx("2026-08-11", 2_000, categoryId = "cat_transport"),
            tx("2026-07-11", 9_999, categoryId = "cat_transport"),
        )
        val byCategory = MonthSpend.byCategory(txs, august)
        assertEquals(mapOf("cat_food_out" to 15_000L, "cat_transport" to 2_000L), byCategory)
        assertEquals(MonthSpend.total(txs, august), byCategory.values.sum())
    }

    @Test
    fun emptyMonth() {
        assertEquals(0L, MonthSpend.total(emptyList(), august))
        assertEquals(emptyMap<String, Long>(), MonthSpend.byCategory(emptyList(), august))
    }

    private fun tx(
        occurredOn: String,
        amountBaseMinor: Long,
        kind: String = TransactionKind.EXPENSE,
        categoryId: String = "cat_food_out",
        amountMinor: Long = amountBaseMinor,
    ) = Transaction(
        id = "$occurredOn-$categoryId-$amountBaseMinor",
        kind = kind,
        amountMinor = amountMinor,
        currency = "IDR",
        fxRateToBase = "1",
        amountBaseMinor = amountBaseMinor,
        categoryId = categoryId,
        note = "",
        occurredOn = occurredOn,
        occurredAt = null,
        recurringId = null,
        receiptId = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
        dirty = false,
    )
}
