package com.flla.wherego.feature.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.designsystem.component.WheregoBadge
import com.flla.wherego.core.designsystem.component.WheregoBottomSheet
import com.flla.wherego.core.designsystem.component.WheregoCapCard
import com.flla.wherego.core.designsystem.component.WheregoCard
import com.flla.wherego.core.designsystem.component.WheregoMeterCard
import com.flla.wherego.core.designsystem.component.WheregoMonthPill
import com.flla.wherego.core.designsystem.component.WheregoNumpad
import com.flla.wherego.core.designsystem.component.WheregoPageHeader
import com.flla.wherego.core.designsystem.component.WheregoPrimaryButton
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
        onSelectMonth = viewModel::selectMonth,
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
    onSelectMonth: (String) -> Unit,
    onAddBudget: (String?, Long) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onAddRule: (Long, String, String, String, Int?) -> Unit,
    onDeleteRule: (String) -> Unit,
    onAddGoal: (String, Long, Long) -> Unit,
    onDeleteGoal: (String) -> Unit,
) {
    val colors = WheregoTheme.colors
    var budgetSheet by remember { mutableStateOf(false) }
    var ruleSheet by remember { mutableStateOf(false) }
    var goalSheet by remember { mutableStateOf(false) }
    var monthSheet by remember { mutableStateOf(false) }
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
            trailing = { WheregoMonthPill(label = state.monthLabel, onClick = { monthSheet = true }) },
        )

        WheregoCapCard(
            label = "Spent in ${state.monthLabel}",
            amount = MoneyFormatter.format(state.monthSpentMinor, state.currency),
            fraction = state.capFraction,
            footLabel = capFootLabel(state),
            pillLabel = if (state.daysLeft < 0) null else daysLeftLabel(state.daysLeft),
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
        AddRow("Set a budget for another category") { budgetSheet = true }

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
        AddRow("Set aside for something new") { goalSheet = true }

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
        AddRow("Add a recurring bill") { ruleSheet = true }
    }

    if (budgetSheet) {
        AmountCategorySheet(
            title = "Set a budget",
            categories = listOf(null to "Overall") + state.categories.map { it.id to "${it.emoji} ${it.name}" },
            currency = state.currency,
            confirmLabel = "Set it",
            onDismiss = { budgetSheet = false },
            onConfirm = { catId, amount, _ ->
                onAddBudget(catId, amount)
                budgetSheet = false
            },
        )
    }
    if (ruleSheet) {
        AmountCategorySheet(
            title = "Add a bill",
            categories = state.categories.map { it.id to "${it.emoji} ${it.name}" },
            currency = state.currency,
            showNote = true,
            confirmLabel = "Add it",
            onDismiss = { ruleSheet = false },
            onConfirm = { catId, amount, note ->
                if (catId != null) {
                    onAddRule(amount, catId, note, Recurrence.MONTHLY, 1)
                }
                ruleSheet = false
            },
        )
    }
    if (goalSheet) {
        GoalSheet(
            currency = state.currency,
            onDismiss = { goalSheet = false },
            onConfirm = { name, amount, target ->
                onAddGoal(name, amount, target)
                goalSheet = false
            },
        )
    }
    if (monthSheet) {
        WheregoBottomSheet(title = "Month", onDismiss = { monthSheet = false }) {
            state.monthChoices.forEach { choice ->
                MonthChoiceRow(
                    label = choice.label,
                    selected = choice.id == state.monthId,
                    onClick = {
                        onSelectMonth(choice.id)
                        monthSheet = false
                    },
                )
            }
        }
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

@Composable
private fun MonthChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.tealSoft else colors.chipIdle)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = WheregoType.settingLabel, color = colors.ink)
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.tealDeep,
                modifier = Modifier.size(18.dp),
            )
        }
    }
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
private fun AmountCategorySheet(
    title: String,
    categories: List<Pair<String?, String>>,
    currency: String,
    showNote: Boolean = false,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String?, Long, String) -> Unit,
) {
    var digits by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(categories.firstOrNull()?.first) }
    val amount = DigitBuffer.amountMinor(digits)
    WheregoBottomSheet(title = title, onDismiss = onDismiss) {
        AmountReadout(amountMinor = amount, currency = currency)
        if (showNote) {
            SheetField(label = "Note", value = note, onValueChange = { note = it.take(40) })
        }
        CategoryChipRow(
            categories = categories,
            selected = selected,
            onSelect = { selected = it },
        )
        WheregoNumpad(
            onDigit = { digits = DigitBuffer.append(digits, it) },
            onBackspace = { digits = DigitBuffer.backspace(digits) },
        )
        WheregoPrimaryButton(
            label = confirmLabel,
            onClick = { onConfirm(selected, amount, note) },
            enabled = amount > 0L && (!showNote || selected != null),
            icon = Icons.Outlined.Check,
        )
    }
}

