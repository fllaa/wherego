package com.flla.wherego.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.designsystem.component.WheregoPinDots
import com.flla.wherego.core.designsystem.component.WheregoPinPad
import com.flla.wherego.core.designsystem.component.WheregoWaypointMark
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.datastore.AppLock
import com.flla.wherego.core.i18n.R

/**
 * The launch gate. Nothing behind it renders: `MainActivity` swaps this in for the whole nav host,
 * so no balance, figure or transaction is ever composed while the app is locked.
 *
 * Back is swallowed. A gate that dismisses on back is not a gate.
 */
@Composable
fun LockRoute(viewModel: LockViewModel = hiltViewModel()) {
    val colors = WheregoTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val biometricPreferred by viewModel.biometricPreferred.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity() as? FragmentActivity
    var forgotOpen by remember { mutableStateOf(false) }
    var promptShown by remember { mutableStateOf(false) }

    val promptTitle = stringResource(R.string.lock_biometric_prompt_title)
    val promptSub = stringResource(R.string.lock_biometric_prompt_sub)
    val promptNegative = stringResource(R.string.lock_biometric_negative)

    BackHandler(enabled = true) {}

    // Hardware enrolment can change between launches, so it is re-read on every show rather than
    // trusted from the stored opt-in.
    LaunchedEffect(Unit) {
        viewModel.onBiometricAvailability(activity != null && BiometricGate.available(context))
    }

    fun showPrompt() {
        val target = activity ?: return
        BiometricGate.prompt(
            activity = target,
            title = promptTitle,
            subtitle = promptSub,
            negative = promptNegative,
            onSuccess = viewModel::onBiometricSuccess,
            onFallback = {},
        )
    }

    // Fires once per gate appearance: the common case is one touch and no typing.
    LaunchedEffect(state.biometricOffered, biometricPreferred) {
        if (state.biometricOffered && biometricPreferred && !promptShown) {
            promptShown = true
            showPrompt()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .systemBarsPadding()
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The header group centres itself in whatever is left above the pad, so a tall screen does
        // not open a void between the dots and the keys.
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            WheregoWaypointMark(size = 64.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.lock_title),
                style = WheregoType.onboardTitle,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.lock_sub),
                style = WheregoType.helper,
                color = colors.muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            WheregoPinDots(
                filled = state.digits.length,
                total = AppLock.PIN_LENGTH,
                error = state.message != null,
            )
            Spacer(Modifier.height(12.dp))
            // Reserved height, so nothing shifts when a message appears or clears.
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val message = state.message
                if (message != null) {
                    Text(
                        lockMessageText(message),
                        style = WheregoType.helper,
                        color = colors.coral,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        WheregoPinPad(
            onDigit = viewModel::onDigit,
            onBackspace = viewModel::onBackspace,
            modifier = Modifier.widthIn(max = 340.dp),
            enabled = state.cooldownSeconds == 0 && !state.busy,
        )
        Spacer(Modifier.height(18.dp))
        if (state.biometricOffered) {
            Text(
                stringResource(R.string.lock_biometric_cta),
                style = WheregoType.chip,
                color = colors.accentText,
                modifier = Modifier
                    .clickable { showPrompt() }
                    .padding(8.dp),
            )
        }
        Text(
            stringResource(R.string.lock_forgot),
            style = WheregoType.link,
            color = colors.muted,
            modifier = Modifier
                .clickable { forgotOpen = true }
                .padding(10.dp),
        )
        Spacer(Modifier.height(12.dp))
    }

    if (forgotOpen) {
        if (state.signedIn) {
            AlertDialog(
                onDismissRequest = { forgotOpen = false },
                title = { Text(stringResource(R.string.lock_reauth_title)) },
                text = { Text(stringResource(R.string.lock_reauth_body)) },
                confirmButton = {
                    TextButton(
                        enabled = !state.busy,
                        onClick = {
                            forgotOpen = false
                            viewModel.reauth(activity)
                        },
                    ) {
                        Text(stringResource(R.string.lock_reauth_cta), color = colors.accentText)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { forgotOpen = false }) {
                        Text(stringResource(R.string.dialog_cancel), color = colors.muted)
                    }
                },
            )
        } else {
            AlertDialog(
                onDismissRequest = { forgotOpen = false },
                title = { Text(stringResource(R.string.lock_erase_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.lock_erase_body))
                        Text(
                            stringResource(R.string.lock_erase_guest_warning),
                            color = colors.coral,
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !state.busy,
                        onClick = {
                            forgotOpen = false
                            viewModel.eraseAndReset()
                        },
                    ) {
                        Text(stringResource(R.string.lock_erase_confirm), color = colors.coral)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { forgotOpen = false }) {
                        Text(stringResource(R.string.dialog_cancel), color = colors.muted)
                    }
                },
            )
        }
    }
}
