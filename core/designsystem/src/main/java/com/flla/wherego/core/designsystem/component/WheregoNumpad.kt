package com.flla.wherego.core.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.designsystem.theme.LocalReducedMotion
import com.flla.wherego.core.i18n.R

@Composable
fun WheregoNumpad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "⌫"),
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                row.forEach { key ->
                    NumpadKey(
                        label = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (key == "⌫") onBackspace() else onDigit(key)
                        },
                    )
                }
            }
        }
    }
}

/**
 * [pressAnimated] squashes the key toward the finger on touch-down.
 *
 * Off by default, so the money pad in the capture sheet renders exactly as it always has. That pad
 * commits real ledger rows, and putting movement under a finger that is entering an amount is a
 * behavioural change to a surface nobody asked about.
 */
@Composable
private fun NumpadKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pressAnimated: Boolean = false,
) {
    val colors = WheregoTheme.colors
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val reduced = LocalReducedMotion.current
    val animate = pressAnimated && !reduced
    val scale by animateFloatAsState(
        targetValue = if (animate && pressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "keyPress",
    )
    Box(
        modifier
            .height(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(colors.key)
            .clickable(
                interactionSource = interactions,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (label == "⌫") {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Backspace,
                contentDescription = stringResource(R.string.ds_cd_backspace),
                tint = colors.ink,
            )
        } else {
            Text(
                text = label,
                style = if (label == "000") WheregoType.key.copy(fontSize = 20.sp) else WheregoType.key,
                color = colors.ink,
            )
        }
    }
}

/**
 * The app-lock keypad. Same [NumpadKey] as [WheregoNumpad], so the two pads cannot drift apart,
 * but without its `000` key — that key exists for entering money, and a PIN digit is not an
 * amount. A `mode` flag on [WheregoNumpad] would have put a lock concern inside the money pad.
 *
 * Unlike the money pad, this one is springy and ticks under the finger: entering a PIN is the one
 * keypad interaction in the app with no on-screen number growing to confirm the press landed, so
 * the key itself has to answer.
 *
 * [enabled] goes false during a failure cooldown. It drops the click handlers and nothing else:
 * greying the keys out would make the pad flash on every wrong PIN, and the countdown line above
 * it already says why nothing is responding.
 */
@Composable
fun WheregoPinPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫"),
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        NumpadKey(
                            label = key,
                            modifier = Modifier.weight(1f),
                            pressAnimated = true,
                            onClick = {
                                if (enabled) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (key == "⌫") onBackspace() else onDigit(key)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * PIN progress: [total] dots, the first [filled] of them solid.
 *
 * Each dot springs past its final size on the way in, so a digit landing is visible in peripheral
 * vision while the eye is still on the keypad. Turns coral wholesale on [error], because a wrong
 * PIN is one rejected attempt and not six individually bad digits.
 */
@Composable
fun WheregoPinDots(
    filled: Int,
    total: Int,
    modifier: Modifier = Modifier,
    error: Boolean = false,
) {
    val colors = WheregoTheme.colors
    val reduced = LocalReducedMotion.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(total) { index ->
            val on = index < filled
            val progress by animateFloatAsState(
                targetValue = if (on) 1f else 0f,
                animationSpec = if (reduced) {
                    spring(stiffness = Spring.StiffnessHigh)
                } else {
                    spring(
                        dampingRatio = 0.42f,
                        stiffness = Spring.StiffnessMediumLow,
                    )
                },
                label = "pinDot$index",
            )
            val target = if (error) colors.coral else colors.ink
            Box(
                Modifier
                    .size(14.dp)
                    .graphicsLayer {
                        // Floor at 0.82, not near-zero: an empty slot still has to read as one of
                        // six waiting for a digit. The pop comes from the spring carrying progress
                        // past 1 for a beat, plus the track-to-ink colour jump.
                        val s = 0.82f + 0.18f * progress
                        scaleX = s
                        scaleY = s
                    }
                    .clip(CircleShape)
                    .background(
                        if (error) colors.coral else lerp(colors.track, target, progress.coerceIn(0f, 1f)),
                    ),
            )
        }
    }
}
