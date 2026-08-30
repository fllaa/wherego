package com.flla.wherego

import com.flla.wherego.core.common.UlidGenerator
import com.flla.wherego.core.model.UserProfile
import com.flla.wherego.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuestProfileDefaultsTest {
    @Test
    fun guestUsesJakartaIdrAndLocalId() {
        val profile = UserProfile.guest(id = "01GUEST", nowMillis = 1L)
        assertEquals("IDR", profile.baseCurrency)
        assertEquals(AppLanguage.SYSTEM, profile.localeTag)
        assertEquals("Asia/Jakarta", profile.timeZoneId)
        assertFalse(profile.onboardingDone)
        assertEquals(0L, profile.startingBalanceMinor)
    }

    @Test
    fun ulidIsCrockford26() {
        val id = UlidGenerator().next()
        assertEquals(26, id.length)
        assertTrue(id.all { it in "0123456789ABCDEFGHJKMNPQRSTVWXYZ" })
    }
}
