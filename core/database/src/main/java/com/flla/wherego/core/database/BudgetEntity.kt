package com.flla.wherego.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flla.wherego.core.model.Budget

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String?,
    val amountMinor: Long,
    val currency: String,
    val yearMonth: String,
    val rollover: Boolean,
    val updatedAt: Long,
) {
    fun toModel(): Budget = Budget(id, categoryId, amountMinor, currency, yearMonth, rollover, updatedAt)

    companion object {
        fun from(model: Budget): BudgetEntity = BudgetEntity(
            id = model.id,
            categoryId = model.categoryId,
            amountMinor = model.amountMinor,
            currency = model.currency,
            yearMonth = model.yearMonth,
            rollover = model.rollover,
            updatedAt = model.updatedAt,
        )
    }
}
