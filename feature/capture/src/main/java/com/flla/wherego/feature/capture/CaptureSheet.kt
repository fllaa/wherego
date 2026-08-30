package com.flla.wherego.feature.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.designsystem.component.ParkItButton
import com.flla.wherego.core.designsystem.component.WheregoNumpad
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.designsystem.theme.parseHexColor
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.TransactionKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(
    editing: Transaction?,
    onDismiss: () -> Unit,
    onParked: (Transaction) -> Unit = {},
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var attachId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(editing?.id) {
        if (editing == null) viewModel.beginCreate() else viewModel.beginEdit(editing)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = WheregoTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.sheet,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        dragHandle = { Grabber() },
    ) {
        CaptureSheetBody(
            state = state,
            onKind = viewModel::onKind,
            onDigit = viewModel::onDigit,
            onBackspace = viewModel::onBackspace,
            onQuickAmount = viewModel::onQuickAmount,
            onCategory = viewModel::onCategory,
            onNote = viewModel::onNote,
            onToggleNote = viewModel::toggleNote,
            onToday = viewModel::onToday,
            onYesterday = viewModel::onYesterday,
            onPickRequested = viewModel::onPickRequested,
            onToggleMore = viewModel::onToggleMore,
            onAttach = { id -> attachId = id },
            onCycleCurrency = viewModel::cycleCurrency,
            onFxRate = viewModel::onFxRate,
            onSave = { viewModel.save { parked -> onParked(parked); onDismiss() } },
        )
    }
    if (state.showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(state.occurredOn)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = viewModel::onPickDismissed,
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = pickerState.selectedDateMillis ?: return@TextButton
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .toString()
                        viewModel.onDatePicked(picked)
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onPickDismissed) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
    val pendingAttach = attachId
    if (pendingAttach != null) {
        ReceiptAttachDialog(
            transactionId = pendingAttach,
            onFinished = { attachId = null },
        )
    }
}

@Composable
private fun CaptureSheetBody(
    state: CaptureUiState,
    onKind: (String) -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onQuickAmount: (Long) -> Unit,
    onCategory: (String) -> Unit,
    onNote: (String) -> Unit,
    onToggleNote: () -> Unit,
    onToday: () -> Unit,
    onYesterday: () -> Unit,
    onPickRequested: () -> Unit,
    onToggleMore: () -> Unit,
    onAttach: (String) -> Unit,
    onCycleCurrency: () -> Unit,
    onFxRate: (String) -> Unit,
    onSave: () -> Unit,
) {
    val colors = WheregoTheme.colors
    val today = state.occurredOn.isNotEmpty() &&
        state.occurredOn == java.time.LocalDate.now(state.zoneId).toString()
    val yesterday = state.occurredOn == java.time.LocalDate.now(state.zoneId).minusDays(1).toString()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KindToggle(kind = state.kind, onKind = onKind)
        AmountDisplay(state = state)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateChip("Today", selected = today, onClick = onToday)
            DateChip("Yesterday", selected = yesterday, onClick = onYesterday)
            DateChip("Pick", selected = !today && !yesterday && state.occurredOn.isNotEmpty(), onClick = onPickRequested)
            QuickChip("10rb", onClick = { onQuickAmount(10_000L) })
            QuickChip("15rb", onClick = { onQuickAmount(15_000L) })
            QuickChip("25rb", onClick = { onQuickAmount(25_000L) })
            QuickChip("note", selected = state.noteOpen, onClick = onToggleNote)
            QuickChip(state.currency, selected = state.currency != state.baseCurrency, onClick = onCycleCurrency)
            if (state.editingId != null) {
                QuickChip("photo", onClick = { onAttach(state.editingId) })
            }
        }
        if (state.currency != state.baseCurrency) {
            OutlinedTextField(
                value = state.fxRate,
                onValueChange = onFxRate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                singleLine = true,
                placeholder = { Text("rate to ${state.baseCurrency}", color = colors.muted) },
            )
        }
        if (state.noteOpen) {
            OutlinedTextField(
                value = state.note,
                onValueChange = onNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                singleLine = true,
                placeholder = { Text("note", color = colors.muted) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
            )
        }
        CategoryRow(
            chips = state.chipCategories,
            selectedId = state.categoryId,
            onCategory = onCategory,
            onMore = onToggleMore,
        )
        if (state.showAllCategories) {
            CategoryGrid(
                categories = state.matchingCategories,
                selectedId = state.categoryId,
                onCategory = onCategory,
            )
        }
        WheregoNumpad(onDigit = onDigit, onBackspace = onBackspace)
        ParkItButton(enabled = state.canSave, onClick = onSave)
    }
}

