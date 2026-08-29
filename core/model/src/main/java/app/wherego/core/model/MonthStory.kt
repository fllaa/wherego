package app.wherego.core.model

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

    fun sentence(top: List<CategorySpend>): String {
        if (top.isEmpty()) return "New page. Nothing parked this month."
        val first = top[0]
        if (top.size == 1) {
            return "${first.name} ${first.percent}% · rest is quieter."
        }
        val second = top[1]
        return "${first.name} ${first.percent}% · ${second.name} ${second.percent}% · rest is quieter."
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
