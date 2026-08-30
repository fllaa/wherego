package com.flla.wherego.feature.plan

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.designsystem.component.WheregoBadge
import com.flla.wherego.core.designsystem.component.WheregoCapCard
import com.flla.wherego.core.designsystem.component.WheregoCard
import com.flla.wherego.core.designsystem.component.WheregoMeterCard
import com.flla.wherego.core.designsystem.component.WheregoMonthPill
import com.flla.wherego.core.designsystem.component.WheregoPageHeader
import com.flla.wherego.core.designsystem.component.WheregoSectionHeader
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.model.DigitBuffer
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.Recurrence
import com.flla.wherego.core.model.RecurringRule

@Composable
fun PlanRoute(
    viewModel: PlanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlanScreen(
        state = state,
        onAddBudget = viewModel::addBudget,
        onDeleteBudget = viewModel::deleteBudget,
        onAddRule = viewModel::addRule,
        onDeleteRule = viewModel::deleteRule,
        onAddGoal = viewModel::addGoal,
        onDeleteGoal = viewModel::deleteGoal,
    )
}

@Composable
fun PlanScreen(
    state: PlanUiState,
    onAddBudget: (String?, Long) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onAddRule: (Long, String, String, String, Int?) -> Unit,
    onDeleteRule: (String) -> Unit,
    onAddGoal: (String, Long, Long) -> Unit,
    onDeleteGoal: (String) -> Unit,
) {
    val colors = WheregoTheme.colors
    var budgetDialog by remember { mutableStateOf(false) }
    var ruleDialog by remember { mutableStateOf(false) }
    var goalDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WheregoPageHeader(
            title = "Plan",
            // The plan always describes the running month; the pill states which one.
            trailing = { WheregoMonthPill(label = state.monthLabel, onClick = {}) },
        )

        WheregoCapCard(
            label = "Spent in ${state.monthLabel}",
            amount = MoneyFormatter.format(state.monthSpentMinor, state.currency),
            fraction = state.capFraction,
            footLabel = capFootLabel(state),
            pillLabel = daysLeftLabel(state.daysLeft),
        )

        WheregoSectionHeader(
            title = "Budgets",
            trailing = if (editing) "Done" else "Edit",
            trailingColor = colors.teal,
            onTrailingClick = { editing = !editing },
        )
        if (state.budgets.isEmpty()) {
            Text(
                "No budgets yet · set a cap for this month.",
                style = WheregoType.meta,
                color = colors.muted,
            )
        } else {
            state.budgets.forEach { budget ->
                val strong = colors.teal
                WheregoMeterCard(
                    emoji = budget.emoji,
                    badgeFill = colors.tealSoft,
                    name = budget.name,
                    detail = budget.detail,
                    fraction = budget.fraction,
                    fillColor = if (budget.over) colors.coral else strong,
                ) {
                    if (editing) {
                        RemoveLink { onDeleteBudget(budget.id) }
                    } else {
                        Text(
                            budget.note,
                            style = WheregoType.link,
                            color = if (budget.over) colors.coral else strong,
                        )
                    }
                }
            }
        }
        AddRow("Set a budget for another category") { budgetDialog = true }

        WheregoSectionHeader(
            title = "Set aside",
            hint = "from the same pot",
            trailing = state.goalsTotalLabel,
        )
        if (state.goals.isEmpty()) {
            WheregoCard(cornerRadius = 22.dp, padding = 14.dp) {
                Text(
                    "Earmark a slice. Not another account.",
                    style = WheregoType.meta,
                    color = colors.muted,
                )
            }
        } else {
            state.goals.forEach { goal ->
                WheregoMeterCard(
                    emoji = goal.emoji,
                    badgeFill = colors.tealSoft,
                    name = goal.name,
                    detail = goal.detail,
                    fraction = goal.fraction,
                    fillColor = colors.teal,
                    badgeSize = 36.dp,
                    padding = 13.dp,
                ) {
                    if (editing) {
                        RemoveLink { onDeleteGoal(goal.id) }
                    } else {
                        Text(
                            goal.percentLabel,
                            style = WheregoType.leftPill,
                            color = colors.ink,
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(colors.tealSoft)
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
        AddRow("Set aside for something new") { goalDialog = true }

        // The design frame moved the recurring entry point to Me; the editing UI still lives here,
        // demoted below Set aside until it has another home.
        WheregoSectionHeader(title = "Recurring")
        if (state.rules.isEmpty()) {
            WheregoCard(cornerRadius = 22.dp, padding = 14.dp) {
                Text(
                    "Wifi, rent, pulsa — log when they hit.",
                    style = WheregoType.meta,
                    color = colors.muted,
                )
            }
        } else {
            state.rules.forEach { rule ->
                val category = state.categories.firstOrNull { it.id == rule.categoryId }
                RuleCard(
                    rule = rule,
                    emoji = category?.emoji ?: "🔁",
                    softHex = category?.softColorHex,
                    name = rule.note.ifBlank { category?.name ?: "Bill" },
                    editing = editing,
                    onDelete = { onDeleteRule(rule.id) },
                )
            }
        }
        AddRow("Add a recurring bill") { ruleDialog = true }
    }

    if (budgetDialog) {
        AmountCategoryDialog(
            title = "New budget",
            categories = listOf(null to "Overall") + state.categories.map { it.id to "${it.emoji} ${it.name}" },
            currency = state.currency,
            onDismiss = { budgetDialog = false },
            onConfirm = { catId, amount, _ ->
                onAddBudget(catId, amount)
                budgetDialog = false
            },
        )
    }
    if (ruleDialog) {
        AmountCategoryDialog(
            title = "New recurring",
            categories = state.categories.map { it.id to "${it.emoji} ${it.name}" },
            currency = state.currency,
            showNote = true,
            onDismiss = { ruleDialog = false },
            onConfirm = { catId, amount, note ->
                if (catId != null) {
                    onAddRule(amount, catId, note, Recurrence.MONTHLY, 1)
                }
                ruleDialog = false
            },
        )
    }
    if (goalDialog) {
        GoalDialog(
            currency = state.currency,
            onDismiss = { goalDialog = false },
            onConfirm = { name, amount, target ->
                onAddGoal(name, amount, target)
                goalDialog = false
            },
        )
    }
}

private fun capFootLabel(state: PlanUiState): String {
    val currency = state.currency
    val cap = MoneyFormatter.format(state.capTotalMinor, currency)
    return when {
        state.capTotalMinor <= 0L -> "No caps set for ${state.monthLabel} yet"
        state.capRemainingMinor >= 0L ->
            "${MoneyFormatter.format(state.capRemainingMinor, currency)} left of $cap"
        else -> "${MoneyFormatter.format(-state.capRemainingMinor, currency)} over $cap"
    }
}

private fun daysLeftLabel(daysLeft: Int): String = when {
    daysLeft <= 0 -> "Last day"
    daysLeft == 1 -> "1 day left"
    else -> "$daysLeft days left"
}

/** `Plan / Add Budget` — the outlined-only call to action that opens an editor. */
@Composable
private fun AddRow(label: String, onClick: () -> Unit) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .border(BorderStroke(2.dp, colors.muted), shape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            Icons.Outlined.Add,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(17.dp),
        )
        Text(label, style = WheregoType.link, color = colors.muted)
    }
}

@Composable
private fun RemoveLink(onClick: () -> Unit) {
    Text(
        "Remove",
        style = WheregoType.link,
        color = WheregoTheme.colors.coral,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun RuleCard(
    rule: RecurringRule,
    emoji: String,
    softHex: String?,
    name: String,
    editing: Boolean,
    onDelete: () -> Unit,
) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.sheet)
            .border(BorderStroke(2.5.dp, colors.ink), shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        WheregoBadge(
            fill = colors.tealSoft,
            size = 34.dp,
            cornerRadius = 17.dp,
        ) {
            Text(emoji, fontSize = 16.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(name, style = WheregoType.txTitle, color = colors.ink)
            Text(
                "${rule.freq.replaceFirstChar { it.uppercase() }} · next ${rule.nextOn}",
                style = WheregoType.meterDetail,
                color = colors.muted,
            )
        }
        if (editing) {
            RemoveLink(onDelete)
        } else {
            Text(
                MoneyFormatter.format(rule.amountMinor, rule.currency),
                style = WheregoType.txAmount,
                color = colors.ink,
            )
        }
    }
}

@Composable
private fun AmountCategoryDialog(
    title: String,
    categories: List<Pair<String?, String>>,
    currency: String,
    showNote: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String?, Long, String) -> Unit,
) {
    var digits by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(categories.firstOrNull()?.first) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(MoneyFormatter.format(DigitBuffer.amountMinor(digits), currency))
                OutlinedTextField(
                    value = digits,
                    onValueChange = { digits = it.filter { ch -> ch.isDigit() }.take(12) },
                    singleLine = true,
                    label = { Text("Amount") },
                )
                if (showNote) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(40) },
                        singleLine = true,
                        label = { Text("Note") },
                    )
                }
                categories.take(8).forEach { pair ->
                    val id = pair.first
                    val label = pair.second
                    val on = selected == id
                    Text(
                        text = label,
                        color = if (on) WheregoTheme.colors.tealDeep else WheregoTheme.colors.ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = id }
                            .padding(6.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected, DigitBuffer.amountMinor(digits), note) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun GoalDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var digits by remember { mutableStateOf("") }
    var targetDigits by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    singleLine = true,
                    label = { Text("Name") },
                )
                Text(MoneyFormatter.format(DigitBuffer.amountMinor(digits), currency))
                OutlinedTextField(
                    value = digits,
                    onValueChange = { digits = it.filter { ch -> ch.isDigit() }.take(12) },
                    singleLine = true,
                    label = { Text("Set aside now") },
                )
                Text(MoneyFormatter.format(DigitBuffer.amountMinor(targetDigits), currency))
                OutlinedTextField(
                    value = targetDigits,
                    onValueChange = { targetDigits = it.filter { ch -> ch.isDigit() }.take(12) },
                    singleLine = true,
                    label = { Text("Target (optional)") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                        DigitBuffer.amountMinor(digits),
                        DigitBuffer.amountMinor(targetDigits),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
