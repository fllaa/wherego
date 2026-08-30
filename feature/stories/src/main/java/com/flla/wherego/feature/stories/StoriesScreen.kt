package com.flla.wherego.feature.stories

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.designsystem.theme.parseHexColor
import com.flla.wherego.core.model.CategorySpend
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.UserProfile
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
@Composable
fun StoriesRoute(
    viewModel: StoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StoriesScreen(
        state = state,
        onPrev = viewModel::prevMonth,
        onNext = viewModel::nextMonth,
    )
}

@Composable
fun StoriesScreen(
    state: StoriesUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = colors.ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .clickable(onClick = onPrev)
                    .padding(8.dp),
            )
            Text(state.title, style = WheregoType.cardTitle, color = colors.ink)
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = if (state.canGoNext) colors.ink else colors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .clickable(enabled = state.canGoNext, onClick = onNext)
                    .padding(8.dp),
            )
        }
        Text("Spent this month", style = WheregoType.eyebrow, color = colors.muted)
        Text(state.totalLabel, style = WheregoType.heroAmount, color = colors.ink)
        Text(state.sentence, style = WheregoType.meta, color = colors.ink)
        if (state.balance.size >= 2) {
            Text("Balance", style = WheregoType.chip, color = colors.ink)
            val ys = state.balance.map { it.balanceMinor.toFloat() }
            Chart(
                chart = lineChart(),
                model = entryModelOf(*ys.toTypedArray()),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            )
        }
        val context = LocalContext.current
        Text(
            "Share PDF",
            style = WheregoType.cta,
            color = colors.tealDeep,
            modifier = Modifier.clickable {
                val uri = MonthPdfWriter.write(context, "wherego-${state.yearMonth}.pdf", state.pdfLines)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "Share month"))
            },
        )
        if (state.bars.isEmpty()) {
            Text("No category bars yet.", style = WheregoType.meta, color = colors.muted)
        } else {
            state.bars.forEach { bar ->
                CategoryBar(bar)
            }
        }
    }
}

@Composable
private fun CategoryBar(bar: CategorySpend) {
    val colors = WheregoTheme.colors
    val fraction = (bar.percent / 100f).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${bar.emoji}  ${bar.name}", style = WheregoType.chip, color = colors.ink)
            Text(
                "${bar.percent}% · ${MoneyFormatter.format(bar.amountMinor, UserProfile.DEFAULT_CURRENCY)}",
                style = WheregoType.meta,
                color = colors.muted,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(13.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(colors.track),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(13.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(parseHexColor(bar.colorHex)),
            )
        }
    }
}
