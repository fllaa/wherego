package com.flla.wherego.feature.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * The system biometric prompt, as the app lock's fast path in front of the PIN.
 *
 * `BIOMETRIC_STRONG` only, never `DEVICE_CREDENTIAL`. The combination is unsupported by
 * `canAuthenticate` on API 28-29, and the device credential would only duplicate what this app
 * already owns — a PIN it can fall back to — while adding an API-level minefield. Because
 * `DEVICE_CREDENTIAL` is absent, `setNegativeButtonText` is mandatory rather than optional; it is
 * the "Use PIN" escape hatch.
 */
object BiometricGate {
    /** Whether this device has a strong biometric enrolled *right now*. Re-check on every show. */
    fun available(context: Context): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Shows the prompt. [onFallback] fires for every non-success outcome, including a dead sensor,
     * so the keypad is always reachable — a biometric failure must never be a dead end.
     *
     * `onAuthenticationFailed` (a finger that did not match) is deliberately not forwarded: the
     * system prompt stays up and lets the user try again, and tearing it down on the first bad
     * read would be worse than what the OS already does.
     */
    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negative: String,
        onSuccess: () -> Unit,
        onFallback: () -> Unit,
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFallback()
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText(negative)
                .setConfirmationRequired(false)
                .build(),
        )
    }
}
