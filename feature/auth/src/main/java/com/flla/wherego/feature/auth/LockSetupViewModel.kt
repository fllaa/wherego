package com.flla.wherego.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flla.wherego.core.datastore.AppLock
import com.flla.wherego.core.datastore.PinVerdict
import com.flla.wherego.core.i18n.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LockSetupStep { Manage, Enter, Confirm, VerifyToDisable, VerifyToChange }

data class LockSetupUiState(
    val step: LockSetupStep = LockSetupStep.Manage,
    val enabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val digits: String = "",
    val message: LockMessage? = null,
    /** Bumped on every rejected PIN or mismatch so the stage can shake again. */
    val shakeKey: Int = 0,
    /** Holds the celebration frame for a beat after the lock is first switched on. */
    val justEnabled: Boolean = false,
    val cooldownSeconds: Int = 0,
    val busy: Boolean = false,
)

/**
 * `Me → App lock`: sets, changes, or removes the PIN, and toggles the biometric opt-in.
 *
 * Injects [AppLock] but deliberately not `AppLockController`: this screen only runs inside an
 * already-unlocked session, and touching the runtime gate here could lock the user out mid-flow.
 */
@HiltViewModel
class LockSetupViewModel @Inject constructor(
    private val appLock: AppLock,
) : ViewModel() {
    /** [first] is the PIN captured on the `Enter` step, held only until `Confirm` matches it. */
    private data class Local(
        val step: LockSetupStep = LockSetupStep.Manage,
        val digits: String = "",
        val message: LockMessage? = null,
        val shakeKey: Int = 0,
        val justEnabled: Boolean = false,
        val cooldownSeconds: Int = 0,
        val busy: Boolean = false,
        val first: String = "",
    )

    private val local = MutableStateFlow(Local())
    private var ticker: Job? = null

    val state: StateFlow<LockSetupUiState> = combine(
        local,
        appLock.enabled,
        appLock.biometricEnabled,
    ) { l, enabled, biometric ->
        LockSetupUiState(
            step = l.step,
            enabled = enabled,
            biometricEnabled = biometric,
            digits = l.digits,
            message = l.message,
            shakeKey = l.shakeKey,
            justEnabled = l.justEnabled,
            cooldownSeconds = l.cooldownSeconds,
            busy = l.busy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LockSetupUiState())

    fun startSetup() = go(LockSetupStep.Enter)

    fun startChange() = go(LockSetupStep.VerifyToChange)

    fun startDisable() = go(LockSetupStep.VerifyToDisable)

    /** Returns to the manage list, dropping any half-typed PIN. */
    fun cancel() = go(LockSetupStep.Manage)

    private fun go(step: LockSetupStep) {
        local.value = local.value.copy(
            step = step,
            digits = "",
            message = null,
            first = "",
            justEnabled = false,
        )
    }

    fun toggleBiometric() {
        viewModelScope.launch { appLock.setBiometricEnabled(!state.value.biometricEnabled) }
    }

    fun onDigit(digit: String) {
        val current = local.value
        if (current.busy || current.cooldownSeconds > 0) return
        if (current.digits.length >= AppLock.PIN_LENGTH) return
        val next = current.digits + digit
        local.value = current.copy(digits = next, message = null)
        if (next.length == AppLock.PIN_LENGTH) advance(next)
    }

    fun onBackspace() {
        val current = local.value
        if (current.busy || current.cooldownSeconds > 0) return
        local.value = current.copy(digits = current.digits.dropLast(1), message = null)
    }

    private fun advance(pin: String) {
        when (local.value.step) {
            LockSetupStep.Enter ->
                local.value = local.value.copy(step = LockSetupStep.Confirm, digits = "", first = pin)

            LockSetupStep.Confirm -> confirm(pin)
            LockSetupStep.VerifyToDisable -> verifyThen(pin) {
                appLock.disable()
                go(LockSetupStep.Manage)
            }

            LockSetupStep.VerifyToChange -> verifyThen(pin) { go(LockSetupStep.Enter) }
            LockSetupStep.Manage -> Unit
        }
    }

    private fun confirm(pin: String) {
        val first = local.value.first
        if (pin != first) {
            // Back to the start, not to a retry of the confirm: the user does not know which of
            // the two entries was the typo, so re-typing only the second one is guesswork.
            local.value = local.value.copy(
                step = LockSetupStep.Enter,
                digits = "",
                first = "",
                message = LockMessage.Info(R.string.lock_mismatch),
                shakeKey = local.value.shakeKey + 1,
            )
            return
        }
        viewModelScope.launch {
            local.value = local.value.copy(busy = true)
            appLock.enable(pin)
            // The one moment the user is choosing the lock rather than being stopped by it, so it
            // gets a beat of celebration before dropping back to the manage list.
            local.value = Local(justEnabled = true)
            delay(CELEBRATION_MILLIS)
            if (local.value.justEnabled) local.value = Local()
        }
    }

    /**
     * Runs [onOk] only for a correct PIN, routing `Wrong`/`CoolingDown` to the shared error line.
     * Turning the lock **off** goes through the same throttle as opening the app — otherwise the
     * settings screen would be the cheap way past it.
     */
    private fun verifyThen(pin: String, onOk: suspend () -> Unit) {
        viewModelScope.launch {
            local.value = local.value.copy(busy = true)
            when (val verdict = appLock.verify(pin)) {
                is PinVerdict.Ok -> {
                    local.value = local.value.copy(busy = false)
                    onOk()
                }
                is PinVerdict.Wrong -> local.value = local.value.copy(
                    digits = "",
                    busy = false,
                    message = LockMessage.WrongPin(verdict.attemptsLeft),
                    shakeKey = local.value.shakeKey + 1,
                )
                is PinVerdict.CoolingDown -> {
                    local.value = local.value.copy(
                        digits = "",
                        busy = false,
                        shakeKey = local.value.shakeKey + 1,
                    )
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
                local.value = local.value.copy(
                    cooldownSeconds = remaining,
                    message = LockMessage.CoolingDown(remaining),
                )
                delay(1_000)
                remaining = secondsUntil(untilMillis)
            }
            local.value = local.value.copy(cooldownSeconds = 0, message = null)
        }
    }

    private fun secondsUntil(untilMillis: Long): Int {
        val left = untilMillis - System.currentTimeMillis()
        return if (left <= 0) 0 else ((left + 999) / 1_000).toInt()
    }

    private companion object {
        /** Long enough to register as a reply, short enough not to feel like a wait. */
        const val CELEBRATION_MILLIS = 1_300L
    }
}
