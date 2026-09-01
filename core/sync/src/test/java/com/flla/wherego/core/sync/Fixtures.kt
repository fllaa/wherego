package com.flla.wherego.core.sync

import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.TransactionKind
import com.flla.wherego.core.model.UserProfile

internal fun profile(
    id: String,
    onboarded: Boolean,
    updatedAt: Long,
): UserProfile = UserProfile(
    id = id,
    googleSub = "sub",
    email = "a@b.c",
    displayName = "Aria",
    photoUrl = null,
    baseCurrency = "IDR",
    localeTag = "id-ID",
    timeZoneId = "Asia/Jakarta",
    onboardingDone = onboarded,
    startingBalanceMinor = if (onboarded) 1_000L else 0L,
    startingBalanceOn = if (onboarded) "2026-01-01" else null,
    createdAt = 1L,
    updatedAt = updatedAt,
    firebaseUid = "uid-1",
)

internal fun category(
    id: String,
    archived: Boolean,
    updatedAt: Long,
): Category = Category(
    id = id,
    name = "Food out",
    emoji = "🍜",
    colorHex = "#FF6B4A",
    softColorHex = "#FFE1D6",
    kind = "expense",
    isPreset = true,
    archived = archived,
    sortOrder = 0,
    updatedAt = updatedAt,
    deletedAt = null,
)

internal fun transaction(
    id: String,
    updatedAt: Long,
    dirty: Boolean,
): Transaction = Transaction(
    id = id,
    kind = TransactionKind.EXPENSE,
    amountMinor = 25_000L,
    currency = "IDR",
    fxRateToBase = "1",
    amountBaseMinor = 25_000L,
    categoryId = "cat_food_out",
    note = "",
    occurredOn = "2026-08-30",
    occurredAt = null,
    recurringId = null,
    receiptId = null,
    createdAt = updatedAt,
    updatedAt = updatedAt,
    deletedAt = null,
    dirty = dirty,
)
