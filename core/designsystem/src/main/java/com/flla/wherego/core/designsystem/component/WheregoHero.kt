package com.flla.wherego.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.flla.wherego.core.i18n.R
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType

@Composable
fun WheregoHero(
    amountLabel: String,
    modifier: Modifier = Modifier,
    incomeLabel: String? = null,
    leftLabel: String? = null,
) {
    val colors = WheregoTheme.colors
    Column(modifier) {
        Text(
            text = stringResource(R.string.ds_hero_spent_this_month),
            style = WheregoType.eyebrow,
            color = colors.muted,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = amountLabel,
            style = WheregoType.heroAmount,
            color = colors.ink,
        )
        if (!incomeLabel.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(incomeLabel, style = WheregoType.meta, color = colors.muted)
                if (!leftLabel.isNullOrBlank()) {
                    Text(
                        leftLabel,
                        style = WheregoType.leftPill,
                        color = colors.tealDeep,
                        modifier = Modifier
                            .background(colors.tealSoft, RoundedCornerShape(99.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
