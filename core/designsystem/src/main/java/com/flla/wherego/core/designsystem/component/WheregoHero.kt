package com.flla.wherego.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType

@Composable
fun WheregoHero(
    amountLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    Column(modifier) {
        Text(
            text = "Spent this month",
            style = WheregoType.eyebrow,
            color = colors.muted,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = amountLabel,
            style = WheregoType.heroAmount,
            color = colors.ink,
        )
    }
}
