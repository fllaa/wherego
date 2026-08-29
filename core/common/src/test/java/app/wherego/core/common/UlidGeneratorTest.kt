package app.wherego.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UlidGeneratorTest {
    private val crockford = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toSet()

    @Test
    fun nextIs26CharCrockford() {
        val id = UlidGenerator().next()
        assertEquals(26, id.length)
        assertTrue(id.all { it in crockford })
    }

    @Test
    fun nextIsUnique() {
        val gen = UlidGenerator()
        assertNotEquals(gen.next(), gen.next())
    }
}
