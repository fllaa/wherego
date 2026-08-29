package app.wherego.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanTest {
    @Test
    fun dueWhenNextOnTodayOrPast() {
        assertTrue(Recurrence.isDue("2026-08-12", "2026-08-12", null))
        assertTrue(Recurrence.isDue("2026-08-10", "2026-08-12", null))
        assertFalse(Recurrence.isDue("2026-08-13", "2026-08-12", null))
        assertFalse(Recurrence.isDue("2026-08-12", "2026-08-12", "2026-08-11"))
    }

    @Test
    fun advanceWeeklyAndMonthly() {
        val wed = LocalDate.parse("2026-08-12")
        assertEquals(LocalDate.parse("2026-08-19"), Recurrence.advance(wed, Recurrence.WEEKLY, 1, null))
        assertEquals(
            LocalDate.parse("2026-09-12"),
            Recurrence.advance(wed, Recurrence.MONTHLY, 1, 12),
        )
        val jan31 = LocalDate.parse("2026-01-31")
        assertEquals(
            LocalDate.parse("2026-02-28"),
            Recurrence.advance(jan31, Recurrence.MONTHLY, 1, 31),
        )
    }

    @Test
    fun csvEscapesNotes() {
        val csv = CsvExport.table(
            listOf(
                CsvRow("2026-08-12", "expense", "18000", "IDR", "Food out", "Warteg, spicy"),
            ),
        )
        assertEquals(
            "date,kind,amount,currency,category,note\n2026-08-12,expense,18000,IDR,Food out,\"Warteg, spicy\"",
            csv,
        )
    }

    @Test
    fun compactIdr() {
        assertEquals("Rp 190rb", MoneyFormatter.compact(190_000L, "IDR"))
        assertEquals("Rp 3jt", MoneyFormatter.compact(3_000_000L, "IDR"))
    }

    @Test
    fun budgetBarOverWhenSpentExceedsCap() {
        val bar = BudgetBar(null, "Overall", "📦", "#78918E", spentMinor = 120, capMinor = 100)
        assertTrue(bar.over)
        assertEquals(-20L, bar.remainingMinor)
    }
}
