package com.flla.wherego.feature.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flla.wherego.core.designsystem.component.GoMood
import com.flla.wherego.core.designsystem.component.WheregoGoAvatar
import com.flla.wherego.core.designsystem.component.WheregoPinDots
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.designsystem.theme.LocalReducedMotion
import kotlin.math.abs

/**
 * Everything above the keypad on every PIN surface: Go, the title, the dots and the message line.
 *
 * Shared by the unlock gate and by all four setup steps, so the reaction to a rejected PIN is
 * defined exactly once. Before this existed, the gate and the setup screen carried two copies of
 * the same stack, which is two places for the choreography to drift.
 *
 * [shakeKey] is a counter, not a boolean: two wrong PINs in a row have to shake twice, and a flag
 * that is already `true` cannot re-fire.
 */
@Composable
fun PinStage(
    title: String,
    subtitle: String,
    mood: GoMood,
    digits: Int,
    total: Int,
    message: LockMessage?,
    shakeKey: Int,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    val haptics = LocalHapticFeedback.current
    val reduced = LocalReducedMotion.current
    val shake = remember { Animatable(0f) }

    LaunchedEffect(shakeKey) {
        if (shakeKey == 0) return@LaunchedEffect
        // Fires even under reduced motion: refusing a PIN is feedback, not decoration.
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (reduced) return@LaunchedEffect
        shake.snapTo(0f)
        shake.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 420
                0f at 0
                -13f at 55
                11f at 115
                -8f at 175
                6f at 235
                -3f at 300
                0f at 420
            },
        )
    }

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WheregoGoAvatar(
            mood = mood,
            size = 76.dp,
            modifier = Modifier.graphicsLayer {
                // Go recoils rather than sliding with the dots, so the rejection reads as him
                // flinching at the wrong PIN instead of the whole screen twitching.
                rotationZ = shake.value * -0.5f
                val dip = 1f - abs(shake.value) / 260f
                scaleX = dip
                scaleY = dip
            },
        )
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = WheregoType.onboardTitle,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = WheregoType.helper,
            color = colors.muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        WheregoPinDots(
            filled = digits,
            total = total,
            error = message != null,
            modifier = Modifier.graphicsLayer { translationX = shake.value },
        )
        Spacer(Modifier.height(12.dp))
        // Reserved height, so nothing shifts when a message appears or clears.
        Column(
            Modifier
                .fillMaxWidth()
                .height(34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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
}
