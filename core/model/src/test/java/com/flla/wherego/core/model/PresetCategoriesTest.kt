package com.flla.wherego.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PresetCategoriesTest {
    /**
     * Every preset shares one soft hex, so none of them carries a colour choice worth painting on
     * a row badge — the theme's own soft accent is the same colour in light mode and the only one
     * that survives dark mode. If a preset is ever given its own hex, this fails and the badge
     * behaviour has to be decided again rather than changing silently.
     */
    @Test
    fun presetsCarryNoColourOfTheirOwn() {
        for (preset in PresetCategories.all) {
            assertEquals(preset.id, "", PresetCategories.customSoftHex(preset.softColorHex))
        }
    }

    @Test
    fun aCustomColourSurvives() {
        assertEquals("#FFE1D6", PresetCategories.customSoftHex("#FFE1D6"))
    }

    /** Hexes arrive from Room and CSV in whatever case they were written in. */
    @Test
    fun theSharedDefaultCollapsesRegardlessOfCase() {
        assertEquals("", PresetCategories.customSoftHex(PresetCategories.ACCENT_SOFT_HEX.lowercase()))
    }

    @Test
    fun missingColourCollapses() {
        assertEquals("", PresetCategories.customSoftHex(null))
        assertEquals("", PresetCategories.customSoftHex("   "))
    }
}
