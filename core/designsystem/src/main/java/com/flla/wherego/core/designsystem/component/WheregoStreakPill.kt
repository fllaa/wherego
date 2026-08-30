package com.flla.wherego.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType

@Composable
fun WheregoStreakPill(
    days: Int,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    Row(
        modifier
            .border(2.dp, colors.ink, RoundedCornerShape(99.dp))
            .background(colors.mascotFill, RoundedCornerShape(99.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("🔥", fontSize = 14.sp, color = colors.coral)
        Text(
            text = days.toString(),
            style = WheregoType.greeting.copy(fontSize = 15.sp),
            color = colors.ink,
        )
    }
}
