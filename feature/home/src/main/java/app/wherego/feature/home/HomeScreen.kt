package app.wherego.feature.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wherego.core.designsystem.component.GoMood
import app.wherego.core.designsystem.component.WheregoGoAvatar
import app.wherego.core.designsystem.component.WheregoHero
import app.wherego.core.designsystem.component.WheregoStreakPill
import app.wherego.core.designsystem.component.WheregoTxRow
import app.wherego.core.designsystem.theme.WheregoTheme
import app.wherego.core.designsystem.theme.WheregoType
import app.wherego.feature.capture.CaptureSheet
import app.wherego.core.model.Transaction
import app.wherego.core.sync.CloudDot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenPlan: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onDelete = viewModel::delete,
        onUndo = viewModel::undoDelete,
        onDuplicate = viewModel::duplicateNow,
        onConfirmDue = viewModel::confirmDue,
        onOpenPlan = onOpenPlan,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onDelete: (String) -> Unit,
    onUndo: () -> Unit,
    onDuplicate: (String) -> Unit,
    onConfirmDue: (app.wherego.core.database.DueItem) -> Unit,
    onOpenPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var captureOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var goMood by remember { mutableStateOf(GoMood.Idle) }
    LaunchedEffect(state.hasTxToday) {
        if (goMood != GoMood.Happy) {
            goMood = if (state.hasTxToday) GoMood.Idle else GoMood.Sleepy
        }
    }

    LaunchedEffect(state.undoId) {
        if (state.undoId == null) {
            snackbarHostState.currentSnackbarData?.dismiss()
        } else {
            val result = snackbarHostState.showSnackbar(
                message = "Removed",
                actionLabel = "Undo",
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
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WheregoGoAvatar(mood = goMood)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Hey ${state.greetingName} 👋",
                        style = WheregoType.greeting,
                        color = colors.ink,
                        modifier = Modifier.weight(1f),
                    )
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
                WheregoHero(amountLabel = state.monthSpentLabel)
            }
            if (state.budgetBars.isNotEmpty()) {
                item {
                    BudgetCard(bars = state.budgetBars, currency = state.currency, onPlan = onOpenPlan)
                }
            }
            if (state.due.isNotEmpty()) {
                item {
                    Text("Due", style = WheregoType.cardTitle, color = colors.ink)
                }
                items(state.due, key = { it.rule.id }) { item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                item.rule.note.ifBlank { item.categoryName },
                                style = WheregoType.chip,
                                color = colors.ink,
                            )
                            Text(item.rule.nextOn, style = WheregoType.meta, color = colors.muted)
                        }
                        Text(
                            "Log it",
                            color = colors.tealDeep,
                            style = WheregoType.cta,
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
                    Text("Today", style = WheregoType.cardTitle, color = colors.ink)
                    Text(state.todayTotalLabel, style = WheregoType.meta, color = colors.muted)
                }
            }
            if (state.today.isEmpty()) {
                item {
                    Text(
                        "First one is the hardest. Tap +",
                        style = WheregoType.meta,
                        color = colors.muted,
                    )
                }
            } else {
                items(state.today, key = { it.id }) { row ->
                    TxItem(
                        row = row,
                        onClick = {
                            editing = row.transaction
                            captureOpen = true
                        },
                        onDelete = { onDelete(row.id) },
                        onDuplicate = { onDuplicate(row.id) },
                    )
                }
            }
            if (state.earlierThisWeek.isNotEmpty()) {
                item {
                    Text("Earlier this week", style = WheregoType.cardTitle, color = colors.ink)
                }
                items(state.earlierThisWeek, key = { it.id }) { row ->
                    TxItem(
                        row = row,
                        onClick = {
                            editing = row.transaction
                            captureOpen = true
                        },
                        onDelete = { onDelete(row.id) },
                        onDuplicate = { onDuplicate(row.id) },
                    )
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 12.dp)
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.teal)
                .border(2.5.dp, colors.ink, RoundedCornerShape(20.dp))
                .clickable {
                    editing = null
                    captureOpen = true
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = "Add",
                tint = colors.white,
                modifier = Modifier.size(28.dp),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 84.dp),
        )
    }

    if (captureOpen) {
        CaptureSheet(
            editing = editing,
            onDismiss = {
                captureOpen = false
                editing = null
            },
            onParked = {
                goMood = GoMood.Happy
                scope.launch {
                    delay(800)
                    goMood = GoMood.Idle
                }
            },
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
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.coral)
                        .padding(end = 18.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text("Delete", color = colors.white, style = WheregoType.meta)
                }
            },
            enableDismissFromStartToEnd = false,
        ) {
            WheregoTxRow(
                emoji = row.emoji,
                title = row.title,
                subtitle = row.subtitle,
                amountLabel = row.amountLabel,
                badgeSoftHex = row.badgeSoftHex,
                modifier = Modifier.combinedClickable(
                    onClick = onClick,
                    onLongClick = { menu = true },
                ),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Same again") },
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
    bars: List<app.wherego.core.model.BudgetBar>,
    currency: String,
    onPlan: () -> Unit,
) {
    val colors = WheregoTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.white)
            .border(2.5.dp, colors.ink, RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Budget check", style = WheregoType.cardTitle, color = colors.ink)
            Text("Plan →", style = WheregoType.meta, color = colors.teal, modifier = Modifier.clickable(onClick = onPlan))
        }
        bars.forEach { bar ->
            val fraction = if (bar.capMinor <= 0L) 0f else (bar.spentMinor.toFloat() / bar.capMinor).coerceIn(0f, 1f)
            val fill = if (bar.over) colors.coral else app.wherego.core.designsystem.theme.parseHexColor(bar.colorHex)
            val label = if (bar.over) {
                "${app.wherego.core.model.MoneyFormatter.compact(-bar.remainingMinor, currency)} over"
            } else {
                "${app.wherego.core.model.MoneyFormatter.compact(bar.remainingMinor, currency)} left"
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${bar.emoji}  ${bar.name}", style = WheregoType.chip, color = colors.ink)
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

