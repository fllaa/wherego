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
    private val conflictKey = stringPreferencesKey("balance_conflict")
    private val hideAmountsKey = booleanPreferencesKey("amounts_hidden")

    val mode: Flow<String> = store.data.map { ThemeMode.parse(it[key]) }

    /**
     * Whether the first-run Sign In screen (`pencil-new.pen` → `Sign In`) has been
     * answered. Device-local UI state, deliberately not part of the synced profile:
     * "Try it first, sign in later" must not resurface the gate on every launch.
     */
    val welcomeSeen: Flow<Boolean> = store.data.map { it[welcomeKey] == true }

    /**
     * `mine|theirs` transaction ids when a sync handed the balance to another device's assertion
     * and the number moved. The arithmetic is right — the later claim anchors — but the figure the
     * user was shown changed, and the peer's could be the typo, so it is worth one question.
     *
     * Device-local on purpose: the phone that made the winning claim has nothing to ask about.
     */
    val balanceConflict: Flow<Pair<String, String>?> = store.data.map { prefs ->
        prefs[conflictKey]
            ?.split('|')
            ?.takeIf { it.size == 2 && it.none(String::isBlank) }
            ?.let { it[0] to it[1] }
    }

    /**
     * Whether every amount on the browsing surfaces renders as bullets — the
     * shoulder-surfing guard behind `Me → Hide amounts`.
     *
     * Device-local, deliberately outside the synced profile: it describes who can see this screen
     * right now, which is a fact about the room the phone is in and not about the account.
     */
    val amountsHidden: Flow<Boolean> = store.data.map { it[hideAmountsKey] == true }

    suspend fun setMode(mode: String) {
        store.edit { it[key] = ThemeMode.parse(mode) }
    }

    suspend fun setWelcomeSeen(seen: Boolean) {
        store.edit { it[welcomeKey] = seen }
    }

    suspend fun setBalanceConflict(mineId: String, theirsId: String) {
        store.edit { it[conflictKey] = "$mineId|$theirsId" }
    }

    /**
     * Flips the guard inside the store's own transaction. Both the hero eye and the `Me` row drive
     * this, and reading the flow to negate it outside the edit would race the other one.
     */
    suspend fun toggleAmountsHidden() {
        store.edit { it[hideAmountsKey] = it[hideAmountsKey] != true }
    }

    suspend fun clearBalanceConflict() {
        store.edit { it.remove(conflictKey) }
    }

    /** Wipes every device preference — theme, `welcome_seen` and `amounts_hidden` — for a reset. */
    suspend fun clear() {
        store.edit { it.clear() }
    }
}
