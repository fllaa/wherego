package com.flla.wherego.core.model

data class SpendRow(
    val categoryId: String,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val amountMinor: Long,
)

data class CategorySpend(
    val categoryId: String,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val amountMinor: Long,
    val percent: Int,
)

sealed interface StoryHeadline {
    data object Empty : StoryHeadline
    data class One(val name: String, val categoryId: String, val percent: Int) : StoryHeadline
    data class Two(
        val firstName: String,
        val firstCategoryId: String,
        val firstPercent: Int,
        val secondName: String,
        val secondCategoryId: String,
        val secondPercent: Int,
    ) : StoryHeadline
}

object MonthStory {
    fun aggregate(rows: List<SpendRow>): List<CategorySpend> {
        val total = rows.sumOf { it.amountMinor }
        if (total <= 0L) return emptyList()
        return rows
            .groupBy { it.categoryId }
            .map { (id, group) ->
                val amount = group.sumOf { it.amountMinor }
                val head = group.first()
                CategorySpend(
                    categoryId = id,
                    name = head.name,
                    emoji = head.emoji,
                    colorHex = head.colorHex,
                    amountMinor = amount,
                    percent = ((amount * 100L) / total).toInt(),
                )
            }
            .sortedByDescending { it.amountMinor }
    }

    fun headline(top: List<CategorySpend>): StoryHeadline = when {
        top.isEmpty() -> StoryHeadline.Empty
        top.size == 1 -> StoryHeadline.One(top[0].name, top[0].categoryId, top[0].percent)
        else -> StoryHeadline.Two(
            top[0].name, top[0].categoryId, top[0].percent,
            top[1].name, top[1].categoryId, top[1].percent,
        )
    }
}

object LogStreak {
    fun distinctDays(occurredOn: Collection<String>): Int = occurredOn.toSet().size

    fun loggedOn(occurredOn: Collection<String>, day: String): Boolean = day in occurredOn.toSet()
}

object ThemeMode {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    fun parse(raw: String?): String = when (raw) {
        LIGHT, DARK -> raw
        else -> SYSTEM
    }
}
