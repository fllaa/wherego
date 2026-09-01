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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import com.flla.wherego.core.i18n.R
import com.flla.wherego.core.i18n.categoryDisplayName
import com.flla.wherego.core.i18n.dayTitle
import com.flla.wherego.core.i18n.monthLabel
import com.flla.wherego.core.model.DigitBuffer
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.Recurrence
import com.flla.wherego.core.model.RecurringRule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
    onAddRule: (amountMinor: Long, categoryId: String, note: String, freq: String, firstOn: LocalDate) -> Unit,
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
    val monthText = monthLabel(state.month, state.currentMonth)

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
            title = stringResource(R.string.plan_title),
            trailing = { WheregoMonthPill(label = monthText, onClick = { monthSheet = true }) },
        )

        WheregoCapCard(
            label = stringResource(R.string.plan_spent_in_month, monthText),
            amount = MoneyFormatter.format(state.monthSpentMinor, state.currency),
            fraction = state.capFraction,
            footLabel = capFootLabel(state),
            pillLabel = if (state.daysLeft < 0) null else daysLeftLabel(state.daysLeft),
        )

        WheregoSectionHeader(
            title = stringResource(R.string.plan_section_budgets),
            trailing = if (editing) stringResource(R.string.plan_done) else stringResource(R.string.plan_edit),
            trailingColor = colors.teal,
            onTrailingClick = { editing = !editing },
        )
        if (state.budgets.isEmpty()) {
            Text(
                stringResource(R.string.plan_empty_budgets),
                style = WheregoType.meta,
                color = colors.muted,
            )
        } else {
            state.budgets.forEach { budget ->
                val strong = colors.teal
                val displayName = budget.categoryId?.let { categoryDisplayName(it, budget.name) }
                    ?: stringResource(R.string.plan_choice_overall)
                val detail = stringResource(
                    R.string.plan_budget_detail,
                    MoneyFormatter.format(budget.spentMinor, state.currency),
                    MoneyFormatter.number(budget.capMinor, state.currency),
                )
                val note = if (budget.over) {
                    stringResource(
                        R.string.money_over,
                        MoneyFormatter.compact(budget.spentMinor - budget.capMinor, state.currency),
                    )
                } else {
                    stringResource(
                        R.string.money_left,
                        MoneyFormatter.compact(budget.capMinor - budget.spentMinor, state.currency),
                    )
                }
                WheregoMeterCard(
                    emoji = budget.emoji,
                    badgeFill = colors.tealSoft,
                    name = displayName,
                    detail = detail,
                    fraction = budget.fraction,
                    fillColor = if (budget.over) colors.coral else strong,
                ) {
                    if (editing) {
                        RemoveLink { onDeleteBudget(budget.id) }
                    } else {
                        Text(
                            note,
                            style = WheregoType.link,
                            color = if (budget.over) colors.coral else strong,
                        )
                    }
                }
            }
        }
        AddRow(stringResource(R.string.plan_cta_add_budget)) { budgetSheet = true }

        WheregoSectionHeader(
            title = stringResource(R.string.plan_section_set_aside),
            hint = stringResource(R.string.plan_hint_same_pot),
            trailing = state.goalsTotalLabel,
        )
        if (state.goals.isEmpty()) {
            WheregoCard(cornerRadius = 22.dp, padding = 14.dp) {
                Text(
                    stringResource(R.string.plan_empty_goals),
                    style = WheregoType.meta,
                    color = colors.muted,
                )
            }
        } else {
            state.goals.forEach { goal ->
                val detail = if (goal.targetMinor > 0L) {
                    stringResource(
                        R.string.plan_budget_detail,
                        MoneyFormatter.format(goal.allocatedMinor, state.currency),
                        MoneyFormatter.format(goal.targetMinor, state.currency),
                    )
                } else {
                    stringResource(
                        R.string.plan_goal_set_aside,
                        MoneyFormatter.format(goal.allocatedMinor, state.currency),
                    )
                }
                WheregoMeterCard(
                    emoji = goal.emoji,
                    badgeFill = colors.tealSoft,
                    name = goal.name,
                    detail = detail,
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
        AddRow(stringResource(R.string.plan_cta_add_goal)) { goalSheet = true }

        // The design frame moved the recurring entry point to Me; the editing UI still lives here,
        // demoted below Set aside until it has another home.
        WheregoSectionHeader(title = stringResource(R.string.plan_section_recurring))
        if (state.rules.isEmpty()) {
            WheregoCard(cornerRadius = 22.dp, padding = 14.dp) {
                Text(
                    stringResource(R.string.plan_empty_recurring),
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
                    name = rule.note.ifBlank {
                        category?.let { categoryDisplayName(it.id, it.name) }
                            ?: stringResource(R.string.recurring_fallback_label)
                    },
                    editing = editing,
                    onDelete = { onDeleteRule(rule.id) },
                )
            }
        }
        AddRow(stringResource(R.string.plan_cta_add_recurring)) { ruleSheet = true }
    }

    if (budgetSheet) {
        AmountCategorySheet(
            title = stringResource(R.string.plan_sheet_set_budget),
            categories = listOf(null to stringResource(R.string.plan_choice_overall)) +
                state.categories.map { it.id to "${it.emoji} ${categoryDisplayName(it.id, it.name)}" },
            currency = state.currency,
            confirmLabel = stringResource(R.string.plan_cta_set_it),
            onDismiss = { budgetSheet = false },
            onConfirm = { catId, amount ->
                onAddBudget(catId, amount)
                budgetSheet = false
            },
        )
    }
    if (ruleSheet) {
        BillSheet(
            categories = state.categories.map { it.id to "${it.emoji} ${categoryDisplayName(it.id, it.name)}" },
            currency = state.currency,
            today = state.today,
            onDismiss = { ruleSheet = false },
            onConfirm = { catId, amount, note, firstOn ->
                onAddRule(amount, catId, note, Recurrence.MONTHLY, firstOn)
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
        WheregoBottomSheet(title = stringResource(R.string.plan_sheet_month), onDismiss = { monthSheet = false }) {
            state.monthChoices.forEach { choice ->
                MonthChoiceRow(
                    label = monthLabel(choice.yearMonth, state.currentMonth),
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

@Composable
private fun capFootLabel(state: PlanUiState): String {
    val currency = state.currency
    val cap = MoneyFormatter.format(state.capTotalMinor, currency)
    return when {
        state.capTotalMinor <= 0L -> stringResource(
            R.string.plan_cap_no_caps,
            monthLabel(state.month, state.currentMonth),
        )
        state.capRemainingMinor >= 0L -> stringResource(
            R.string.plan_cap_left_of,
            MoneyFormatter.format(state.capRemainingMinor, currency),
            cap,
        )
        else -> stringResource(
            R.string.plan_cap_over_of,
            MoneyFormatter.format(-state.capRemainingMinor, currency),
            cap,
        )
    }
}

@Composable
private fun daysLeftLabel(daysLeft: Int): String =
    if (daysLeft <= 0) {
        stringResource(R.string.plan_days_last_day)
    } else {
        pluralStringResource(R.plurals.plan_days_left, daysLeft, daysLeft)
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
        stringResource(R.string.plan_cta_remove),
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
                stringResource(
                    R.string.plan_recurring_detail,
                    freqLabel(rule.freq),
                    dayTitle(LocalDate.parse(rule.nextOn)),
                ),
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
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: String?, amountMinor: Long) -> Unit,
) {
    var digits by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(categories.firstOrNull()?.first) }
    val amount = DigitBuffer.amountMinor(digits)
    WheregoBottomSheet(title = title, onDismiss = onDismiss) {
        AmountReadout(amountMinor = amount, currency = currency)
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
            onClick = { onConfirm(selected, amount) },
            enabled = amount > 0L,
            icon = Icons.Outlined.Check,
        )
    }
}

/**
 * `Plan / Add a bill`. The first due date belongs to the user, not the clock: it seeds
 * `startOn`/`nextOn` and, for a monthly rule, the day of month every later hit lands on.
 */
@Composable
private fun BillSheet(
    categories: List<Pair<String?, String>>,
    currency: String,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: String, amountMinor: Long, note: String, firstOn: LocalDate) -> Unit,
) {
    val colors = WheregoTheme.colors
    var digits by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(categories.firstOrNull()?.first) }
    var firstOn by remember { mutableStateOf(today) }
    var picking by remember { mutableStateOf(false) }
    val amount = DigitBuffer.amountMinor(digits)
    WheregoBottomSheet(title = stringResource(R.string.plan_sheet_add_bill), onDismiss = onDismiss) {
        AmountReadout(amountMinor = amount, currency = currency)
        SheetField(
            label = stringResource(R.string.plan_field_note),
            value = note,
            onValueChange = { note = it.take(40) },
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.plan_field_first_due),
                style = WheregoType.settingLabel,
                color = colors.ink,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val isToday = firstOn == today
                ChipPill(
                    label = stringResource(R.string.plan_chip_today),
                    selected = isToday,
                    onClick = { firstOn = today },
                )
                ChipPill(
                    label = if (isToday) stringResource(R.string.plan_chip_pick_date) else dayTitle(firstOn),
                    selected = !isToday,
                    onClick = { picking = true },
                )
            }
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
            label = stringResource(R.string.plan_cta_add_it),
            onClick = { selected?.let { onConfirm(it, amount, note, firstOn) } },
            enabled = amount > 0L && selected != null,
            icon = Icons.Outlined.Check,
        )
    }
    if (picking) {
        FirstDuePicker(
            selected = firstOn,
            earliest = today,
            onDismiss = { picking = false },
            onPicked = {
                firstOn = it
                picking = false
            },
        )
    }
}

/**
 * A bill that first falls due before today would be overdue on creation — and would fire its
 * reminder the moment it was saved — so the calendar starts at [earliest].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FirstDuePicker(
    selected: LocalDate,
    earliest: LocalDate,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit,
) {
    val floor = earliest.utcMillis()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = selected.utcMillis(),
        yearRange = earliest.year..earliest.year + 5,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= floor
            override fun isSelectableYear(year: Int): Boolean = year >= earliest.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis ?: return@TextButton
                    onPicked(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                },
            ) { Text(stringResource(R.string.dialog_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

/** `DatePicker` speaks UTC midnight, whatever zone the user lives in. */
private fun LocalDate.utcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

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
    WheregoBottomSheet(title = stringResource(R.string.plan_sheet_set_aside), onDismiss = onDismiss) {
        SheetField(label = stringResource(R.string.plan_field_name), value = name, onValueChange = { name = it.take(40) })
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
            label = stringResource(R.string.plan_cta_set_it),
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
        listOf(
            GoalAmount.NOW to stringResource(R.string.plan_tab_now),
            GoalAmount.TARGET to stringResource(R.string.plan_tab_target),
        ).forEach { (value, label) ->
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
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.forEach { (id, label) ->
            ChipPill(label = label, selected = selected == id, onClick = { onSelect(id) })
        }
    }
}

/** The sheet pill: teal-soft when idle, solid teal under an ink hairline when picked. */
@Composable
private fun ChipPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = WheregoTheme.colors
    val pill = RoundedCornerShape(99.dp)
    Text(
        label,
        style = WheregoType.chip,
        color = if (selected) colors.white else colors.ink,
        modifier = Modifier
            .clip(pill)
            .background(if (selected) colors.teal else colors.tealSoft)
            .border(
                BorderStroke(if (selected) 2.5.dp else 2.dp, if (selected) colors.ink else colors.tealSoft),
                pill,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun freqLabel(freq: String): String =
    stringResource(
        if (freq == Recurrence.WEEKLY) R.string.freq_weekly else R.string.freq_monthly,
    )
