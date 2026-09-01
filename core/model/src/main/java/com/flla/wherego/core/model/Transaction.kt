package com.flla.wherego.core.model

data class Transaction(
    val id: String,
    val kind: String,
    val amountMinor: Long,
    val currency: String,
    val fxRateToBase: String,
    val amountBaseMinor: Long,
    val categoryId: String,
    val note: String,
    val occurredOn: String,
    val occurredAt: Long?,
    val recurringId: String?,
    val receiptId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val dirty: Boolean,
)

object TransactionKind {
    const val EXPENSE = "expense"
    const val INCOME = "income"

    /**
     * The older correction form: a signed delta computed from one device's view of the balance.
     * Two devices replicating deltas stack them and land on a total neither one asked for, so
     * nothing writes one any more. The kind stays for rows already on disk.
     */
    const val ADJUSTMENT = "adjustment"

    /**
     * An assertion rather than a movement: "as of `occurredOn`, everything totalled
     * `amountMinor`". Assertions do not stack — the latest one anchors the balance and the rest
     * stay as history — which is what lets two devices agree without coordinating.
     */
    const val RECONCILE = "reconcile"

    /**
     * Whether the row is something the user logged. A reconcile row is bookkeeping: it must not
     * feed the streak, the week's count or the month's tally, or onboarding would hand out a
     * one-day streak for typing an opening balance.
     */
    fun isActivity(kind: String): Boolean = kind != RECONCILE
}
