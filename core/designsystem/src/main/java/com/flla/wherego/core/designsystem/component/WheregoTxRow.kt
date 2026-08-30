package com.flla.wherego.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.designsystem.theme.parseHexColor

private val CardShape = RoundedCornerShape(22.dp)

@Composable
fun WheregoTxRow(
    emoji: String,
    title: String,
    subtitle: String,
    amountLabel: String,
    badgeSoftHex: String,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .border(2.5.dp, colors.ink, CardShape)
            .background(colors.white, CardShape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .background(parseHexColor(badgeSoftHex), RoundedCornerShape(21.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 19.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = WheregoType.txTitle, color = colors.ink)
            Text(subtitle, style = WheregoType.helper, color = colors.muted)
        }
        Text(amountLabel, style = WheregoType.txAmount, color = colors.ink)
    }
}
