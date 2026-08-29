package app.wherego.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogStreakTest {
    @Test
    fun distinctCalendarDays() {
        val days = listOf("2026-08-10", "2026-08-12", "2026-08-10", "2026-08-11")
        assertEquals(3, LogStreak.distinctDays(days))
        assertTrue(LogStreak.loggedOn(days, "2026-08-12"))
        assertFalse(LogStreak.loggedOn(days, "2026-08-13"))
    }

    @Test
    fun emptyIsZero() {
        assertEquals(0, LogStreak.distinctDays(emptyList()))
        assertFalse(LogStreak.loggedOn(emptyList(), "2026-08-12"))
    }
}
