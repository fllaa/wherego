package com.flla.wherego.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flla.wherego.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeStore by preferencesDataStore(name = "wherego_prefs")

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store = context.themeStore
    private val key = stringPreferencesKey("theme_mode")
    private val welcomeKey = booleanPreferencesKey("welcome_seen")

    val mode: Flow<String> = store.data.map { ThemeMode.parse(it[key]) }

    /**
     * Whether the first-run Sign In screen (`pencil-new.pen` → `Sign In`) has been
     * answered. Device-local UI state, deliberately not part of the synced profile:
     * "Try it first, sign in later" must not resurface the gate on every launch.
     */
    val welcomeSeen: Flow<Boolean> = store.data.map { it[welcomeKey] == true }

    suspend fun setMode(mode: String) {
        store.edit { it[key] = ThemeMode.parse(mode) }
    }

    suspend fun setWelcomeSeen(seen: Boolean) {
        store.edit { it[welcomeKey] = seen }
    }
}
