package com.flla.wherego.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLockControllerTest {
    private lateinit var lock: AppLock
    private lateinit var controller: AppLockController

    /** Starts well clear of zero so the fake clock never coincides with a fresh-boot reading. */
    private var now = 5_000_000L

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lock = AppLock(context, FakePinMac())
        lock.disable()
        controller = AppLockController(lock).also { it.elapsed = { now } }
    }

    /**
     * `onForeground` finishes on the controller's own dispatcher, so the assertion polls instead of
     * assuming the write already landed.
     */
    private suspend fun settledLocked(expected: Boolean): Boolean =
        withTimeoutOrNull(2_000) {
            while (controller.locked.value != expected) delay(10)
            true
        } ?: false

    @Test
    fun bindLeavesUnlockedWhenNoPinIsSet() = runBlocking {
        controller.bind()
        assertFalse(controller.locked.value)
    }

    @Test
    fun bindLocksWhenAPinIsSet() = runBlocking {
        lock.enable("123456")
        controller.bind()
        assertTrue(controller.locked.value)
    }

    /**
     * The receipt-camera case. Attaching a receipt backgrounds the process; coming straight back
     * must not demand a PIN, or the lock would fight the capture flow it is wrapped around.
     */
    @Test
    fun shortTripToTheCameraDoesNotRelock() = runBlocking {
        lock.enable("123456")
        controller.bind()
        controller.unlock()

        controller.onBackground()
        now += AppLockController.GRACE_MILLIS - 1_000
        controller.onForeground()

        assertFalse("a sub-grace absence must not re-lock", settledLocked(true))
        assertFalse(controller.locked.value)
    }

    @Test
    fun absenceBeyondGraceRelocks() = runBlocking {
        lock.enable("123456")
        controller.bind()
        controller.unlock()

        controller.onBackground()
        now += AppLockController.GRACE_MILLIS
        controller.onForeground()

        assertTrue("an absence of the full grace window must re-lock", settledLocked(true))
    }

    /** With no PIN set, no amount of backgrounding may ever produce a gate. */
    @Test
    fun longAbsenceDoesNotRelockWhenLockIsOff() = runBlocking {
        controller.bind()

        controller.onBackground()
        now += AppLockController.GRACE_MILLIS * 10
        controller.onForeground()

        assertFalse(settledLocked(true))
        assertFalse(controller.locked.value)
    }

    /**
     * `ProcessLifecycleOwner` fires `ON_START` on cold start, before `bind()` runs. Treating that
     * as an absence would lock a user who never set a PIN.
     */
    @Test
    fun foregroundWithoutAPrecedingBackgroundIsIgnored() = runBlocking {
        lock.enable("123456")
        controller.bind()
        controller.unlock()

        now += AppLockController.GRACE_MILLIS * 10
        controller.onForeground()

        assertFalse(settledLocked(true))
        assertFalse(controller.locked.value)
    }

    /** A second foreground without another background must not re-use the stale timestamp. */
    @Test
    fun graceTimestampIsConsumedByTheFirstForeground() = runBlocking {
        lock.enable("123456")
        controller.bind()
        controller.unlock()

        controller.onBackground()
        now += 1_000
        controller.onForeground()
        assertFalse(controller.locked.value)

        now += AppLockController.GRACE_MILLIS * 10
        controller.onForeground()

        assertFalse(settledLocked(true))
        assertFalse(controller.locked.value)
    }
}
