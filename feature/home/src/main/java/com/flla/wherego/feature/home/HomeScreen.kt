package com.flla.wherego.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.designsystem.component.GoMood
import com.flla.wherego.core.designsystem.component.WheregoGoAvatar
import com.flla.wherego.core.designsystem.component.WheregoHero
import com.flla.wherego.core.designsystem.component.WheregoStreakPill
import com.flla.wherego.core.designsystem.component.WheregoTxRow
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.i18n.R
import com.flla.wherego.core.i18n.categoryDisplayName
import com.flla.wherego.core.i18n.dayTitle
import com.flla.wherego.core.i18n.weekdayFull
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.TransactionKind
import com.flla.wherego.core.sync.CloudDot
import java.time.LocalDate

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenPlan: () -> Unit = {},
    onOpenStories: () -> Unit = {},
    onOpenCapture: (Transaction?) -> Unit = {},
    goMood: GoMood = GoMood.Idle,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clash by viewModel.balanceClash.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        goMood = goMood,
        onDelete = viewModel::delete,
        onUndo = viewModel::undoDelete,
        onDuplicate = viewModel::duplicateNow,
        onConfirmDue = viewModel::confirmDue,
        onOpenPlan = onOpenPlan,
        onOpenStories = onOpenStories,
        onOpenCapture = onOpenCapture,
    )
    clash?.let { BalanceClashDialog(it, viewModel::resolveBalanceClash) }
}

/**
 * Two devices each said what the pot totalled. The newer claim already anchors the balance, so
 * this is not "pick a number" — it is the one chance to overrule an arithmetic result that
 * silently changed a figure the user had been shown. Keeping one soft-deletes the other, and that
 * decision syncs, so every device lands on the same anchor.
 */
