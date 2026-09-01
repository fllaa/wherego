package com.flla.wherego.core.datastore

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Whether the unlock gate is currently showing, and the rule that re-arms it.
 *
 * Runtime state, deliberately not persisted: a cold start re-derives it from [AppLock.enabled] in
 * [bind], which is also the only place a launch can decide to lock. Backgrounding is the other
 * trigger, and it is throttled by [GRACE_MILLIS] rather than being immediate.
 *
 * The grace window is what keeps the lock compatible with the app's own flows. Attaching a receipt
 * launches the camera, the photo picker or a share chooser, all of which background the process;
 * locking the moment that happens would demand a PIN in the middle of logging a spend, in an app
 * whose stated principle is that capture never waits. A minute is long enough to cover a round trip
 * to the camera and short enough that a phone put down and picked up later is still protected.
 */
@Singleton
class AppLockController @Inject constructor(private val appLock: AppLock) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    /**
     * Monotonic on purpose: the window must not move when the user changes the clock, and a reboot
     * resetting it to zero re-locks, which is the safe direction. Overridden in tests.
     */
    internal var elapsed: () -> Long = { SystemClock.elapsedRealtime() }

    /** `null` until a background transition is observed in this process. */
    private var backgroundedAt: Long? = null

    /**
     * Cold-start decision, called once before the UI is allowed to render content. Reconciles a
     * lock whose Keystore key has gone missing first, so a device that cannot verify a PIN opens
     * rather than dead-ends.
     */
    suspend fun bind() {
        appLock.reconcile()
        _locked.value = appLock.enabled.first()
    }

    fun onBackground() {
        backgroundedAt = elapsed()
    }

    /**
     * Re-locks only if the process was genuinely away for [GRACE_MILLIS].
     *
     * The null guard matters: `ProcessLifecycleOwner` fires `ON_START` during cold start, before
     * [bind] has run, and an unguarded elapsed-time comparison would read that as an infinitely
     * long absence and lock a user who has no PIN set. A sentinel of `0` would be ambiguous with a
     * genuine reading, so absence is modelled as absence.
     */
    fun onForeground() {
        val since = backgroundedAt ?: return
        backgroundedAt = null
        if (elapsed() - since < GRACE_MILLIS) return
        scope.launch {
            if (appLock.enabled.first()) _locked.value = true
        }
    }

    fun unlock() {
        _locked.value = false
    }

    companion object {
        const val GRACE_MILLIS = 60_000L
    }
}
