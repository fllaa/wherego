package com.flla.wherego.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionKindTest {
    @Test
    fun expenseIsOutAndIncomeIsIn() {
        assertEquals(-1, TransactionKind.polarity(TransactionKind.EXPENSE))
        assertEquals(1, TransactionKind.polarity(TransactionKind.INCOME))
    }

    /** A legacy signed delta still moves the pot the way it always did. */
    @Test
    fun adjustmentIsIn() {
        assertEquals(1, TransactionKind.polarity(TransactionKind.ADJUSTMENT))
    }

    /**
     * A reconcile row asserts a total rather than moving money, and a kind minted by a newer
     * build is not assumed to move any either — so neither may be drawn or summed as a spend.
     */
    @Test
    fun reconcileAndUnknownKindsMoveNothing() {
        assertEquals(0, TransactionKind.polarity(TransactionKind.RECONCILE))
        assertEquals(0, TransactionKind.polarity("transfer-from-a-future-build"))
        assertEquals(0, TransactionKind.polarity(""))
    }

    /**
     * The balance arithmetic and the transaction rows read the same rule, so they cannot disagree
     * about which way a row moves money. This pins that: `signedBase` is exactly the polarity
     * applied to the amount.
     */
    @Test
    fun signedBaseIsPolarityAppliedToTheAmount() {
        val kinds = listOf(
            TransactionKind.EXPENSE,
            TransactionKind.INCOME,
            TransactionKind.ADJUSTMENT,
            TransactionKind.RECONCILE,
            "unknown",
        )
        for (kind in kinds) {
            assertEquals(
                "kind=$kind",
                TransactionKind.polarity(kind) * 25_000L,
                BalanceSeries.signedBase(kind, 25_000L),
            )
        }
    }
}
