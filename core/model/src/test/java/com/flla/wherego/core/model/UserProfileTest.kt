package com.flla.wherego.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class UserProfileTest {
    @Test
    fun guestUsesLockedDefaults() {
        val profile = UserProfile.guest(id = "01TESTID", nowMillis = 42L)
        assertEquals("01TESTID", profile.id)
        assertEquals(UserProfile.DEFAULT_CURRENCY, profile.baseCurrency)
        assertEquals("IDR", profile.baseCurrency)
        assertEquals(UserProfile.DEFAULT_LANGUAGE, profile.localeTag)
        assertEquals(UserProfile.DEFAULT_ZONE, profile.timeZoneId)
        assertEquals("Asia/Jakarta", profile.timeZoneId)
        assertFalse(profile.onboardingDone)
        assertEquals(0L, profile.startingBalanceMinor)
        assertNull(profile.startingBalanceOn)
        assertNull(profile.googleSub)
        assertNull(profile.firebaseUid)
        assertEquals(42L, profile.createdAt)
        assertEquals(42L, profile.updatedAt)
    }
}
