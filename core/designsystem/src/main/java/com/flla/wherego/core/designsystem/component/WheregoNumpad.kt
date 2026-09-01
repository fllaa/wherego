package com.flla.wherego.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.flla.wherego.core.i18n.R
import androidx.compose.ui.unit.sp
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType

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

@Composable
private fun NumpadKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    Box(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.key)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
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
                            onClick = {
                                if (enabled) {
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
 * PIN progress: [total] dots, the first [filled] of them solid. Turns coral wholesale on [error]
 * so a wrong PIN reads as one rejected attempt rather than as individual bad digits.
 */
@Composable
fun WheregoPinDots(
    filled: Int,
    total: Int,
    modifier: Modifier = Modifier,
    error: Boolean = false,
) {
    val colors = WheregoTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(total) { index ->
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            error -> colors.coral
                            index < filled -> colors.ink
                            else -> colors.track
                        },
                    ),
            )
        }
    }
}
