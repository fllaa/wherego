package com.flla.wherego.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.flla.wherego.core.i18n.LocalAmountsHidden
import com.flla.wherego.core.i18n.R
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType

/**
 * The month hero. [amountLabel], [incomeLabel] and [leftLabel] arrive already masked when
 * `Me → Hide amounts` is on — the caller owns that, the same as every other amount on screen.
 *
 * Passing [onToggleAmounts] adds the eye next to the eyebrow, so the number can be revealed from
 * where it is read instead of a round trip through settings.
 */
@Composable
fun WheregoHero(
    amountLabel: String,
    modifier: Modifier = Modifier,
    incomeLabel: String? = null,
    leftLabel: String? = null,
    onToggleAmounts: (() -> Unit)? = null,
) {
    val colors = WheregoTheme.colors
    val hidden = LocalAmountsHidden.current
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.ds_hero_spent_this_month),
                style = WheregoType.eyebrow,
                color = colors.muted,
            )
            if (onToggleAmounts != null) {
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(
                        if (hidden) R.string.ds_cd_show_amounts else R.string.ds_cd_hide_amounts,
                    ),
                    tint = colors.muted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .clickable(onClick = onToggleAmounts)
                        .padding(6.dp)
                        .size(18.dp),
                )
            }
        }
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
