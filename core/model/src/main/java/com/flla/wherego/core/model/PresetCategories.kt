package com.flla.wherego.core.model

data class PresetCategory(
    val id: String,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val softColorHex: String,
    val kind: String,
    val sortOrder: Int,
)

data class CategoryPack(
    val id: String,
    val label: String,
    val categoryIds: List<String>,
)

object PresetCategories {
    const val ACCENT_HEX = "#2157C7"
    const val ACCENT_SOFT_HEX = "#D7E3F8"

    val all: List<PresetCategory> = listOf(
        PresetCategory("cat_food_out", "Food out", "🍜", ACCENT_HEX, ACCENT_SOFT_HEX, CategoryKind.EXPENSE, 0),
        PresetCategory("cat_groceries", "Groceries", "🛒", ACCENT_HEX, ACCENT_SOFT_HEX, CategoryKind.EXPENSE, 1),
        PresetCategory("cat_transport", "Transport", "🚕", ACCENT_HEX, ACCENT_SOFT_HEX, CategoryKind.EXPENSE, 2),
        PresetCategory("cat_rent", "Rent & bills", "🏠", ACCENT_HEX, ACCENT_SOFT_HEX, CategoryKind.EXPENSE, 3),
        PresetCategory("cat_fun", "Fun", "🎬", ACCENT_HEX, ACCENT_SOFT_HEX, CategoryKind.EXPENSE, 4),
        PresetCategory("cat_shopping", "Shopping", "🛍️", ACCENT_HEX, ACCENT_SOFT_HEX, CategoryKind.EXPENSE, 5),
        PresetCategory("cat_health", "Health", "💊", ACCENT_HEX, ACCENT_SOFT_HEX, CategoryKind.EXPENSE, 6),
        PresetCategory("cat_gifts", "Gifts", "🎁", ACCENT_HEX, ACCENT_SOFT_HEX, CategoryKind.EXPENSE, 7),
        PresetCategory("cat_other", "Other", "✨", ACCENT_HEX, ACCENT_SOFT_HEX, CategoryKind.EXPENSE, 8),
        PresetCategory("cat_bills", "Bills", "📄", ACCENT_HEX, ACCENT_SOFT_HEX, CategoryKind.EXPENSE, 9),
        PresetCategory("cat_salary", "Salary", "💼", ACCENT_HEX, ACCENT_SOFT_HEX, CategoryKind.INCOME, 10),
        PresetCategory("cat_side", "Side hustle", "🛠️", ACCENT_HEX, ACCENT_SOFT_HEX, CategoryKind.INCOME, 11),
        PresetCategory("cat_refund", "Refund", "↩️", ACCENT_HEX, ACCENT_SOFT_HEX, CategoryKind.INCOME, 12),
        PresetCategory("cat_other_in", "Other in", "✨", ACCENT_HEX, ACCENT_SOFT_HEX, CategoryKind.INCOME, 13),
    )

    val expense: List<PresetCategory> = all.filter { it.kind == CategoryKind.EXPENSE }

    /**
     * Starter packs offered by `pencil-new.pen` → `Onboarding 3 · Categories`.
     * A pack is a preselection, not a taxonomy: every preset is still seeded, the
     * ones left unticked are archived so they stay recoverable from Me → Categories.
     */
    val packs: List<CategoryPack> = listOf(
        CategoryPack(
            "everyday_id",
            "Everyday ID",
            listOf("cat_food_out", "cat_groceries", "cat_transport", "cat_rent", "cat_fun"),
        ),
        CategoryPack(
            "minimal",
            "Minimal",
            listOf("cat_food_out", "cat_transport", "cat_other"),
        ),
    )

    const val CUSTOM_PACK_ID = "custom"
    const val CUSTOM_PACK_LABEL = "Custom"

    fun softHex(id: String): String =
        all.firstOrNull { it.id == id }?.softColorHex ?: ACCENT_SOFT_HEX

    fun strongHex(id: String): String =
        all.firstOrNull { it.id == id }?.colorHex ?: ACCENT_HEX
}
