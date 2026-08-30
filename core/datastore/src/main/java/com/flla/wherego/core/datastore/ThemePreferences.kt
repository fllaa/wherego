package com.flla.wherego.core.datastore

import android.content.Context
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

    val mode: Flow<String> = store.data.map { ThemeMode.parse(it[key]) }

    suspend fun setMode(mode: String) {
        store.edit { it[key] = ThemeMode.parse(mode) }
    }
}
