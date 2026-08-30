package com.flla.wherego.feature.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.designsystem.component.WheregoBadge
import com.flla.wherego.core.designsystem.component.WheregoCard
import com.flla.wherego.core.designsystem.component.WheregoMeter
import com.flla.wherego.core.designsystem.component.WheregoMonthStepper
import com.flla.wherego.core.designsystem.component.WheregoPageHeader
import com.flla.wherego.core.designsystem.component.WheregoSectionHeader
import com.flla.wherego.core.designsystem.component.WheregoTxRow
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.designsystem.theme.parseHexColor
import com.flla.wherego.core.model.TransactionKind
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.entry.entryModelOf

private val Pill = RoundedCornerShape(99.dp)
private val CardShape = RoundedCornerShape(28.dp)

/** The `Day List / Filter Pill` states, cycled in place by tapping the pill. */
private enum class TxFilter(val label: String) {
    All("All"),
    Expense("Expense"),
    Income("Income"),
    ;

    fun next(): TxFilter = TxFilter.values()[(ordinal + 1) % TxFilter.values().size]

    fun keeps(kind: String): Boolean = when (this) {
        All -> true
        Expense -> kind == TransactionKind.EXPENSE
        Income -> kind == TransactionKind.INCOME
    }
}

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
    var filter by remember { mutableStateOf(TxFilter.All) }
    val days = remember(state.days, filter) {
        if (filter == TxFilter.All) {
            state.days
        } else {
            state.days.mapNotNull { day ->
                day.transactions
                    .filter { filter.keeps(it.kind) }
                    .takeIf { it.isNotEmpty() }
                    ?.let { day.copy(transactions = it) }
            }
        }
    }
    Column(
        modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WheregoPageHeader("Stories") {
            WheregoMonthStepper(
                label = state.monthLabel,
                canGoNext = state.canGoNext,
                onPrev = onPrev,
                onNext = onNext,
            )
        }

        WheregoCard(gap = 2.dp) {
            Text("Spent in ${state.monthLabel}", style = WheregoType.eyebrow, color = colors.muted)
            Text(
                state.totalLabel,
                style = WheregoType.heroAmount.copy(fontSize = 40.sp, lineHeight = 48.sp),
                color = colors.ink,
            )
            if (state.deltaLabel != null) {
                Row(
                    Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val onPill = if (state.deltaIsLess) colors.onGreenSoft else colors.ink
                    Row(
                        Modifier
                            .clip(Pill)
                            .background(if (state.deltaIsLess) colors.greenSoft else colors.peach)
                            .padding(horizontal = 11.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            if (state.deltaIsLess) {
                                Icons.AutoMirrored.Outlined.TrendingDown
                            } else {
                                Icons.AutoMirrored.Outlined.TrendingUp
                            },
                            contentDescription = null,
                            tint = onPill,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(state.deltaLabel, style = WheregoType.leftPill, color = onPill)
                    }
                    Text("than ${state.prevMonthLabel}", style = WheregoType.meta, color = colors.muted)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(colors.amberSoft)
                .border(2.5.dp, colors.ink, CardShape)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WheregoBadge(
                fill = colors.sheet,
                size = 40.dp,
                cornerRadius = 20.dp,
                strokeColor = colors.ink,
            ) {
                Text("🪙", fontSize = 20.sp)
            }
            Text(
                state.sentence,
                style = WheregoType.meta.copy(fontSize = 14.sp, lineHeight = 20.sp),
                color = colors.ink,
            )
        }

        WheregoCard(padding = 16.dp, gap = 10.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Where it went", style = WheregoType.cardTitle, color = colors.ink)
                Text("${state.logCount} logs", style = WheregoType.leftPill, color = colors.muted)
            }
            if (state.bars.isEmpty()) {
                Text("No category bars yet.", style = WheregoType.meta, color = colors.muted)
            } else {
                state.bars.forEach { bar -> BreakdownRow(bar) }
            }
        }

        if (state.days.isEmpty()) {
            WheregoCard {
                Text("Nothing logged this month yet.", style = WheregoType.meta, color = colors.muted)
            }
        } else if (days.isEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "No ${filter.label.lowercase()} logs this month.",
                    style = WheregoType.meta,
                    color = colors.muted,
                )
                FilterPill(filter) { filter = filter.next() }
            }
        } else {
            days.forEachIndexed { index, day ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(day.dayTitle, style = WheregoType.cardTitle, color = colors.ink)
                            Text(
                                if (filter == TxFilter.Income) day.dayIncomeLabel else day.dayTotalLabel,
                                style = WheregoType.link,
                                color = colors.muted,
                            )
                        }
                        if (index == 0) {
                            FilterPill(filter) { filter = filter.next() }
                        }
                    }
                    day.transactions.forEach { tx ->
                        WheregoTxRow(
                            emoji = tx.emoji,
                            title = tx.title,
                            subtitle = tx.subtitle,
                            amountLabel = tx.amountLabel,
                            badgeSoftHex = tx.badgeSoftHex,
                        )
                    }
                }
            }
        }

        if (state.balance.size >= 2) {
            WheregoSectionHeader("Balance")
            WheregoCard {
                val ys = state.balance.map { it.balanceMinor.toFloat() }
                Chart(
                    chart = lineChart(
                        lines = listOf(
                            lineSpec(lineColor = colors.teal, lineBackgroundShader = null),
                        ),
                    ),
                    model = entryModelOf(*ys.toTypedArray()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                )
            }
        }
    }
}

@Composable
private fun BreakdownRow(bar: StoryBar) {
    val colors = WheregoTheme.colors
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(bar.emoji, fontSize = 15.sp)
                Text(
                    bar.name,
                    style = WheregoType.chip,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(bar.amountLabel, style = WheregoType.barAmount, color = colors.ink)
            Text(bar.percentLabel, style = WheregoType.leftPill, color = colors.muted)
        }
        WheregoMeter(fraction = bar.fraction, fillColor = parseHexColor(bar.colorHex))
    }
}

@Composable
private fun FilterPill(filter: TxFilter, onClick: () -> Unit) {
    val colors = WheregoTheme.colors
    Row(
        Modifier
            .clip(Pill)
            .background(colors.sheet)
            .border(2.dp, colors.ink, Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Outlined.Tune,
            contentDescription = "Filter logs",
            tint = colors.ink,
            modifier = Modifier.size(14.dp),
        )
        Text(filter.label, style = WheregoType.leftPill, color = colors.ink)
    }
}
