package app.wherego.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthStoryTest {
    @Test
    fun emptyMonth() {
        assertEquals(emptyList<CategorySpend>(), MonthStory.aggregate(emptyList()))
        assertEquals("New page. Nothing parked this month.", MonthStory.sentence(emptyList()))
    }

    @Test
    fun twoCategoriesPercentAndSentence() {
        val rows = listOf(
            SpendRow("cat_food_out", "Food out", "🍜", "#FF6B4A", 18_000),
            SpendRow("cat_transport", "Transport", "🚕", "#4CA8FF", 2_000),
        )
        val agg = MonthStory.aggregate(rows)
        assertEquals(2, agg.size)
        assertEquals("cat_food_out", agg[0].categoryId)
        assertEquals(90, agg[0].percent)
        assertEquals(10, agg[1].percent)
        assertEquals(
            "Food out 90% · Transport 10% · rest is quieter.",
            MonthStory.sentence(agg),
        )
    }

    @Test
    fun oneCategorySentence() {
        val agg = MonthStory.aggregate(
            listOf(SpendRow("cat_rent", "Rent/Kos", "🏠", "#E07A5F", 1_000_000)),
        )
        assertEquals("Rent/Kos 100% · rest is quieter.", MonthStory.sentence(agg))
    }

    @Test
    fun groupsSameCategory() {
        val agg = MonthStory.aggregate(
            listOf(
                SpendRow("cat_food_out", "Food out", "🍜", "#FF6B4A", 10_000),
                SpendRow("cat_food_out", "Food out", "🍜", "#FF6B4A", 5_000),
            ),
        )
        assertEquals(1, agg.size)
        assertEquals(15_000L, agg[0].amountMinor)
        assertEquals(100, agg[0].percent)
    }
}
