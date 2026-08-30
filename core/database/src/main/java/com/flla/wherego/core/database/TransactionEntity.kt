package com.flla.wherego.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flla.wherego.core.model.Transaction

@Entity(
    tableName = "transactions",
    indices = [
        Index("occurredOn"),
        Index("dirty"),
        Index("deletedAt"),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
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
) {
    fun toModel(): Transaction = Transaction(
        id = id,
        kind = kind,
        amountMinor = amountMinor,
        currency = currency,
        fxRateToBase = fxRateToBase,
        amountBaseMinor = amountBaseMinor,
        categoryId = categoryId,
        note = note,
        occurredOn = occurredOn,
        occurredAt = occurredAt,
        recurringId = recurringId,
        receiptId = receiptId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        dirty = dirty,
    )

    companion object {
        fun from(model: Transaction): TransactionEntity = TransactionEntity(
            id = model.id,
            kind = model.kind,
            amountMinor = model.amountMinor,
            currency = model.currency,
            fxRateToBase = model.fxRateToBase,
            amountBaseMinor = model.amountBaseMinor,
            categoryId = model.categoryId,
            note = model.note,
            occurredOn = model.occurredOn,
            occurredAt = model.occurredAt,
            recurringId = model.recurringId,
            receiptId = model.receiptId,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
            deletedAt = model.deletedAt,
            dirty = model.dirty,
        )
    }
}
