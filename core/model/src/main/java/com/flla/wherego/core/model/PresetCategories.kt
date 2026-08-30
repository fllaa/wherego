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
    val all: List<PresetCategory> = listOf(
        PresetCategory("cat_food_out", "Food out", "🍜", "#FF6B4A", "#FFE1D8", CategoryKind.EXPENSE, 0),
        PresetCategory("cat_groceries", "Groceries", "🛒", "#0A7F70", "#DAF6E9", CategoryKind.EXPENSE, 1),
        PresetCategory("cat_transport", "Transport", "🚕", "#4CA8FF", "#DBECFF", CategoryKind.EXPENSE, 2),
        PresetCategory("cat_rent", "Rent & bills", "🏠", "#E07A5F", "#FFEECC", CategoryKind.EXPENSE, 3),
        PresetCategory("cat_fun", "Fun", "🎬", "#8B7CF6", "#E7E3FE", CategoryKind.EXPENSE, 4),
        PresetCategory("cat_shopping", "Shopping", "🛍️", "#E85A9B", "#FFDFEC", CategoryKind.EXPENSE, 5),
        PresetCategory("cat_health", "Health", "💊", "#2A9D8F", "#DAF6E9", CategoryKind.EXPENSE, 6),
        PresetCategory("cat_gifts", "Gifts", "🎁", "#F2A7C3", "#FFDFEC", CategoryKind.EXPENSE, 7),
        PresetCategory("cat_other", "Other", "✨", "#78918E", "#EDE4D5", CategoryKind.EXPENSE, 8),
        PresetCategory("cat_bills", "Bills", "📄", "#C4A574", "#F5EFE4", CategoryKind.EXPENSE, 9),
        PresetCategory("cat_salary", "Salary", "💼", "#10B5A0", "#D5F4EE", CategoryKind.INCOME, 10),
        PresetCategory("cat_side", "Side hustle", "🛠️", "#E09F3E", "#FFEECC", CategoryKind.INCOME, 11),
        PresetCategory("cat_refund", "Refund", "↩️", "#4CA8FF", "#DBECFF", CategoryKind.INCOME, 12),
        PresetCategory("cat_other_in", "Other in", "✨", "#8B7CF6", "#E7E3FE", CategoryKind.INCOME, 13),
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
        all.firstOrNull { it.id == id }?.softColorHex ?: "#EDE4D5"

    fun strongHex(id: String): String =
        all.firstOrNull { it.id == id }?.colorHex ?: "#78918E"
}