@Composable
private fun GoalSheet(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var nowDigits by remember { mutableStateOf("") }
    var targetDigits by remember { mutableStateOf("") }
    var field by remember { mutableStateOf(GoalAmount.NOW) }
    val now = DigitBuffer.amountMinor(nowDigits)
    val target = DigitBuffer.amountMinor(targetDigits)
    WheregoBottomSheet(title = "Set aside", onDismiss = onDismiss) {
        SheetField(label = "Name", value = name, onValueChange = { name = it.take(40) })
        GoalAmountToggle(selected = field, onSelect = { field = it })
        AmountReadout(
            amountMinor = if (field == GoalAmount.NOW) now else target,
            currency = currency,
        )
        WheregoNumpad(
            onDigit = { chunk ->
                if (field == GoalAmount.NOW) {
                    nowDigits = DigitBuffer.append(nowDigits, chunk)
                } else {
                    targetDigits = DigitBuffer.append(targetDigits, chunk)
                }
            },
            onBackspace = {
                if (field == GoalAmount.NOW) {
                    nowDigits = DigitBuffer.backspace(nowDigits)
                } else {
                    targetDigits = DigitBuffer.backspace(targetDigits)
                }
            },
        )
        WheregoPrimaryButton(
            label = "Set it",
            onClick = { onConfirm(name.trim(), now, target) },
            enabled = name.isNotBlank(),
            icon = Icons.Outlined.Check,
        )
    }
}

private enum class GoalAmount { NOW, TARGET }

@Composable
private fun GoalAmountToggle(selected: GoalAmount, onSelect: (GoalAmount) -> Unit) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.chipIdle)
            .padding(4.dp),
    ) {
        listOf(GoalAmount.NOW to "Now", GoalAmount.TARGET to "Target").forEach { (value, label) ->
            val on = selected == value
            val tab = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .weight(1f)
                    .clip(tab)
                    .background(if (on) colors.teal else colors.sheet)
                    .then(if (on) Modifier.border(BorderStroke(2.5.dp, colors.ink), tab) else Modifier)
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = WheregoType.kindTab,
                    color = if (on) colors.white else colors.ink,
                )
            }
        }
    }
}

@Composable
private fun AmountReadout(amountMinor: Long, currency: String) {
    Text(
        MoneyFormatter.format(amountMinor, currency),
        style = WheregoType.heroAmount.copy(fontSize = 34.sp, lineHeight = 42.sp),
        color = WheregoTheme.colors.ink,
    )
}

@Composable
private fun SheetField(label: String, value: String, onValueChange: (String) -> Unit) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = WheregoType.settingLabel, color = colors.ink)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = WheregoType.settingLabel.copy(color = colors.ink),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.chipIdle)
                .border(BorderStroke(2.dp, colors.track), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun CategoryChipRow(
    categories: List<Pair<String?, String>>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    val colors = WheregoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.forEach { (id, label) ->
            val on = selected == id
            val pill = RoundedCornerShape(99.dp)
            Text(
                label,
                style = WheregoType.chip,
                color = if (on) colors.white else colors.ink,
                modifier = Modifier
                    .clip(pill)
                    .background(if (on) colors.teal else colors.tealSoft)
                    .border(
                        BorderStroke(if (on) 2.5.dp else 2.dp, if (on) colors.ink else colors.tealSoft),
                        pill,
                    )
                    .clickable { onSelect(id) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