@Composable
private fun Grabber() {
    val colors = WheregoTheme.colors
    Box(
        Modifier
            .padding(top = 10.dp, bottom = 8.dp)
            .size(width = 42.dp, height = 3.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(colors.track),
    )
}

@Composable
private fun KindToggle(kind: String, onKind: (String) -> Unit) {
    val colors = WheregoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.chipIdle)
            .padding(4.dp),
    ) {
        listOf(TransactionKind.EXPENSE to "Expense", TransactionKind.INCOME to "Income").forEach { (value, label) ->
            val selected = kind == value
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) colors.teal else colors.sheet)
                    .then(
                        if (selected) Modifier.border(2.5.dp, colors.ink, RoundedCornerShape(14.dp))
                        else Modifier,
                    )
                    .clickable { onKind(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = WheregoType.kindTab,
                    color = if (selected) colors.white else colors.ink,
                )
            }
        }
    }
}

@Composable
private fun AmountDisplay(state: CaptureUiState) {
    val colors = WheregoTheme.colors
    val raw = state.amountLabel
    val prefix = if (raw.startsWith("Rp ")) "Rp" else raw.substringBefore(" ", missingDelimiterValue = "")
    val digits = if (raw.startsWith("Rp ")) raw.removePrefix("Rp ") else raw.substringAfter(" ", raw)
    Row(verticalAlignment = Alignment.Bottom) {
        if (prefix.isNotEmpty()) {
            Text(
                prefix,
                style = WheregoType.currencyPrefix,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 6.dp, end = 6.dp),
            )
        }
        Text(digits, style = WheregoType.amountHuge, color = colors.ink)
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier
                .padding(bottom = 6.dp)
                .width(3.dp)
                .height(44.dp)
                .background(colors.coral),
        )
    }
}

@Composable
private fun DateChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = WheregoTheme.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) colors.tealSoft else colors.chipIdle)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (label == "Today" || label == "Pick") {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (selected) colors.tealDeep else colors.muted,
            )
        }
        Text(
            label,
            style = WheregoType.chip.copy(fontSize = WheregoType.meta.fontSize),
            color = if (selected) colors.tealDeep else colors.ink,
        )
    }
}

@Composable
private fun QuickChip(label: String, selected: Boolean = false, onClick: () -> Unit) {
    val colors = WheregoTheme.colors
    Text(
        text = label,
        style = WheregoType.chip.copy(fontSize = WheregoType.meta.fontSize),
        color = if (selected) colors.tealDeep else colors.ink,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) colors.tealSoft else colors.chipIdle)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun CategoryRow(
    chips: List<Category>,
    selectedId: String?,
    onCategory: (String) -> Unit,
    onMore: () -> Unit,
) {
    val colors = WheregoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEach { cat ->
            CategoryChip(cat = cat, selected = cat.id == selectedId, onClick = { onCategory(cat.id) })
        }
        Box(
            Modifier
                .height(40.dp)
                .width(44.dp)
                .clip(RoundedCornerShape(99.dp))
                .border(2.dp, colors.track, RoundedCornerShape(99.dp))
                .clickable(onClick = onMore),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.MoreHoriz, contentDescription = "More categories", tint = colors.ink)
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selectedId: String?,
    onCategory: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cat ->
                    CategoryChip(
                        cat = cat,
                        selected = cat.id == selectedId,
                        onClick = { onCategory(cat.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    cat: Category,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = WheregoTheme.colors
    val fill = if (selected) parseHexColor(cat.colorHex) else parseHexColor(cat.softColorHex)
    val labelColor = if (selected) colors.white else colors.ink
    Row(
        Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(fill)
            .then(
                if (selected) Modifier.border(2.5.dp, colors.ink, RoundedCornerShape(99.dp))
                else Modifier.border(2.dp, fill, RoundedCornerShape(99.dp)),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(cat.emoji)
        Text(cat.name, style = WheregoType.chip, color = labelColor)
    }
}
