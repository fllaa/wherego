package com.flla.wherego.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun parseMapsUnknownAndNullToSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.parse(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.parse(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.parse("fr-FR"))
        assertEquals(AppLanguage.EN, AppLanguage.parse(AppLanguage.EN))
        assertEquals(AppLanguage.ID, AppLanguage.parse(AppLanguage.ID))
    }

    @Test
    fun resolveFollowsDeviceWhenSystem() {
        assertEquals(AppLanguage.ID, AppLanguage.resolve(AppLanguage.SYSTEM, listOf("in")))
        assertEquals(AppLanguage.ID, AppLanguage.resolve(AppLanguage.SYSTEM, listOf("id")))
        assertEquals(AppLanguage.ID, AppLanguage.resolve(AppLanguage.SYSTEM, emptyList()))
        assertEquals(AppLanguage.EN, AppLanguage.resolve(AppLanguage.SYSTEM, listOf("en")))
        assertEquals(AppLanguage.EN, AppLanguage.resolve(AppLanguage.SYSTEM, listOf("de")))
    }

    @Test
    fun resolveIgnoresDeviceWhenExplicit() {
        assertEquals(AppLanguage.EN, AppLanguage.resolve(AppLanguage.EN, listOf("in")))
        assertEquals(AppLanguage.ID, AppLanguage.resolve(AppLanguage.ID, listOf("en")))
    }
}
