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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.designsystem.theme.parseHexColor
import com.flla.wherego.core.i18n.R

private val CardShape = RoundedCornerShape(22.dp)

/**
 * Which way a row's amount moves the pot, as the row draws it.
 *
 * The design system stays clear of the domain vocabulary, so callers map their own kind onto a
 * tone — `TxAmountTone.ofPolarity(TransactionKind.polarity(kind))` — which keeps the polarity
 * rule itself in one place in `:core:model`.
 */
enum class TxAmountTone {
    /** Money out. Drawn plain, since spending is the common case and needs no decoration. */
    Out,

    /** Money in. Drawn with a `+` and the accent, so it cannot be mistaken for a spend. */
    In,

    /**
     * Moves nothing — a balance assertion, or a kind this build does not know. Drawn muted so it
     * reads as bookkeeping rather than as a spend of that size.
     */
    Neutral,
    ;

    companion object {
        /** Maps a domain polarity (`-1` out, `+1` in, `0` neither) onto a tone. */
        fun ofPolarity(polarity: Int): TxAmountTone = when {
            polarity > 0 -> In
            polarity < 0 -> Out
            else -> Neutral
        }
    }
}

@Composable
fun WheregoTxRow(
    emoji: String,
    title: String,
    subtitle: String,
    amountLabel: String,
    tone: TxAmountTone,
    badgeSoftHex: String,
    modifier: Modifier = Modifier,
    hasReceipt: Boolean = false,
) {
    val colors = WheregoTheme.colors
    // Blank means "no colour of its own" — every preset category shares one soft hex, and painting
    // that on the badge would only restate `tealSoft` in light mode while burning a light pastel
    // onto dark paper in dark mode. Custom categories carry a real choice, so they win.
    val badgeColor = remember(badgeSoftHex, colors.tealSoft) {
        if (badgeSoftHex.isBlank()) colors.tealSoft else parseHexColor(badgeSoftHex)
    }
    Row(
        modifier
            .fillMaxWidth()
            .border(2.5.dp, colors.outline, CardShape)
            .background(colors.white, CardShape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .background(badgeColor, RoundedCornerShape(21.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 19.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = WheregoType.txTitle, color = colors.ink)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(subtitle, style = WheregoType.helper, color = colors.muted)
                if (hasReceipt) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = stringResource(R.string.receipt_parked_title),
                        tint = colors.accentText,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
        Text(
            // The sign carries the distinction without relying on colour alone.
            if (tone == TxAmountTone.In) "+$amountLabel" else amountLabel,
            style = WheregoType.txAmount,
            color = when (tone) {
                TxAmountTone.In -> colors.tealDeep
                TxAmountTone.Out -> colors.ink
                TxAmountTone.Neutral -> colors.muted
            },
        )
    }
}
