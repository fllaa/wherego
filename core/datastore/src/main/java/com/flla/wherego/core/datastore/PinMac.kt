package com.flla.wherego.core.datastore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.KeyStoreException
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keyed digest for the app-lock PIN.
 *
 * An interface rather than a concrete class for one reason: Robolectric ships no working
 * `AndroidKeyStore` provider, so [AppLock] can only be unit-tested against a substitute. The
 * production implementation is [KeystorePinMac].
 */
interface PinMac {
    /** Whether the signing key exists. False on a device the key was never created on. */
    fun hasKey(): Boolean

    /** Creates the signing key if it is absent. Idempotent. */
    fun ensureKey()

    /** `HMAC-SHA256(key, salt ‖ pin)`. Requires [ensureKey] to have run. */
    fun mac(salt: ByteArray, pin: String): ByteArray

    /** Destroys the signing key, making every previously stored digest unverifiable. */
    fun deleteKey()
}

/**
 * Binds the PIN digest to this specific device via a non-exportable Android Keystore key.
 *
 * A 6-digit PIN is a 10^6 keyspace. A plain salted SHA — or even PBKDF2 — sitting in a file is
 * therefore exhaustible in well under a second by anyone holding a copy of that file, which makes
 * the salt the only thing a conventional password hash buys here, and the salt does not stop a
 * brute force this small. Signing the PIN with a Keystore key changes the shape of the problem:
 * the key material is non-exportable and hardware-backed where a TEE/StrongBox is present, so the
 * stored digest cannot be attacked at all without this handset. It also costs no new dependency —
 * [KeyStore] and [Mac] are platform APIs.
 *
 * Deliberately **no** `setUserAuthenticationRequired`: this key authenticates the *digest*, and
 * biometrics are a separate factor handled by `BiometricPrompt`. Requiring user auth here would
 * make the PIN unverifiable on a device with no enrolled biometrics — exactly the device that
 * needs a PIN most.
 */
@Singleton
class KeystorePinMac @Inject constructor() : PinMac {
    private fun store(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    override fun hasKey(): Boolean = runCatching { store().containsAlias(ALIAS) }.getOrDefault(false)

    override fun ensureKey() {
        if (hasKey()) return
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, PROVIDER).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build(),
            )
            generateKey()
        }
    }

    override fun mac(salt: ByteArray, pin: String): ByteArray {
        val key = store().getKey(ALIAS, null) ?: error("app lock key missing")
        return Mac.getInstance(MAC_ALGORITHM).run {
            init(key)
            update(salt)
            doFinal(pin.toByteArray(Charsets.UTF_8))
        }
    }

    /** A missing alias is the desired end state, so a [KeyStoreException] here is not a failure. */
    override fun deleteKey() {
        runCatching { store().deleteEntry(ALIAS) }
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "wherego_pin_hmac"
        const val MAC_ALGORITHM = "HmacSHA256"
    }
}