@Composable
private fun BalanceClashDialog(clash: BalanceClash, onResolve: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.home_clash_title)) },
        text = {
            Text(
                stringResource(
                    R.string.home_clash_body,
                    clash.mineLabel,
                    dayTitle(LocalDate.parse(clash.mineOn)),
                    clash.theirsLabel,
                    dayTitle(LocalDate.parse(clash.theirsOn)),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onResolve(clash.mineId) }) {
                Text(stringResource(R.string.home_clash_keep, clash.theirsLabel))
            }
        },
        dismissButton = {
            TextButton(onClick = { onResolve(clash.theirsId) }) {
                Text(stringResource(R.string.home_clash_keep, clash.mineLabel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    goMood: GoMood,
    onDelete: (String) -> Unit,
    onUndo: () -> Unit,
    onDuplicate: (String) -> Unit,
    onConfirmDue: (com.flla.wherego.core.database.DueItem) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenStories: () -> Unit,
    onOpenCapture: (Transaction?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    var mood by remember { mutableStateOf(goMood) }
    LaunchedEffect(goMood) { mood = goMood }
    LaunchedEffect(state.hasTxToday, goMood) {
        if (goMood != GoMood.Happy) {
            mood = if (state.hasTxToday) GoMood.Idle else GoMood.Sleepy
        }
    }

    val removedMessage = stringResource(R.string.home_snack_removed)
    val undoLabel = stringResource(R.string.home_snack_undo)
    LaunchedEffect(state.undoId) {
        if (state.undoId == null) {
            snackbarHostState.currentSnackbarData?.dismiss()
        } else {
            val result = snackbarHostState.showSnackbar(
                message = removedMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Indefinite,
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(colors.paper),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WheregoGoAvatar(mood = mood)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                R.string.home_greeting,
                                state.greetingName ?: stringResource(R.string.home_greeting_fallback),
                            ),
                            style = WheregoType.greeting,
                            color = colors.ink,
                        )
                        Text(
                            text = listOfNotNull(
                                state.weekday?.let { weekdayFull(it) },
                                pluralStringResource(
                                    R.plurals.home_sub_logged_week,
                                    state.weekLoggedCount,
                                    state.weekLoggedCount,
                                ),
                            ).joinToString(" · "),
                            style = WheregoType.meta,
                            color = colors.muted,
                        )
                    }
                    Box(
                        Modifier
                            .padding(end = 8.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (state.cloudDot) {
                                    CloudDot.Synced -> colors.teal
                                    CloudDot.Pending -> colors.coral
                                    CloudDot.Offline -> colors.muted
                                },
                            ),
                    )
                    WheregoStreakPill(days = state.streakDays)
                }
            }
            item {
                WheregoHero(
                    amountLabel = state.monthSpentLabel,
                    incomeLabel = state.monthIncomeMinor?.let {
                        stringResource(R.string.home_hero_income, MoneyFormatter.format(it, state.currency))
                    },
                    leftLabel = state.monthLeftMinor?.let {
                        stringResource(R.string.money_left, MoneyFormatter.format(it, state.currency))
                    },
                )
            }
            item {
                BudgetCard(bars = state.budgetBars, currency = state.currency, onPlan = onOpenPlan)
            }
            if (state.due.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.home_section_due), style = WheregoType.cardTitle, color = colors.ink)
                }
                items(state.due, key = { it.rule.id }) { item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                item.rule.note.ifBlank {
                                    categoryDisplayName(item.rule.categoryId, item.categoryName)
                                },
                                style = WheregoType.chip,
                                color = colors.ink,
                            )
                            Text(item.rule.nextOn, style = WheregoType.meta, color = colors.muted)
                        }
                        Text(
                            stringResource(R.string.home_cta_log_it),
                            color = colors.tealDeep,
                            style = WheregoType.link,
                            modifier = Modifier.clickable { onConfirmDue(item) },
                        )
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(stringResource(R.string.home_section_today), style = WheregoType.cardTitle, color = colors.ink)
                    Text(state.todayTotalLabel, style = WheregoType.streakNum, color = colors.muted)
                }
            }
            if (state.today.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.home_empty_today),
                        style = WheregoType.meta,
                        color = colors.muted,
                    )
                }
            } else {
                items(state.today, key = { it.id }) { row ->
                    TxItem(
                        row = row,
                        onClick = { onOpenCapture(row.transaction) },
                        onDelete = { onDelete(row.id) },
                        onDuplicate = { onDuplicate(row.id) },
                    )
                }
            }
            if (state.earlierThisWeek.isNotEmpty()) {
                val earlierDates = state.earlierThisWeek.map { it.transaction.occurredOn }.distinct()
                item {
                    val earlierTitle = if (earlierDates.size == 1) {
                        stringResource(R.string.home_section_yesterday)
                    } else {
                        stringResource(R.string.home_section_earlier_week)
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(earlierTitle, style = WheregoType.cardTitle, color = colors.ink)
                        Text(
                            stringResource(R.string.home_link_stories),
                            style = WheregoType.link,
                            color = colors.accentText,
                            modifier = Modifier.clickable(onClick = onOpenStories),
                        )
                    }
                }
                items(state.earlierThisWeek, key = { it.id }) { row ->
                    TxItem(
                        row = row,
                        onClick = { onOpenCapture(row.transaction) },
                        onDelete = { onDelete(row.id) },
                        onDuplicate = { onDuplicate(row.id) },
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TxItem(
    row: TxRowUi,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
) {
    val colors = WheregoTheme.colors
    val name = row.categoryName?.let { categoryDisplayName(row.categoryId, it) }
        ?: stringResource(R.string.category_fallback_other)
    // A reconcile row asserts a total. It carries no note, the capture sheet cannot represent it,
    // and duplicating it would manufacture a second same-day anchor out of nothing.
    val isReconcile = row.transaction.kind == TransactionKind.RECONCILE
    val title = if (isReconcile) stringResource(R.string.kind_reconcile) else row.note.ifBlank { name }
    val subtitle = row.time?.let { "$it · $name" } ?: name
    var menu by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    Box {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.coral)
                        .padding(end = 18.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(stringResource(R.string.home_swipe_delete), color = colors.onAlarm, style = WheregoType.meta)
                }
            },
            enableDismissFromStartToEnd = false,
        ) {
            WheregoTxRow(
                emoji = row.emoji,
                title = title,
                subtitle = subtitle,
                amountLabel = row.amountLabel,
                badgeSoftHex = row.badgeSoftHex,
                hasReceipt = row.hasReceipt,
                modifier = Modifier.combinedClickable(
                    enabled = !isReconcile,
                    onClick = onClick,
                    onLongClick = { menu = true },
                ),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_menu_same_again)) },
                onClick = {
                    menu = false
                    onDuplicate()
                },
            )
        }
    }
}

@Composable
private fun BudgetCard(
    bars: List<com.flla.wherego.core.model.BudgetBar>,
    currency: String,
    onPlan: () -> Unit,
) {
    val colors = WheregoTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.white)
            .border(2.5.dp, colors.outline, RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.home_section_budget_check), style = WheregoType.cardTitle, color = colors.ink)
            Text(
                stringResource(R.string.home_link_plan),
                style = WheregoType.link,
                color = colors.accentText,
                modifier = Modifier.clickable(onClick = onPlan),
            )
        }
        if (bars.isEmpty()) {
            Text(stringResource(R.string.home_empty_budget), style = WheregoType.meta, color = colors.muted)
        } else {
            bars.forEach { bar ->
                val fraction = if (bar.capMinor <= 0L) 0f else (bar.spentMinor.toFloat() / bar.capMinor).coerceIn(0f, 1f)
                val fill = if (bar.over) colors.coral else colors.teal
                val name = bar.categoryId?.let { categoryDisplayName(it, bar.name) }
                    ?: stringResource(R.string.plan_choice_overall)
                val label = if (bar.over) {
                    stringResource(
                        R.string.money_over,
                        MoneyFormatter.compact(-bar.remainingMinor, currency),
                    )
                } else {
                    stringResource(
                        R.string.money_left,
                        MoneyFormatter.compact(bar.remainingMinor, currency),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${bar.emoji}  $name", style = WheregoType.chip, color = colors.ink)
                        Text(label, style = WheregoType.meta, color = if (bar.over) colors.coral else colors.tealDeep)
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
                                .fillMaxWidth(if (bar.over) 1f else fraction)
                                .height(13.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(fill),
                        )
                    }
                }
            }
        }
    }
}
