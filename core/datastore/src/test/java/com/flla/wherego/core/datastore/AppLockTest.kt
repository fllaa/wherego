package com.flla.wherego.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val PIN = "123456"
private const val WRONG = "000000"
private const val LOCK_FILE = "wherego_lock.preferences_pb"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLockTest {
    private lateinit var context: Context
    private lateinit var mac: FakePinMac
    private lateinit var lock: AppLock

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext<Context>()
        mac = FakePinMac()
        lock = AppLock(context, mac)
        lock.disable()
        mac.deleteCount = 0
    }

    /** The lock has to be absent on a fresh install, or a new user is gated out of their own app. */
    @Test
    fun lockIsOffByDefault() = runBlocking {
        assertFalse(lock.enabled.first())
        assertFalse(lock.biometricEnabled.first())
    }

    @Test
    fun enableThenVerifyCorrectPinSucceeds() = runBlocking {
        lock.enable(PIN)
        assertTrue(lock.enabled.first())
        assertEquals(PinVerdict.Ok, lock.verify(PIN))
    }

    @Test
    fun wrongPinReportsRemainingAttempts() = runBlocking {
        lock.enable(PIN)
        assertEquals(PinVerdict.Wrong(attemptsLeft = 4), lock.verify(WRONG))
    }

    /**
     * The throttle is checked before the digest is compared, so a lucky guess during a cooldown is
     * still refused. Otherwise the delay would only inconvenience someone who is already wrong and
     * do nothing to the person converging on the answer.
     */
    @Test
    fun fifthWrongPinStartsCooldownThatEvenTheCorrectPinCannotBypass() = runBlocking {
        lock.enable(PIN)
        repeat(4) { assertTrue(lock.verify(WRONG) is PinVerdict.Wrong) }

        val fifth = lock.verify(WRONG)
        assertTrue("fifth failure must start a cooldown, was $fifth", fifth is PinVerdict.CoolingDown)

        val duringCooldown = lock.verify(PIN)
        assertTrue(
            "a correct PIN during a cooldown must still be refused, was $duringCooldown",
            duringCooldown is PinVerdict.CoolingDown,
        )
    }

    @Test
    fun correctPinClearsFailureCount() = runBlocking {
        lock.enable(PIN)
        repeat(3) { lock.verify(WRONG) }
        assertEquals(PinVerdict.Wrong(attemptsLeft = 1), lock.verify(WRONG))
        assertEquals(PinVerdict.Ok, lock.verify(PIN))

        assertEquals(PinVerdict.Wrong(attemptsLeft = 4), lock.verify(WRONG))
    }

    /**
     * Asserts on the bytes DataStore actually wrote, and on the file name: `backup_rules.xml`
     * excludes `datastore/wherego_lock.preferences_pb` by name, so if DataStore ever renamed or
     * relocated it, the exclusion would silently stop matching and a PIN digest would start riding
     * backups to devices that can never verify it.
     *
     * Located by search rather than from `context.filesDir`, because `preferencesDataStore` is a
     * property delegate that caches one store for the first Context it is handed, and Robolectric
     * gives each test class its own sandbox — so this class's `filesDir` is not necessarily the
     * directory that got written.
     */
    @Test
    fun storedFileHoldsNoPlaintextPin() = runBlocking {
        lock.enable(PIN)

        val stored = findLockFile(context.filesDir)
        assertNotNull("no wherego_lock.preferences_pb was written", stored)

        val raw = stored!!.readBytes().toString(Charsets.ISO_8859_1)
        assertTrue("a digest must actually be recorded", raw.isNotEmpty())
        assertFalse("the PIN itself must never be written", raw.contains(PIN))
    }

    private fun findLockFile(from: File): File? {
        var dir: File? = from
        repeat(6) {
            val hit = dir?.walkTopDown()?.maxDepth(6)
                ?.firstOrNull { it.name == LOCK_FILE }
            if (hit != null) return hit
            dir = dir?.parentFile
        }
        return null
    }

    @Test
    fun disableClearsEverythingAndDestroysTheKey() = runBlocking {
        lock.enable(PIN)
        lock.setBiometricEnabled(true)
        assertTrue(lock.biometricEnabled.first())

        lock.disable()

        assertFalse(lock.enabled.first())
        assertFalse(lock.biometricEnabled.first())
        assertEquals(1, mac.deleteCount)
    }

    /**
     * The restore-to-a-new-device guard. A digest whose signing key is gone can never verify, so
     * holding on to it would trap the owner out of an offline ledger with no recovery at all.
     */
    @Test
    fun reconcileClearsLockWhenSigningKeyIsGone() = runBlocking {
        lock.enable(PIN)
        assertTrue(lock.enabled.first())

        mac.keyPresent = false
        lock.reconcile()

        assertFalse(lock.enabled.first())
    }

    /** Reconcile must not disturb a healthy lock — it runs on every single launch. */
    @Test
    fun reconcileKeepsLockWhenSigningKeyIsPresent() = runBlocking {
        lock.enable(PIN)
        lock.reconcile()

        assertTrue(lock.enabled.first())
        assertEquals(PinVerdict.Ok, lock.verify(PIN))
    }

    /** Changing the PIN must invalidate the old one, not merely add a second accepted value. */
    @Test
    fun reEnableReplacesThePreviousPin() = runBlocking {
        lock.enable(PIN)
        lock.enable("654321")

        assertEquals(PinVerdict.Ok, lock.verify("654321"))
        assertTrue(lock.verify(PIN) is PinVerdict.Wrong)
    }
}
