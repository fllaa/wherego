package app.wherego.feature.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wherego.core.designsystem.theme.WheregoTheme
import app.wherego.core.designsystem.theme.WheregoType
import app.wherego.core.model.Budget
import app.wherego.core.model.Category
import app.wherego.core.model.DigitBuffer
import app.wherego.core.model.MoneyFormatter
import app.wherego.core.model.Recurrence
import app.wherego.core.model.RecurringRule

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
    )
}

@Composable
fun PlanScreen(
    state: PlanUiState,
    onAddBudget: (String?, Long) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onAddRule: (Long, String, String, String, Int?) -> Unit,
    onDeleteRule: (String) -> Unit,
) {
    val colors = WheregoTheme.colors
    var budgetDialog by remember { mutableStateOf(false) }
    var ruleDialog by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Plan", style = WheregoType.cardTitle, color = colors.ink)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Budgets · ${state.yearMonth}", style = WheregoType.chip, color = colors.ink)
            Text("Add", color = colors.tealDeep, style = WheregoType.cta, modifier = Modifier.clickable { budgetDialog = true })
        }
        if (state.budgets.isEmpty()) {
            Text("No budgets yet · set a cap for this month.", style = WheregoType.meta, color = colors.muted)
        } else {
            state.budgets.forEach { budget ->
                BudgetRow(budget, state.categories, state.currency, onDelete = { onDeleteBudget(budget.id) })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recurring", style = WheregoType.chip, color = colors.ink)
            Text("Add", color = colors.tealDeep, style = WheregoType.cta, modifier = Modifier.clickable { ruleDialog = true })
        }
        if (state.rules.isEmpty()) {
            Text("Wifi, rent, pulsa — log when they hit.", style = WheregoType.meta, color = colors.muted)
        } else {
            state.rules.forEach { rule ->
                RuleRow(rule, state.categories, onDelete = { onDeleteRule(rule.id) })
            }
        }
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
}

@Composable
private fun BudgetRow(
    budget: Budget,
    categories: List<Category>,
    currency: String,
    onDelete: () -> Unit,
) {
    val colors = WheregoTheme.colors
    val name = budget.categoryId?.let { id -> categories.firstOrNull { it.id == id }?.name } ?: "Overall"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(name, style = WheregoType.chip, color = colors.ink)
        Text(MoneyFormatter.format(budget.amountMinor, currency), style = WheregoType.meta, color = colors.muted)
        Text("Remove", color = colors.coral, style = WheregoType.meta, modifier = Modifier.clickable(onClick = onDelete))
    }
}

@Composable
private fun RuleRow(
    rule: RecurringRule,
    categories: List<Category>,
    onDelete: () -> Unit,
) {
    val colors = WheregoTheme.colors
    val name = rule.note.ifBlank { categories.firstOrNull { it.id == rule.categoryId }?.name ?: "Bill" }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(name, style = WheregoType.chip, color = colors.ink)
            Text("${rule.freq} · next ${rule.nextOn}", style = WheregoType.meta, color = colors.muted)
        }
        Text("Remove", color = colors.coral, style = WheregoType.meta, modifier = Modifier.clickable(onClick = onDelete))
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
