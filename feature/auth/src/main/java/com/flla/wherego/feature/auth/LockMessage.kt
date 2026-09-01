package com.flla.wherego.feature.auth

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.flla.wherego.core.i18n.R

/**
 * The line under the PIN dots.
 *
 * A sealed type rather than a bare string-resource id plus a loose format argument, because
 * [WrongPin] needs `pluralStringResource` ("1 try left", not "1 tries left") while the rest need
 * `stringResource`. Follows [signInResultMessage]'s shape, which resolves sign-in outcomes to copy
 * the same way.
 */
sealed interface LockMessage {
    data class WrongPin(val attemptsLeft: Int) : LockMessage

    data class CoolingDown(val seconds: Int) : LockMessage

    /** Anything with fixed copy: a setup mismatch, or a failed Google re-auth. */
    data class Info(@StringRes val res: Int) : LockMessage
}

@Composable
fun lockMessageText(message: LockMessage): String = when (message) {
    is LockMessage.WrongPin -> pluralStringResource(
        R.plurals.lock_wrong,
        message.attemptsLeft,
        message.attemptsLeft,
    )
    is LockMessage.CoolingDown -> stringResource(R.string.lock_cooldown, message.seconds)
    is LockMessage.Info -> stringResource(message.res)
}
