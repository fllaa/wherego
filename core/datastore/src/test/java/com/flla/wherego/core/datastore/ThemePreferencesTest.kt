package com.flla.wherego.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemePreferencesTest {
    private lateinit var preferences: ThemePreferences

    @Before
    fun setUp() = runBlocking {
        preferences = ThemePreferences(ApplicationProvider.getApplicationContext<Context>())
        preferences.clear()
    }

    /**
     * Amounts are visible until asked otherwise. A privacy guard that defaults on would leave a
     * new install looking like it had lost the user's data.
     */
    @Test
    fun amountsAreVisibleByDefault() = runBlocking {
        assertFalse(preferences.amountsHidden.first())
    }

    /** The eye and the `Me` row both drive the same flip, so it has to be symmetric. */
    @Test
    fun toggleHidesThenRevealsAgain() = runBlocking {
        preferences.toggleAmountsHidden()
        assertTrue(preferences.amountsHidden.first())

        preferences.toggleAmountsHidden()
        assertFalse(preferences.amountsHidden.first())
    }

    /**
     * Two toggles racing from the hero and the settings row must not lose one another: the flip
     * reads and writes inside the store's transaction, so a pair of them nets out to no change
     * rather than to one write clobbering the other's basis.
     */
    @Test
    fun concurrentTogglesEachApply() = runBlocking {
        repeat(4) { preferences.toggleAmountsHidden() }
        assertFalse(preferences.amountsHidden.first())

        repeat(5) { preferences.toggleAmountsHidden() }
        assertTrue(preferences.amountsHidden.first())
    }

    /** A full reset returns the device to the visible default. */
    @Test
    fun clearRestoresVisibleAmounts() = runBlocking {
        preferences.toggleAmountsHidden()
        assertTrue(preferences.amountsHidden.first())

        preferences.clear()
        assertFalse(preferences.amountsHidden.first())
    }
}
