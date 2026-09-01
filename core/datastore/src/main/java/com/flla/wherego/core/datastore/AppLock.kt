package com.flla.wherego.core.datastore

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * A separate DataStore file from `wherego_prefs`, for two reasons that both matter.
 *
 * DataStore throws when two `preferencesDataStore` delegates name the same file, and
 * [ThemePreferences] already owns `wherego_prefs`. More importantly, this file is the one thing in
 * the app that must **never** be backed up: its digest is an HMAC under a device-bound Keystore
 * key, so a copy restored onto another handset could not be verified there. A dedicated file lets
 * `backup_rules.xml` exclude exactly it, leaving theme and currency to back up normally.
 */
private val Context.lockStore by preferencesDataStore(name = "wherego_lock")

/** Outcome of one PIN attempt. */
sealed interface PinVerdict {
    data object Ok : PinVerdict

    /** [attemptsLeft] tries remain before a cooldown starts. */
    data class Wrong(val attemptsLeft: Int) : PinVerdict

    /** Throttled: no attempt is even evaluated until [untilMillis] (wall clock). */
    data class CoolingDown(val untilMillis: Long) : PinVerdict
}

/**
 * The app lock's stored state: a keyed PIN digest, the biometric opt-in, and the failure throttle.
 *
 * Nothing here is a secret in plaintext — the PIN exists only as `HMAC(keystoreKey, salt ‖ pin)`
 * via [PinMac], so the file is inert without the device that made it.
 */
@Singleton
class AppLock @Inject constructor(
    @ApplicationContext context: Context,
    private val mac: PinMac,
) {
    private val store = context.lockStore
    private val digestKey = stringPreferencesKey("pin_digest")
    private val saltKey = stringPreferencesKey("pin_salt")
    private val biometricKey = booleanPreferencesKey("biometric_enabled")
    private val attemptsKey = intPreferencesKey("failed_attempts")
    private val cooldownKey = longPreferencesKey("cooldown_until")

    /** Whether a PIN is set. Drives both the launch gate and the `Me → App lock` row. */
    val enabled: Flow<Boolean> = store.data.map { it[digestKey] != null }

    /** Whether the unlock screen should offer biometrics before the keypad. */
    val biometricEnabled: Flow<Boolean> = store.data.map { it[biometricKey] == true }

    /** Wall-clock millis the throttle expires at; `0` when there is no cooldown. */
    val cooldownUntil: Flow<Long> = store.data.map { it[cooldownKey] ?: 0L }

    /**
     * Clears the lock when its Keystore key has vanished but a digest remains.
     *
     * `backup_rules.xml` keeps this file out of Auto Backup, but a key can still go missing — a
     * keystore wipe, or a restore path that predates those rules. The digest would then be
     * unverifiable forever, so the only two options are failing open or trapping the owner out of
     * their own offline ledger with no recovery whatsoever. Failing open is the lesser harm: the
     * attacker this lock defends against is someone holding the unlocked handset, and they cannot
     * conjure a missing hardware key either.
     */
    suspend fun reconcile() {
        val snapshot = store.data.first()
        if (snapshot[digestKey] != null && !mac.hasKey()) {
            store.edit { it.clear() }
        }
    }

    /** Sets or replaces the PIN. Leaves the biometric opt-in alone, and clears any throttle. */
    suspend fun enable(pin: String) {
        mac.ensureKey()
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val digest = mac.mac(salt, pin)
        store.edit { prefs ->
            prefs[digestKey] = Base64.encodeToString(digest, Base64.NO_WRAP)
            prefs[saltKey] = Base64.encodeToString(salt, Base64.NO_WRAP)
            prefs[attemptsKey] = 0
            prefs.remove(cooldownKey)
        }
    }

    /**
     * Checks [pin] and maintains the throttle in the same call, so no caller can forget to.
     *
     * An active cooldown short-circuits **before** the digest is compared: a correct guess during
     * a cooldown is still refused, otherwise the throttle would only slow down someone who is
     * already wrong and not someone converging on the answer.
     */
    suspend fun verify(pin: String): PinVerdict {
        val snapshot = store.data.first()
        val storedDigest = snapshot[digestKey]
        val storedSalt = snapshot[saltKey]
        val now = System.currentTimeMillis()
        val until = snapshot[cooldownKey] ?: 0L
        if (until > now) return PinVerdict.CoolingDown(until)
        // No digest means no lock is set; verification cannot succeed, and must not.
        if (storedDigest == null || storedSalt == null) {
            return PinVerdict.Wrong(ATTEMPTS_BEFORE_COOLDOWN)
        }

        val expected = Base64.decode(storedDigest, Base64.NO_WRAP)
        val actual = mac.mac(Base64.decode(storedSalt, Base64.NO_WRAP), pin)
        // Constant-time: a length-independent early exit would leak the digest a byte at a time.
        if (MessageDigest.isEqual(expected, actual)) {
            store.edit { prefs ->
                prefs[attemptsKey] = 0
                prefs.remove(cooldownKey)
            }
            return PinVerdict.Ok
        }

        var attemptsLeft = ATTEMPTS_BEFORE_COOLDOWN
        var coolingUntil = 0L
        store.edit { prefs ->
            val attempts = (prefs[attemptsKey] ?: 0) + 1
            prefs[attemptsKey] = attempts
            val intoBlock = attempts % ATTEMPTS_BEFORE_COOLDOWN
            if (intoBlock == 0) {
                coolingUntil = now + cooldownFor(attempts)
                prefs[cooldownKey] = coolingUntil
            }
            attemptsLeft = ATTEMPTS_BEFORE_COOLDOWN - intoBlock
        }
        return if (coolingUntil > 0L) PinVerdict.CoolingDown(coolingUntil) else PinVerdict.Wrong(attemptsLeft)
    }

    /** Removes the PIN, the throttle and the biometric opt-in, then destroys the signing key. */
    suspend fun disable() {
        store.edit { it.clear() }
        mac.deleteKey()
    }

    suspend fun setBiometricEnabled(on: Boolean) {
        store.edit { it[biometricKey] = on }
    }

    /**
     * Escalating cooldown, one step per block of [ATTEMPTS_BEFORE_COOLDOWN] failures.
     *
     * Wall clock, not `elapsedRealtime`: the throttle has to survive process death, and force-stop
     * is exactly what someone guessing a PIN would try first. The trade is that moving the system
     * clock backwards shortens a cooldown — acceptable against a shoulder-surfer, and cheaper than
     * persisting two clocks to close it.
     */
    private fun cooldownFor(attempts: Int): Long = when {
        attempts <= 5 -> 30_000L
        attempts <= 10 -> 60_000L
        attempts <= 15 -> 120_000L
        else -> 300_000L
    }

    companion object {
        const val PIN_LENGTH = 6
        const val ATTEMPTS_BEFORE_COOLDOWN = 5
        private const val SALT_BYTES = 16
    }
}
