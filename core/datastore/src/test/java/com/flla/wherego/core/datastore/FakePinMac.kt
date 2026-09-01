package com.flla.wherego.core.datastore

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Substitute for [KeystorePinMac]: Robolectric ships no working `AndroidKeyStore` provider, which
 * is the whole reason [PinMac] is an interface.
 *
 * Kept in test sources on purpose. A stand-in for a security primitive has no business shipping
 * inside the APK, where a mis-wired Hilt binding could silently swap it in for the real thing.
 */
internal class FakePinMac : PinMac {
    var keyPresent = false
    var deleteCount = 0
    private val key = SecretKeySpec("fake-app-lock-key".toByteArray(), ALGORITHM)

    override fun hasKey(): Boolean = keyPresent

    override fun ensureKey() {
        keyPresent = true
    }

    override fun mac(salt: ByteArray, pin: String): ByteArray = Mac.getInstance(ALGORITHM).run {
        init(key)
        update(salt)
        doFinal(pin.toByteArray(Charsets.UTF_8))
    }

    override fun deleteKey() {
        keyPresent = false
        deleteCount++
    }

    private companion object {
        const val ALGORITHM = "HmacSHA256"
    }
}
