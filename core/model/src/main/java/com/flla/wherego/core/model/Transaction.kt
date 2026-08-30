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
    const val ADJUSTMENT = "adjustment"
}
