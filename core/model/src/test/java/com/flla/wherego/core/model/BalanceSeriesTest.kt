package com.flla.wherego.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The balance is anchored, not seeded: the latest `reconcile` row asserts a total, and only rows
 * dated after it still count. Rows before it are already inside that number.
 */
class BalanceSeriesTest {
    @Test
    fun anchorIsTheLatestAssertionInADeterministicOrder() {
        val early = reconcile(id = "b", on = "2026-08-01", total = 5_000_000L, createdAt = 9L)
        val late = reconcile(id = "a", on = "2026-08-20", total = 4_800_000L, createdAt = 1L)

        // Shuffled input, later date wins despite the earlier createdAt and later id.
        assertEquals("a", BalanceSeries.anchor(listOf(early, late))?.id)
        assertEquals("a", BalanceSeries.anchor(listOf(late, early))?.id)
    }

    /** Same day, so the date cannot decide: `createdAt` then `id` must, identically on any device. */
    @Test
    fun aSameDayTieBreaksOnCreatedAtThenId() {
        val first = reconcile(id = "z", on = "2026-08-20", total = 1L, createdAt = 100L)
        val second = reconcile(id = "a", on = "2026-08-20", total = 2L, createdAt = 200L)
        assertEquals("a", BalanceSeries.anchor(listOf(first, second))?.id)

        val tie = reconcile(id = "b", on = "2026-08-20", total = 3L, createdAt = 100L)
        assertEquals("z", BalanceSeries.anchor(listOf(first, tie))?.id)
    }

    @Test
    fun aDeletedAssertionDoesNotAnchor() {
        val dropped = reconcile(id = "a", on = "2026-08-20", total = 4_800_000L).copy(deletedAt = 5L)
        val kept = reconcile(id = "b", on = "2026-08-01", total = 5_000_000L)

        assertEquals("b", BalanceSeries.anchor(listOf(dropped, kept))?.id)
        assertEquals(5_000_000L, BalanceSeries.total(listOf(dropped, kept), fallbackMinor = 0L))
    }

    @Test
    fun noAssertionFallsBackToTheProfileScalar() {
        val spend = expense(on = "2026-08-05", amount = 200_000L)
        assertNull(BalanceSeries.anchor(listOf(spend)))
        assertEquals(4_800_000L, BalanceSeries.total(listOf(spend), fallbackMinor = 5_000_000L))
    }

    /**
     * The single-device bug `startingBalanceOn` was added to prevent and never read: a spend
     * backdated before the assertion is history, not a new debit.
     */
    @Test
    fun rowsBeforeTheAnchorAreAlreadyInsideItsNumber() {
        val rows = listOf(
            expense(on = "2026-08-05", amount = 200_000L),
            reconcile(id = "anchor", on = "2026-08-20", total = 5_000_000L),
            expense(on = "2026-08-25", amount = 300_000L),
        )

        assertEquals(4_700_000L, BalanceSeries.total(rows, fallbackMinor = 0L))
    }

    /** The multi-device case: two claims, the later one anchors, the earlier stays as history. */
    @Test
    fun theLaterOfTwoClaimsDecidesTheTotal() {
        val rows = listOf(
            reconcile(id = "mine", on = "2026-08-01", total = 5_000_000L),
            expense(on = "2026-08-10", amount = 200_000L),
            reconcile(id = "theirs", on = "2026-08-20", total = 4_800_000L),
            expense(on = "2026-08-25", amount = 50_000L),
        )

        assertEquals(4_750_000L, BalanceSeries.total(rows, fallbackMinor = 0L))
    }

    /**
     * Two devices each tapped `Set balance` to the same target. Under the old delta form they
     * wrote `target - localBalance`, two different numbers that stacked into a third; assertions
     * cannot stack.
     */
    @Test
    fun twoDevicesAssertingTheSameTargetLandOnThatTarget() {
        val rows = listOf(
            expense(on = "2026-08-02", amount = 100_000L),
            reconcile(id = "deviceA", on = "2026-08-20", total = 4_800_000L, createdAt = 1L),
            reconcile(id = "deviceB", on = "2026-08-20", total = 4_800_000L, createdAt = 2L),
        )

        assertEquals(4_800_000L, BalanceSeries.total(rows, fallbackMinor = 9_999_999L))
    }

    @Test
    fun legacyAdjustmentDeltasStillCount() {
        val rows = listOf(
            reconcile(id = "anchor", on = "2026-08-01", total = 1_000_000L),
            tx(kind = TransactionKind.ADJUSTMENT, on = "2026-08-02", amount = -250_000L),
        )

        assertEquals(750_000L, BalanceSeries.total(rows, fallbackMinor = 0L))
    }

    @Test
    fun anUnknownKindMovesNothing() {
        assertEquals(0L, BalanceSeries.signedBase("something-newer", 500L))
        assertEquals(0L, BalanceSeries.signedBase(TransactionKind.RECONCILE, 500L))
    }

    /**
     * Knowing the total on the 20th and every movement since the 5th also gives the total on the
     * 5th, so the month plots whole instead of starting mid-card.
     */
    @Test
    fun theSeriesBackProjectsThroughTheAnchor() {
        val rows = listOf(
            expense(on = "2026-08-05", amount = 200_000L),
            reconcile(id = "anchor", on = "2026-08-20", total = 5_000_000L),
        )

        val points = BalanceSeries.points(
            txs = rows,
            fallbackMinor = 0L,
            from = LocalDate.parse("2026-08-01"),
            to = LocalDate.parse("2026-08-31"),
        )

        assertEquals(31, points.size)
        assertEquals("before the backdated spend, the pot held 200k more", 5_200_000L, points[0].balanceMinor)
        assertEquals(5_000_000L, points[4].balanceMinor)
        assertEquals(5_000_000L, points.last().balanceMinor)
    }

    /** A window that opens after the anchor carries the movements between them into its seed. */
    @Test
    fun aLaterWindowStartsFromWhatTheAnchorImplies() {
        val rows = listOf(
            reconcile(id = "anchor", on = "2026-08-20", total = 5_000_000L),
            expense(on = "2026-08-25", amount = 300_000L),
        )

        val points = BalanceSeries.points(
            txs = rows,
            fallbackMinor = 0L,
            from = LocalDate.parse("2026-09-01"),
            to = LocalDate.parse("2026-09-03"),
        )

        assertEquals(listOf(4_700_000L, 4_700_000L, 4_700_000L), points.map { it.balanceMinor })
    }
}

private fun tx(
    kind: String,
    on: String,
    amount: Long,
    id: String = "$kind-$on-$amount",
    createdAt: Long = 1L,
): Transaction = Transaction(
    id = id,
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
    createdAt = createdAt,
    updatedAt = createdAt,
    deletedAt = null,
    dirty = false,
)

private fun expense(on: String, amount: Long): Transaction =
    tx(kind = TransactionKind.EXPENSE, on = on, amount = amount)

private fun reconcile(id: String, on: String, total: Long, createdAt: Long = 1L): Transaction =
    tx(kind = TransactionKind.RECONCILE, on = on, amount = total, id = id, createdAt = createdAt)
