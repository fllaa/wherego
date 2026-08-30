package com.flla.wherego.feature.auth

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.designsystem.component.WheregoCard
import com.flla.wherego.core.designsystem.component.wheregoHardShadow
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType

private const val SIGN_IN_OK = "Backup is on. Capture never waited."

/**
 * `pencil-new.pen` → `Sign In / Proof Card` carries `rotation: -2.5`. pen.dev measures
 * rotation counter-clockwise, Compose `rotationZ` clockwise, so the sign flips.
 */
private const val PROOF_CARD_TILT = 2.5f

/**
 * First-run gate from `pencil-new.pen` → `Sign In`. Backup is offered here, never
 * required: "Try it first, sign in later" drops straight into onboarding, so capture
 * is never blocked on an account. Continue with Google restores an onboarded cloud
 * profile and skips the tour — reinstall is not a new user.
 */
@Composable
fun WelcomeScreen(
    onContinue: (fromBackup: Boolean) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val colors = WheregoTheme.colors
    val context = LocalContext.current
    val authState by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf<String?>(null) }

    fun onGoogle() {
        if (busy) return
        viewModel.signIn(context.findActivity()) { result ->
            message = result
            if (result == SIGN_IN_OK) onContinue(viewModel.fromBackup)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .systemBarsPadding()
            .padding(top = 64.dp, start = 26.dp, end = 26.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                Modifier
                    .size(104.dp)
                    .wheregoHardShadow(shape = CircleShape, color = colors.shadow, offsetY = 5.dp)
                    .clip(CircleShape)
                    .background(colors.teal)
                    .border(BorderStroke(3.dp, colors.ink), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("🪙", fontSize = 46.sp, color = colors.ink)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Wherego", style = WheregoType.wordmark, color = colors.ink)
                Text(
                    "See where your money went.\nLog a spend in seconds.",
                    style = WheregoType.onboardSub.copy(fontSize = 16.sp, lineHeight = 24.sp),
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                )
            }
        }

        ProofCard()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GoogleButton(
                signedIn = authState.signedIn,
                busy = busy,
                onClick = ::onGoogle,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .clickable(enabled = !busy, onClick = { onContinue(false) })
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("Try it first, sign in later", style = WheregoType.chip, color = colors.ink)
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = colors.ink,
                    modifier = Modifier.size(15.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Icon(
                    Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    "No bank connections. You type or snap.",
                    style = WheregoType.helper,
                    color = colors.muted,
                )
            }
            message?.let {
                Text(it, style = WheregoType.helper, color = colors.coral, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ProofCard() {
    val colors = WheregoTheme.colors
    WheregoCard(
        modifier = Modifier
            .graphicsLayer { rotationZ = PROOF_CARD_TILT }
            .wheregoHardShadow(cornerRadius = 28.dp, offsetY = 5.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.mascotFill)
                    .border(BorderStroke(2.dp, colors.ink), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("🪙", fontSize = 18.sp, color = colors.ink)
            }
            Text("Parked. That one won’t vanish.", style = WheregoType.stepText, color = colors.ink)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(colors.track),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(colors.teal)
                    .border(BorderStroke(2.5.dp, colors.ink), RoundedCornerShape(99.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("🍜", fontSize = 15.sp, color = colors.ink)
                Text("Food out", style = WheregoType.stepText, color = Color.White)
            }
            Text("Rp 18.000", style = WheregoType.balanceValue.copy(fontSize = 26.sp), color = colors.ink)
        }
    }
}

@Composable
private fun GoogleButton(signedIn: Boolean, busy: Boolean, onClick: () -> Unit) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .wheregoHardShadow(shape = shape, color = colors.shadow, offsetY = 4.dp)
            .clip(shape)
            .background(colors.sheet)
            .border(BorderStroke(2.5.dp, colors.ink), shape)
            .clickable(enabled = !busy, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text("G", style = WheregoType.buttonLabel.copy(fontSize = 22.sp), color = Color(0xFF4285F4))
        Spacer(Modifier.width(11.dp))
        Text(
            when {
                busy -> "Restoring…"
                signedIn -> "Continue"
                else -> "Continue with Google"
            },
            style = WheregoType.buttonLabel.copy(fontSize = 17.sp),
            color = colors.ink,
        )
    }
}
