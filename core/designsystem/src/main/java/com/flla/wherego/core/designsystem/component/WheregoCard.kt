package com.flla.wherego.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flla.wherego.core.designsystem.theme.WheregoTheme

/**
 * The ink-outlined surface card the whole `pencil-new.pen` document is built on:
 * `fill: $surface`, `stroke: $ink / 2.5`, generous corner radius, 18dp padding.
 */
@Composable
fun WheregoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    padding: Dp = 18.dp,
    gap: Dp = 13.dp,
    strokeColor: Color? = null,
    strokeWidth: Dp = 2.5.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.sheet)
            .border(BorderStroke(strokeWidth, strokeColor ?: colors.ink), shape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(gap),
        horizontalAlignment = horizontalAlignment,
    )
    { content() }
}
