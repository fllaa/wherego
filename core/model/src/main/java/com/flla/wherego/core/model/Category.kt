package com.flla.wherego.core.model

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val softColorHex: String,
    val kind: String,
    val isPreset: Boolean,
    val archived: Boolean,
    val sortOrder: Int,
    val updatedAt: Long,
    val deletedAt: Long?,
) {
    fun matches(transactionKind: String): Boolean =
        kind == "both" || kind == transactionKind
}

object CategoryKind {
    const val EXPENSE = "expense"
    const val INCOME = "income"
    const val BOTH = "both"
}
