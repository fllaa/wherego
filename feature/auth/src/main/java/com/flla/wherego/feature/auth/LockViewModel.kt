package com.flla.wherego.feature.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flla.wherego.core.database.LocalDataEraser
import com.flla.wherego.core.datastore.AppLock
import com.flla.wherego.core.datastore.AppLockController
import com.flla.wherego.core.datastore.PinVerdict
import com.flla.wherego.core.i18n.R
import com.flla.wherego.core.sync.AuthRepository
import com.flla.wherego.core.sync.SignInException
import com.flla.wherego.core.sync.SignInFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LockUiState(
    val digits: String = "",
    /** Line under the dots. `null` when the last attempt has not failed. */
    val message: LockMessage? = null,
    val biometricOffered: Boolean = false,
    /** `0` when input is accepted; counts down while the throttle holds. */
    val cooldownSeconds: Int = 0,
    val signedIn: Boolean = false,
    val busy: Boolean = false,
)

/**
 * Drives the unlock gate. Owns nothing persistent: [AppLock] holds the digest and the throttle,
 * [AppLockController] holds whether the gate is showing, and this class only sequences them.
 */
@HiltViewModel
class LockViewModel @Inject constructor(
    private val appLock: AppLock,
    private val controller: AppLockController,
    private val auth: AuthRepository,
    private val local: LocalDataEraser,
) : ViewModel() {
    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    /** Whether the user opted into biometrics; the screen still checks the hardware separately. */
    val biometricPreferred: StateFlow<Boolean> = appLock.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(signedIn = auth.current().signedIn)
        }
        viewModelScope.launch {
            // A cooldown set before the process died must still be counting when it comes back.
            startCooldown(appLock.cooldownUntil.first())
        }
    }

    fun onDigit(digit: String) {
        val current = _state.value
        if (current.cooldownSeconds > 0 || current.busy) return
        if (current.digits.length >= AppLock.PIN_LENGTH) return
        val next = current.digits + digit
        _state.value = current.copy(digits = next, message = null)
        if (next.length == AppLock.PIN_LENGTH) submit(next)
    }

    fun onBackspace() {
        val current = _state.value
        if (current.cooldownSeconds > 0 || current.busy) return
        _state.value = current.copy(digits = current.digits.dropLast(1), message = null)
    }

    /** Called by the screen once it knows whether the hardware can actually authenticate. */
    fun onBiometricAvailability(available: Boolean) {
        _state.value = _state.value.copy(biometricOffered = available)
    }

    fun onBiometricSuccess() {
        controller.unlock()
    }

    private fun submit(pin: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true)
            when (val verdict = appLock.verify(pin)) {
                is PinVerdict.Ok -> {
                    _state.value = _state.value.copy(digits = "", busy = false)
                    controller.unlock()
                }
                is PinVerdict.Wrong -> _state.value = _state.value.copy(
                    digits = "",
                    busy = false,
                    message = LockMessage.WrongPin(verdict.attemptsLeft),
                )
                is PinVerdict.CoolingDown -> {
                    _state.value = _state.value.copy(digits = "", busy = false)
                    startCooldown(verdict.untilMillis)
                }
            }
        }
    }

    private fun startCooldown(untilMillis: Long) {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            var remaining = secondsUntil(untilMillis)
            while (remaining > 0) {
                _state.value = _state.value.copy(
                    cooldownSeconds = remaining,
                    message = LockMessage.CoolingDown(remaining),
                )
                delay(1_000)
                remaining = secondsUntil(untilMillis)
            }
            _state.value = _state.value.copy(cooldownSeconds = 0, message = null)
        }
    }

    private fun secondsUntil(untilMillis: Long): Int {
        val left = untilMillis - System.currentTimeMillis()
        return if (left <= 0) 0 else ((left + 999) / 1_000).toInt()
    }

    /**
     * Forgot-PIN recovery for a signed-in user: re-running the Google flow proves they still hold
     * the account this ledger backs up to, which is a strictly stronger claim than the PIN itself.
     * The returned uid must match the one already on the device — otherwise any Google account
     * would open any phone.
     */
    fun reauth(activity: Activity?) {
        if (activity == null) {
            _state.value = _state.value.copy(message = LockMessage.Info(R.string.lock_reauth_failed))
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = null)
            val existing = auth.current().firebaseUid
            auth.signIn(activity)
                .onSuccess { signedIn ->
                    if (existing != null && signedIn.firebaseUid == existing) {
                        appLock.disable()
                        _state.value = _state.value.copy(busy = false)
                        controller.unlock()
                    } else {
                        _state.value = _state.value.copy(
                            busy = false,
                            message = LockMessage.Info(R.string.lock_reauth_wrong_account),
                        )
                    }
                }
                .onFailure { e ->
                    val cancelled = (e as? SignInException)?.failure == SignInFailure.CANCELLED
                    _state.value = _state.value.copy(
                        busy = false,
                        message = if (cancelled) null else LockMessage.Info(R.string.lock_reauth_failed),
                    )
                }
        }
    }

    /**
     * Forgot-PIN recovery with no account to prove: wipe the device's ledger and drop the lock.
     *
     * Deliberately **not** `AccountEraser.erase()` — that deletes the Firebase user and the cloud
     * copy, which is far beyond "I forgot my PIN". `resetToGuest()` reseeds a guest profile, so the
     * app lands back in onboarding, which is the intended "start fresh".
     */
    fun eraseAndReset() {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true)
            local.resetToGuest()
            appLock.disable()
            _state.value = LockUiState()
            controller.unlock()
        }
    }
}
