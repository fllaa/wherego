package com.flla.wherego.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType

/**
 * Shared sticker-sheet from `pencil-new.pen` capture/Me chrome:
 * 36dp top corners, 42x3 track grabber, skip partial expand.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheregoBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WheregoTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.sheet,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        dragHandle = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 42.dp, height = 3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(colors.track),
                )
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = WheregoType.cardTitle, color = colors.ink)
            content()
        }
    }
}

private val Pill = RoundedCornerShape(99.dp)

/** `Header / Page Title` — 26sp display title with an optional trailing control. */
@Composable
fun WheregoPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = WheregoType.pageTitle, color = WheregoTheme.colors.ink)
        trailing?.invoke()
    }
}

/** `Stories / Month Switcher` — label plus a prev/next chevron pair in one ink-outlined pill. */
@Composable
fun WheregoMonthStepper(
    label: String,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = WheregoTheme.colors
    Row(
        Modifier
            .clip(Pill)
            .background(colors.sheet)
            .border(BorderStroke(2.5.dp, colors.ink), Pill)
            .padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = WheregoType.monthLabel, color = colors.ink)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = colors.muted,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onPrev),
            )
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = if (canGoNext) colors.ink else colors.muted,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(enabled = canGoNext, onClick = onNext),
            )
        }
    }
}

/** `Plan / Month Pill` — label plus a single down chevron. */
@Composable
fun WheregoMonthPill(label: String, onClick: () -> Unit) {
    val colors = WheregoTheme.colors
    Row(
        Modifier
            .clip(Pill)
            .background(colors.sheet)
            .border(BorderStroke(2.5.dp, colors.ink), Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = WheregoType.monthLabel, color = colors.ink)
        Icon(
            Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Section title (17sp display) with an optional hint beside it and a trailing value/link. */
@Composable
fun WheregoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    trailing: String? = null,
    trailingColor: Color? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    val colors = WheregoTheme.colors
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = WheregoType.cardTitle, color = colors.ink)
            if (hint != null) Text(hint, style = WheregoType.helper, color = colors.muted)
        }
        if (trailing != null) {
            Text(
                trailing,
                style = WheregoType.link,
                color = trailingColor ?: colors.muted,
                modifier = if (onTrailingClick != null) {
                    Modifier.clickable(onClick = onTrailingClick)
                } else {
                    Modifier
                },
            )
        }
    }
}

/** `YOUR MONEY` / `APP` / `DATA` — 12sp extrabold, 1.1sp tracking, muted. */
@Composable
fun WheregoSectionLabel(text: String) {
    Text(text, style = WheregoType.groupLabel, color = WheregoTheme.colors.muted)
}

/** Rounded track with a proportional fill. `fraction` is clamped to `0f..1f`. */
@Composable
fun WheregoMeter(
    fraction: Float,
    fillColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
) {
    val colors = WheregoTheme.colors
    val safe = fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(Pill)
            .background(colors.track),
    ) {
        if (safe > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .layout { measurable, constraints ->
                        val w = (constraints.maxWidth * safe).toInt().coerceAtLeast(1)
                        val placeable = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                    .clip(Pill)
                    .background(fillColor),
            )
        }
    }
}

/** Emoji or icon chip used as a leading badge on cards and rows. */
@Composable
fun WheregoBadge(
    fill: Color,
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    strokeColor: Color? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(fill)
            .then(
                if (strokeColor != null) {
                    Modifier.border(BorderStroke(2.dp, strokeColor), RoundedCornerShape(cornerRadius))
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** `Me / Stats Card` — evenly weighted value/label columns split by 2x34 dividers. */
@Composable
fun WheregoStatsCard(stats: List<Pair<String, String>>) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.sheet)
            .border(BorderStroke(2.5.dp, colors.ink), shape)
            .padding(horizontal = 10.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stats.forEachIndexed { index, (value, label) ->
            if (index > 0) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(34.dp)
                        .clip(Pill)
                        .background(colors.track),
                )
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(value, style = WheregoType.statValue, color = colors.ink)
                Text(label, style = WheregoType.statLabel, color = colors.muted)
            }
        }
    }
}

/** `Me / YOUR MONEY Card` — an ink-outlined card that hairline-divides its rows. */
@Composable
fun WheregoSettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.sheet)
            .border(BorderStroke(2.5.dp, colors.ink), shape)
            .padding(vertical = 3.dp),
    ) { content() }
}

@Composable
fun WheregoSettingDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(WheregoTheme.colors.divider),
    )
}

/** A 46dp `Me` row: soft-filled icon badge, label, optional value, chevron. */
@Composable
fun WheregoSettingRow(
    icon: ImageVector,
    badgeFill: Color,
    label: String,
    onClick: () -> Unit,
    value: String? = null,
) {
    val colors = WheregoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        WheregoBadge(fill = badgeFill, size = 28.dp, cornerRadius = 14.dp) {
            Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(15.dp))
        }
        Text(label, style = WheregoType.settingLabel, color = colors.ink, modifier = Modifier.weight(1f))
        if (value != null) Text(value, style = WheregoType.helper, color = colors.muted)
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Ink-outlined row card carrying a leading badge, two-line text and a trailing slot. */
@Composable
fun WheregoMeterCard(
    emoji: String,
    badgeFill: Color,
    name: String,
    detail: String,
    fraction: Float,
    fillColor: Color,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 34.dp,
    padding: Dp = 12.dp,
    trailing: @Composable RowScope.() -> Unit,
) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.sheet)
            .border(BorderStroke(2.5.dp, colors.ink), shape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                WheregoBadge(fill = badgeFill, size = badgeSize, cornerRadius = badgeSize / 2) {
                    Text(emoji, fontSize = (badgeSize.value * 0.47f).sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(name, style = WheregoType.txTitle, color = colors.ink)
                    Text(detail, style = WheregoType.meterDetail, color = colors.muted)
                }
            }
            trailing()
        }
        WheregoMeter(fraction = fraction, fillColor = fillColor, height = 10.dp)
    }
}

/** `Plan / Cap Card` — the deep-teal month hero with an inverted track. */
@Composable
fun WheregoCapCard(
    label: String,
    amount: String,
    fraction: Float,
    footLabel: String,
    pillLabel: String?,
) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(28.dp)
    val safe = fraction.coerceIn(0f, 1f)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.capFill)
            .border(BorderStroke(2.5.dp, colors.ink), shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(label, style = WheregoType.eyebrow.copy(fontSize = 13.sp), color = colors.capLabel)
        Text(amount, style = WheregoType.heroAmount.copy(fontSize = 40.sp, lineHeight = 50.sp), color = Color.White)
        Box(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(Pill)
                .background(colors.capTrack),
        ) {
            if (safe > 0f) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .layout { measurable, constraints ->
                            val w = (constraints.maxWidth * safe).toInt().coerceAtLeast(1)
                            val placeable = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        }
                        .clip(Pill)
                        .background(colors.paper),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(footLabel, style = WheregoType.helper, color = colors.capLabel)
            if (pillLabel != null) {
                Text(
                    pillLabel,
                    style = WheregoType.helper,
                    color = colors.ink,
                    modifier = Modifier
                        .clip(Pill)
                        .background(colors.paper)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}
