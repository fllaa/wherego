package com.flla.wherego.feature.stories

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.flla.wherego.core.designsystem.component.WheregoTxRow
import com.flla.wherego.core.designsystem.component.WheregoWaypointMark
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.i18n.R
import com.flla.wherego.core.i18n.categoryDisplayName
import com.flla.wherego.core.i18n.dayTitle
import com.flla.wherego.core.i18n.monthLabel
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.StoryHeadline
import com.flla.wherego.core.model.TransactionKind
import java.time.LocalDate
import kotlin.math.abs

private val Pill = RoundedCornerShape(99.dp)
private val CardShape = RoundedCornerShape(28.dp)

/** The `Day List / Filter Pill` states, cycled in place by tapping the pill. */
private enum class TxFilter(@StringRes val labelRes: Int) {
    All(R.string.filter_all),
    Expense(R.string.filter_expense),
    Income(R.string.filter_income),
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
    val shownMonth = monthLabel(state.month, state.currentMonth)
    Column(
        modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WheregoPageHeader(stringResource(R.string.stories_title)) {
            WheregoMonthStepper(
                label = shownMonth,
                canGoNext = state.canGoNext,
                onPrev = onPrev,
                onNext = onNext,
            )
        }

        WheregoCard(gap = 2.dp) {
            val deltaMinor = state.deltaMinor
            if (deltaMinor == null) {
                // No previous month to compare against, so the month's own total leads.
                Text(
                    stringResource(R.string.plan_spent_in_month, shownMonth),
                    style = WheregoType.eyebrow,
                    color = colors.muted,
                )
                Text(
                    state.totalLabel,
                    style = WheregoType.heroAmount.copy(fontSize = 40.sp, lineHeight = 48.sp),
                    color = colors.ink,
                )
            } else {
                Text(
                    stringResource(
                        R.string.stories_vs_prev,
                        monthLabel(state.prevMonth, state.currentMonth),
                    ),
                    style = WheregoType.eyebrow,
                    color = colors.muted,
                )
                Text(
                    MoneyFormatter.format(abs(deltaMinor), state.currency),
                    style = WheregoType.heroAmount.copy(fontSize = 40.sp, lineHeight = 48.sp),
                    color = colors.ink,
                )
                Row(
                    Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val less = state.deltaIsLess
                    val onPill = if (less) colors.onGreenSoft else colors.ink
                    Row(
                        Modifier
                            .clip(Pill)
                            .background(if (less) colors.greenSoft else colors.peach)
                            .padding(horizontal = 11.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            if (less) {
                                Icons.AutoMirrored.Outlined.TrendingDown
                            } else {
                                Icons.AutoMirrored.Outlined.TrendingUp
                            },
                            contentDescription = null,
                            tint = onPill,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            stringResource(
                                if (less) R.string.stories_delta_less else R.string.stories_delta_more,
                            ),
                            style = WheregoType.leftPill,
                            color = onPill,
                        )
                    }
                    Text(
                        stringResource(
                            R.string.stories_total_spent_in,
                            state.totalLabel,
                            shownMonth,
                        ),
                        style = WheregoType.meta,
                        color = colors.muted,
                        modifier = Modifier.weight(1f, fill = false),
                    )
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
                WheregoWaypointMark(modifier = Modifier.size(30.dp))
            }
            Text(
                headlineText(state.headline),
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
                Text(
                    stringResource(R.string.stories_section_where),
                    style = WheregoType.cardTitle,
                    color = colors.ink,
                )
                Text(
                    pluralStringResource(R.plurals.stories_log_count, state.logCount, state.logCount),
                    style = WheregoType.leftPill,
                    color = colors.muted,
                )
            }
            if (state.bars.isEmpty()) {
                Text(
                    stringResource(R.string.stories_empty_bars),
                    style = WheregoType.meta,
                    color = colors.muted,
                )
            } else {
                state.bars.forEach { bar -> BreakdownRow(bar) }
            }
        }

        if (state.days.isEmpty()) {
            WheregoCard {
                Text(
                    stringResource(R.string.stories_empty_month),
                    style = WheregoType.meta,
                    color = colors.muted,
                )
            }
        } else if (days.isEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    filterEmptyText(filter),
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
                            Text(
                                dayTitle(LocalDate.parse(day.date)),
                                style = WheregoType.cardTitle,
                                color = colors.ink,
                            )
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
                        val name = storyTxName(tx)
                        WheregoTxRow(
                            emoji = tx.emoji,
                            title = tx.note.ifBlank { name },
                            subtitle = tx.time?.let { "$it · $name" } ?: name,
                            amountLabel = tx.amountLabel,
                            badgeSoftHex = tx.badgeSoftHex,
                            hasReceipt = tx.hasReceipt,
                        )
                    }
                }
            }
        }

        val balance = state.balance
        if (balance != null) {
            WheregoCard(padding = 16.dp, gap = 12.dp) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(R.string.stories_section_balance),
                        style = WheregoType.cardTitle,
                        color = colors.ink,
                    )
                    Text(balance.nowLabel, style = WheregoType.statValue, color = colors.ink)
                }
                BalanceSparkline(balance)
                if (balance.isFlat) {
                    Text(
                        stringResource(R.string.stories_balance_flat),
                        style = WheregoType.helper,
                        color = colors.muted,
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.stories_balance_low, balance.lowLabel),
                            style = WheregoType.helper,
                            color = colors.muted,
                        )
                        Text(
                            stringResource(R.string.stories_balance_high, balance.highLabel),
                            style = WheregoType.helper,
                            color = colors.muted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun headlineText(headline: StoryHeadline): String = when (headline) {
    StoryHeadline.Empty -> stringResource(R.string.story_headline_empty)
    is StoryHeadline.One -> stringResource(
        R.string.story_headline_one,
        categoryDisplayName(headline.categoryId, headline.name),
        headline.percent,
    )
    is StoryHeadline.Two -> stringResource(
        R.string.story_headline_two,
        categoryDisplayName(headline.firstCategoryId, headline.firstName),
        headline.firstPercent,
        categoryDisplayName(headline.secondCategoryId, headline.secondName),
        headline.secondPercent,
    )
}

@Composable
private fun filterEmptyText(filter: TxFilter): String = stringResource(
    when (filter) {
        TxFilter.All -> R.string.stories_empty_filter_all
        TxFilter.Expense -> R.string.stories_empty_filter_expense
        TxFilter.Income -> R.string.stories_empty_filter_income
    },
)

@Composable
private fun storyTxName(tx: StoryTx): String =
    tx.categoryName?.let { categoryDisplayName(tx.categoryId, it) }
        ?: stringResource(R.string.category_fallback_other)

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
                    categoryDisplayName(bar.categoryId, bar.name),
                    style = WheregoType.chip,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(bar.amountLabel, style = WheregoType.barAmount, color = colors.ink)
            Text(bar.percentLabel, style = WheregoType.leftPill, color = colors.muted)
        }
        WheregoMeter(fraction = bar.fraction, fillColor = colors.teal)
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
            contentDescription = stringResource(R.string.stories_cd_filter),
            tint = colors.ink,
            modifier = Modifier.size(14.dp),
        )
        Text(stringResource(filter.labelRes), style = WheregoType.leftPill, color = colors.ink)
    }
}

