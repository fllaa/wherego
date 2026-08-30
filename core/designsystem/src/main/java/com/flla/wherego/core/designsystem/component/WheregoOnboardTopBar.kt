package com.flla.wherego.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType

/**
 * Onboarding chrome from `pencil-new.pen` → `Onboarding N · … / Top Bar`:
 * a 38dp ink-outlined back circle, a 4-dot progress rail whose active dot is a
 * 24x9 ink pill, and a muted "Skip" affordance.
 */
@Composable
fun WheregoOnboardTopBar(
    stepIndex: Int,
    stepCount: Int,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.sheet)
                .border(BorderStroke(2.5.dp, colors.ink), CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = colors.ink,
                modifier = Modifier.size(19.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(stepCount) { index ->
                val active = index == stepIndex
                Box(
                    Modifier
                        .width(if (active) 24.dp else 9.dp)
                        .height(9.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (active) colors.ink else colors.track),
                )
            }
        }
        Text(
            "Skip",
            style = WheregoType.chip,
            color = colors.muted,
            modifier = Modifier.clickable(onClick = onSkip),
        )
    }
}
