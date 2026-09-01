package com.flla.wherego.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class S6Test {
    @Test
    fun usdToIdrUsesRateString() {
        val base = FxConvert.toBase(1_000L, "USD", "16250", "IDR")
        assertEquals(162_500L, base)
    }

    @Test
    fun sameCurrencyIgnoresRate() {
        assertEquals(18_000L, FxConvert.toBase(18_000L, "IDR", "99", "IDR"))
    }

    @Test
    fun balanceRunsFromStarting() {
        val txs = listOf(
            tx("2026-08-11", TransactionKind.EXPENSE, 5_000L),
            tx("2026-08-12", TransactionKind.INCOME, 20_000L),
            tx("2026-08-12", TransactionKind.EXPENSE, 3_000L),
        )
        val points = BalanceSeries.points(10_000L, txs, LocalDate.parse("2026-08-12"), LocalDate.parse("2026-08-12"))
        assertEquals(1, points.size)
        assertEquals(22_000L, points[0].balanceMinor)
    }

    @Test
    fun sparkNormalisesAgainstItsOwnRange() {
        val spark = BalanceSeries.spark(
            listOf(
                BalancePoint("2026-08-01", 5_000_000L),
                BalancePoint("2026-08-02", 4_975_000L),
                BalancePoint("2026-08-03", 4_950_000L),
            ),
        )!!
        assertFalse(spark.isFlat)
        assertEquals(1f, spark.fractions.first(), 0f)
        assertEquals(0.5f, spark.fractions[1], 1e-6f)
        assertEquals(0f, spark.fractions.last(), 0f)
        assertEquals(4_950_000L, spark.lowMinor)
        assertEquals(5_000_000L, spark.highMinor)
        assertEquals(4_950_000L, spark.lastMinor)
        assertNull(spark.zeroFraction)
    }

    @Test
    fun sparkFlatMonthSitsMidBox() {
        val spark = BalanceSeries.spark(
            List(3) { BalancePoint("2026-08-0${it + 1}", 5_000_000L) },
        )!!
        assertTrue(spark.isFlat)
        assertEquals(listOf(0.5f, 0.5f, 0.5f), spark.fractions)
        assertNull(spark.zeroFraction)
    }

    @Test
    fun sparkPlacesZeroWhenBalanceGoesNegative() {
        val spark = BalanceSeries.spark(
            listOf(
                BalancePoint("2026-08-01", 3_000L),
                BalancePoint("2026-08-02", -2_000L),
            ),
        )!!
        assertEquals(0.4f, spark.zeroFraction!!, 1e-6f)
    }

    @Test
    fun sparkNeedsTwoDays() {
        assertNull(BalanceSeries.spark(listOf(BalancePoint("2026-08-01", 1L))))
    }

    @Test
    fun csvImportQuotedNoteAndPreview() {
        val text = """
            date,kind,amount,currency,category,note
            2026-08-12,expense,18000,IDR,Food out,"Warteg, spicy"
            2026-08-13,income,100000,IDR,Salary,Payday
        """.trimIndent()
        val rows = CsvImport.parse(text)
        val mapping = CsvImport.guessMapping(rows.first())
        val preview = CsvImport.preview(rows, mapping, skipHeader = true, limit = 5)
        assertEquals(2, preview.size)
        assertEquals("Warteg, spicy", preview[0].note)
        assertEquals("18000", preview[0].amount)
    }

    @Test
    fun monthPdfHasTitleAndRows() {
        val lines = MonthPdf.lines(
            titleLine = "Wherego · Agustus 2026",
            totalLine = "Spent Rp 18.000",
            bars = listOf("🍜 Food out" to "Rp 18.000"),
            txs = listOf("2026-08-12  expense  Rp 18.000  Food out  Warteg"),
            emptyBars = "No category bars.",
            emptyTxs = "No transactions.",
        )
        assertEquals("Wherego · Agustus 2026", lines.first())
        assertEquals("Spent Rp 18.000", lines[1])
    }

    private fun tx(on: String, kind: String, amount: Long) = Transaction(
        id = on + kind,
        kind = kind,
        amountMinor = amount,
        currency = "IDR",
        fxRateToBase = "1",
        amountBaseMinor = amount,
        categoryId = "cat_other",
        note = "",
        occurredOn = on,
        occurredAt = null,
        recurringId = null,
        receiptId = null,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = null,
        dirty = false,
    )
}