/**
 * The month's running balance drawn by hand, normalised by `BalanceSeries.spark` so the shape
 * fills the box: fraction `0f` is the month's low, `1f` its high. A real balance sits far from
 * zero, so a zero-based axis would squeeze the whole month into a hairline along the top edge.
 * A flat month draws a mid-height rule instead of a line glued to an edge.
 */
@Composable
private fun BalanceSparkline(balance: StoryBalance, modifier: Modifier = Modifier) {
    val colors = WheregoTheme.colors
    Canvas(
        modifier
            .fillMaxWidth()
            .height(88.dp),
    ) {
        val strokeWidth = 2.5.dp.toPx()
        val dotRadius = 4.dp.toPx()
        val inset = maxOf(strokeWidth, dotRadius)
        val left = inset
        val right = size.width - inset
        val usableHeight = size.height - inset * 2f
        val count = balance.fractions.size
        val stepX = if (count > 1) (right - left) / (count - 1) else 0f
        fun yOf(fraction: Float): Float = inset + (1f - fraction) * usableHeight

        // The sign flip is the one gridline worth drawing: below it the pot is empty.
        balance.zeroFraction?.let { zero ->
            drawLine(
                color = colors.track,
                start = Offset(0f, yOf(zero)),
                end = Offset(size.width, yOf(zero)),
                strokeWidth = 1.5.dp.toPx(),
            )
        }

        val line = Path()
        val area = Path()
        balance.fractions.forEachIndexed { index, fraction ->
            val x = left + stepX * index
            val y = yOf(fraction)
            if (index == 0) {
                line.moveTo(x, y)
                area.moveTo(x, size.height)
                area.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                area.lineTo(x, y)
            }
        }
        area.lineTo(left + stepX * (count - 1), size.height)
        area.close()
        drawPath(area, colors.tealSoft)
        drawPath(
            line,
            colors.teal,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val here = Offset(left + stepX * (count - 1), yOf(balance.fractions.last()))
        drawCircle(colors.ink, radius = dotRadius, center = here)
        drawCircle(colors.teal, radius = dotRadius - 1.5.dp.toPx(), center = here)
    }
}
