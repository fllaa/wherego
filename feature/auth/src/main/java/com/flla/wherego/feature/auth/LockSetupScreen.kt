package com.flla.wherego.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.designsystem.component.GoMood
import com.flla.wherego.core.designsystem.component.WheregoGoAvatar
import com.flla.wherego.core.designsystem.component.WheregoPinPad
import com.flla.wherego.core.designsystem.component.WheregoPrimaryButton
import com.flla.wherego.core.designsystem.component.WheregoSettingDivider
import com.flla.wherego.core.designsystem.component.WheregoSettingRow
import com.flla.wherego.core.designsystem.component.WheregoSettingsCard
import com.flla.wherego.core.designsystem.theme.LocalReducedMotion
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.datastore.AppLock
import com.flla.wherego.core.i18n.R

/**
 * `Me → APP → App lock`. Lives in `:feature:auth` beside the gate it configures, and is reached
 * from `:feature:settings` over the module edge that already exists for `ProfileScreen`.
 *
 * Back chrome is the same inline 38dp ink-outlined circle the other `Me` sub-screens draw, rather
 * than `WheregoOnboardTopBar` — that component carries an onboarding progress rail, which would be
 * meaningless here.
 */
@Composable
fun LockManageRoute(
    onBack: () -> Unit,
    viewModel: LockSetupViewModel = hiltViewModel(),
) {
    val colors = WheregoTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity() as? FragmentActivity
    val atRoot = state.step == LockSetupStep.Manage

    val confirmTitle = stringResource(R.string.lock_biometric_confirm_title)
    val confirmOn = stringResource(R.string.lock_biometric_confirm_on)
    val confirmOff = stringResource(R.string.lock_biometric_confirm_off)
    val confirmNegative = stringResource(R.string.dialog_cancel)

    // Both directions of the opt-in go through the sensor, and the flag is written by the prompt's
    // success callback rather than by the tap. Cancel, a finger that never matches and a sensor
    // that has gone away all land in `onFallback`, which leaves the row exactly as it was — the
    // safe outcome for turning it on, and no worse than today for turning it off, since a stuck-on
    // flag still only offers what the gate re-probes for on every show.
    //
    // The negative button is `Cancel`, not `lock_biometric_negative`: there is no PIN fallback to
    // offer here. This screen already sits inside an unlocked session, so the button's only job is
    // to abandon the change.
    fun confirmBiometric(on: Boolean) {
        val target = activity ?: return
        BiometricGate.prompt(
            activity = target,
            title = confirmTitle,
            subtitle = if (on) confirmOn else confirmOff,
            negative = confirmNegative,
            onSuccess = { viewModel.setBiometricEnabled(on) },
            onFallback = {},
        )
    }

    // A half-finished PIN entry backs out to the manage list, not out of the screen entirely.
    BackHandler(enabled = !atRoot) { viewModel.cancel() }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .systemBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.sheet)
                    .border(BorderStroke(2.5.dp, colors.outline), CircleShape)
                    .clickable { if (atRoot) onBack() else viewModel.cancel() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.ds_cd_back),
                    tint = colors.ink,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        when {
            state.justEnabled -> EnabledBody()
            atRoot -> ManageBody(
                state = state,
                biometricAvailable = activity != null && BiometricGate.available(context),
                onSetUp = viewModel::startSetup,
                onToggleBiometric = { confirmBiometric(!state.biometricEnabled) },
                onChangePin = viewModel::startChange,
                onTurnOff = viewModel::startDisable,
            )
            else -> PinEntryBody(
                state = state,
                onDigit = viewModel::onDigit,
                onBackspace = viewModel::onBackspace,
            )
        }
    }
}

@Composable
private fun ManageBody(
    state: LockSetupUiState,
    biometricAvailable: Boolean,
    onSetUp: () -> Unit,
    onToggleBiometric: () -> Unit,
    onChangePin: () -> Unit,
    onTurnOff: () -> Unit,
) {
    val colors = WheregoTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.lock_manage_title),
            style = WheregoType.pageTitle,
            color = colors.ink,
        )
        Text(
            stringResource(R.string.lock_setup_sub),
            style = WheregoType.helper,
            color = colors.muted,
        )
        if (state.enabled) {
            WheregoSettingsCard {
                WheregoSettingRow(
                    icon = Icons.Outlined.Fingerprint,
                    badgeFill = colors.tealSoft,
                    label = stringResource(R.string.lock_row_biometric),
                    onClick = { if (biometricAvailable) onToggleBiometric() },
                    value = when {
                        !biometricAvailable -> stringResource(R.string.lock_biometric_unavailable)
                        state.biometricEnabled -> stringResource(R.string.me_value_on)
                        else -> stringResource(R.string.me_value_off)
                    },
                )
                WheregoSettingDivider()
                WheregoSettingRow(
                    icon = Icons.Outlined.LockReset,
                    badgeFill = colors.violetSoft,
                    label = stringResource(R.string.lock_row_change_pin),
                    onClick = onChangePin,
                )
                WheregoSettingDivider()
                WheregoSettingRow(
                    icon = Icons.Outlined.LockOpen,
                    badgeFill = colors.peach,
                    label = stringResource(R.string.lock_row_turn_off),
                    onClick = onTurnOff,
                )
            }
        } else {
            WheregoPrimaryButton(
                label = stringResource(R.string.lock_setup_title),
                onClick = onSetUp,
                enabled = !state.busy,
            )
        }
    }
}

@Composable
private fun PinEntryBody(
    state: LockSetupUiState,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    val colors = WheregoTheme.colors
    val title = when (state.step) {
        LockSetupStep.Confirm -> R.string.lock_confirm_title
        LockSetupStep.Enter -> R.string.lock_setup_title
        else -> R.string.lock_manage_title
    }
    val subtitle = when (state.step) {
        LockSetupStep.Enter -> R.string.lock_setup_sub
        LockSetupStep.Confirm -> R.string.lock_setup_sub
        LockSetupStep.VerifyToDisable -> R.string.lock_verify_to_disable
        LockSetupStep.VerifyToChange -> R.string.lock_verify_to_change
        LockSetupStep.Manage -> R.string.lock_setup_sub
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PinStage(
            title = stringResource(title),
            subtitle = stringResource(subtitle),
            // Idle throughout: Go has no opinion about a PIN you are choosing, only about one you
            // are being asked for. He reacts to a mismatch through the recoil, same as the gate.
            mood = GoMood.Idle,
            digits = state.digits.length,
            total = AppLock.PIN_LENGTH,
            message = state.message,
            shakeKey = state.shakeKey,
            // Centres in the space above the pad, matching the unlock gate.
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        WheregoPinPad(
            onDigit = onDigit,
            onBackspace = onBackspace,
            modifier = Modifier.widthIn(max = 340.dp),
            enabled = state.cooldownSeconds == 0 && !state.busy,
        )
        Spacer(Modifier.height(28.dp))
    }
}

/** The one beat of celebration, held for [LockSetupViewModel] `CELEBRATION_MILLIS` after enabling. */
@Composable
private fun EnabledBody() {
    val colors = WheregoTheme.colors
    val reduced = LocalReducedMotion.current
    val pop = remember { Animatable(if (reduced) 1f else 0.5f) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow))
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WheregoGoAvatar(
            mood = GoMood.Happy,
            size = 92.dp,
            modifier = Modifier.graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
        )
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.lock_setup_done),
            style = WheregoType.onboardTitle,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
    }
}
