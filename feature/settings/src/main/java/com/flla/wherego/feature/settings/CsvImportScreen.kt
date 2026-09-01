package com.flla.wherego.feature.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flla.wherego.core.designsystem.component.WheregoCard
import com.flla.wherego.core.designsystem.component.WheregoOnboardTopBar
import com.flla.wherego.core.designsystem.component.WheregoPrimaryButton
import com.flla.wherego.core.designsystem.component.WheregoTxRow
import com.flla.wherego.core.designsystem.component.wheregoHardShadow
import com.flla.wherego.core.designsystem.component.WheregoWaypointMark
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.i18n.R
import com.flla.wherego.core.model.CsvImport
import com.flla.wherego.core.model.CsvMapping
import com.flla.wherego.core.model.CsvRow
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.TransactionKind
import kotlinx.coroutines.launch

private const val STEP_COUNT = 3
private val ExpectedColumns = listOf("date", "kind", "amount", "currency", "category", "note")
private val HeaderNames = setOf("date", "occurredon", "kind", "type", "amount", "amountminor", "currency", "category", "note")
private val Pill = RoundedCornerShape(99.dp)

@Composable
fun CsvImportScreen(
    onBack: () -> Unit,
    onCommit: suspend (String, CsvMapping, Boolean) -> Int,
) {
    val colors = WheregoTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }
    var raw by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var mapping by remember { mutableStateOf(CsvMapping()) }
    var skipHeader by remember { mutableStateOf(true) }
    var pickError by remember { mutableStateOf<Int?>(null) }
    var commitError by remember { mutableStateOf<Int?>(null) }
    var committing by remember { mutableStateOf(false) }
    var parkedCount by remember { mutableStateOf<Int?>(null) }
    val parked = parkedCount
    val parsed = remember(raw) { if (raw.isBlank()) emptyList() else CsvImport.parse(raw) }
    val readyRows = remember(parsed, mapping, skipHeader) {
        if (parsed.isEmpty()) emptyList() else CsvImport.apply(parsed, mapping, skipHeader)
    }
    val preview = remember(readyRows) { readyRows.take(5) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (text.isNullOrBlank()) {
            pickError = R.string.csv_err_read_file
            return@rememberLauncherForActivityResult
        }
        val rows = CsvImport.parse(text)
        if (rows.isEmpty()) {
            pickError = R.string.csv_err_no_rows
            return@rememberLauncherForActivityResult
        }
        raw = text
        fileName = csvDisplayName(context, uri)
        mapping = CsvImport.guessMapping(rows.first())
        skipHeader = looksLikeHeader(rows.first())
        pickError = null
        commitError = null
        parkedCount = null
        step = 1
    }

    fun resetFile() {
        raw = ""
        fileName = ""
        mapping = CsvMapping()
        skipHeader = true
        pickError = null
        commitError = null
        parkedCount = null
        step = 0
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .systemBarsPadding(),
    ) {
        WheregoOnboardTopBar(
            stepIndex = if (parked != null) STEP_COUNT - 1 else step,
            stepCount = STEP_COUNT,
            onBack = {
                when {
                    parked != null || step == 0 -> onBack()
                    else -> step--
                }
            },
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = 18.dp, start = 26.dp, end = 26.dp, bottom = 24.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                when {
                    parked != null -> SuccessStep(count = parked)
                    step == 0 -> PickStep(fileName = fileName, errorRes = pickError)
                    step == 1 -> MapStep(
                        fileName = fileName,
                        parsed = parsed,
                        mapping = mapping,
                        skipHeader = skipHeader,
                        readyCount = readyRows.size,
                        onSkipHeader = { skipHeader = it },
                        onMapping = { mapping = it },
                    )
                    else -> PreviewStep(
                        preview = preview,
                        total = readyRows.size,
                        errorRes = commitError,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    parked != null -> {
                        WheregoPrimaryButton(stringResource(R.string.csv_back_to_me), onClick = onBack)
                    }
                    step == 0 -> {
                        if (parsed.isEmpty()) {
                            WheregoPrimaryButton(
                                label = stringResource(R.string.csv_cta_pick_file),
                                onClick = { pick.launch("*/*") },
                                icon = Icons.Outlined.Upload,
                            )
                        } else {
                            WheregoPrimaryButton(
                                stringResource(R.string.onb_cta_continue),
                                onClick = { step = 1 },
                            )
                            SubtleAction(stringResource(R.string.csv_cta_pick_different)) { resetFile() }
                        }
                    }
                    step == 1 -> {
                        WheregoPrimaryButton(
                            label = stringResource(R.string.onb_cta_continue),
                            onClick = { step = 2 },
                            enabled = readyRows.isNotEmpty(),
                        )
                        SubtleAction(stringResource(R.string.csv_cta_pick_different)) { resetFile() }
                    }
                    else -> {
                        WheregoPrimaryButton(
                            label = if (committing) {
                                stringResource(R.string.csv_cta_parking)
                            } else {
                                stringResource(R.string.csv_cta_park_them)
                            },
                            onClick = {
                                committing = true
                                commitError = null
                                scope.launch {
                                    val result = runCatching { onCommit(raw, mapping, skipHeader) }
                                    committing = false
                                    result.fold(
                                        onSuccess = { n ->
                                            if (n == 0) {
                                                commitError = R.string.csv_err_commit_format
                                            } else {
                                                parkedCount = n
                                            }
                                        },
                                        onFailure = {
                                            commitError = R.string.csv_err_commit_retry
                                        },
                                    )
                                }
                            },
                            enabled = !committing && readyRows.isNotEmpty(),
                            icon = Icons.Outlined.Check,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickStep(fileName: String, @StringRes errorRes: Int?) {
    val colors = WheregoTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(stringResource(R.string.csv_title_pick), style = WheregoType.onboardTitle, color = colors.ink)
        Text(
            stringResource(R.string.csv_sub_pick),
            style = WheregoType.onboardSub,
            color = colors.muted,
        )
        WheregoCard(
            cornerRadius = 30.dp,
            gap = 14.dp,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .wheregoHardShadow(shape = CircleShape, color = colors.shadow, offsetY = 5.dp)
                    .clip(CircleShape)
                    .background(colors.teal)
                    .border(BorderStroke(2.5.dp, colors.outlineStrong), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Upload,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                fileName.ifBlank { stringResource(R.string.csv_empty_no_file) },
                style = WheregoType.buttonLabel,
                color = colors.ink,
            )
            Text(
                stringResource(R.string.csv_helper_export),
                style = WheregoType.link,
                color = colors.muted,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3,
            ) {
                ExpectedColumns.forEach { name ->
                    Text(
                        name,
                        style = WheregoType.helper,
                        color = colors.ink,
                        modifier = Modifier
                            .clip(Pill)
                            .background(colors.chipIdle)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
        if (errorRes != null) {
            Text(stringResource(errorRes), style = WheregoType.meta, color = colors.coral)
        }
    }
}

@Composable
private fun MapStep(
    fileName: String,
    parsed: List<List<String>>,
    mapping: CsvMapping,
    skipHeader: Boolean,
    readyCount: Int,
    onSkipHeader: (Boolean) -> Unit,
    onMapping: (CsvMapping) -> Unit,
) {
    val colors = WheregoTheme.colors
    val columnCount = parsed.maxOfOrNull { it.size } ?: 0
    val headers = parsed.firstOrNull().orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.csv_title_map), style = WheregoType.onboardTitle, color = colors.ink)
        Text(
            stringResource(R.string.csv_sub_map),
            style = WheregoType.onboardSub,
            color = colors.muted,
        )
        WheregoCard(cornerRadius = 22.dp, padding = 14.dp, gap = 8.dp) {
            Text(
                fileName.ifBlank { stringResource(R.string.csv_fallback_title) },
                style = WheregoType.txTitle,
                color = colors.ink,
            )
            Text(
                pluralStringResource(R.plurals.csv_rows_ready, readyCount, readyCount),
                style = WheregoType.helper,
                color = colors.muted,
            )
        }
        HeaderToggle(selected = skipHeader, onClick = { onSkipHeader(!skipHeader) })
        MapField(stringResource(R.string.csv_col_date), mapping.date, columnCount, headers, skipHeader) {
            onMapping(mapping.copy(date = it))
        }
        MapField(stringResource(R.string.csv_col_kind), mapping.kind, columnCount, headers, skipHeader) {
            onMapping(mapping.copy(kind = it))
        }
        MapField(stringResource(R.string.csv_col_amount), mapping.amount, columnCount, headers, skipHeader) {
            onMapping(mapping.copy(amount = it))
        }
        MapField(stringResource(R.string.csv_col_currency), mapping.currency, columnCount, headers, skipHeader) {
            onMapping(mapping.copy(currency = it))
        }
        MapField(stringResource(R.string.csv_col_category), mapping.category, columnCount, headers, skipHeader) {
            onMapping(mapping.copy(category = it))
        }
        MapField(stringResource(R.string.csv_col_note), mapping.note, columnCount, headers, skipHeader) {
            onMapping(mapping.copy(note = it))
        }
    }
}

@Composable
private fun PreviewStep(preview: List<CsvRow>, total: Int, @StringRes errorRes: Int?) {
    val colors = WheregoTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.csv_title_preview), style = WheregoType.onboardTitle, color = colors.ink)
        Text(
            stringResource(R.string.csv_sub_preview),
            style = WheregoType.onboardSub,
            color = colors.muted,
        )
        if (preview.isEmpty()) {
            WheregoCard(cornerRadius = 22.dp, padding = 16.dp) {
                Text(
                    stringResource(R.string.csv_empty_no_match),
                    style = WheregoType.settingLabel,
                    color = colors.ink,
                )
                Text(
                    stringResource(R.string.csv_helper_no_match),
                    style = WheregoType.helper,
                    color = colors.muted,
                )
            }
        } else {
            if (total > preview.size) {
                Text(
                    stringResource(R.string.csv_showing_of, preview.size, total),
                    style = WheregoType.helper,
                    color = colors.muted,
                )
            }
            preview.forEach { row ->
                WheregoTxRow(
                    emoji = previewEmoji(row.kind),
                    title = row.note.ifBlank { row.category.ifBlank { kindLabel(row.kind) } },
                    subtitle = "${row.date} · ${kindLabel(row.kind)}",
                    amountLabel = previewAmount(row),
                    badgeSoftHex = "#D7E3F8",
                )
            }
        }
        if (errorRes != null) {
            Text(stringResource(errorRes), style = WheregoType.meta, color = colors.coral)
        }
    }
}

@Composable
private fun SuccessStep(count: Int) {
    val colors = WheregoTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(stringResource(R.string.csv_success_title), style = WheregoType.onboardTitle, color = colors.ink)
        Text(
            stringResource(R.string.csv_success_sub),
            style = WheregoType.onboardSub,
            color = colors.muted,
        )
        WheregoCard(
            cornerRadius = 30.dp,
            gap = 10.dp,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .wheregoHardShadow(shape = CircleShape, color = colors.shadow, offsetY = 5.dp)
                    .clip(CircleShape)
                    .background(colors.teal)
                    .border(BorderStroke(2.5.dp, colors.outlineStrong), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                WheregoWaypointMark(modifier = Modifier.size(56.dp), baseFillColor = colors.paper)
            }
            Text(
                pluralStringResource(R.plurals.csv_success_rows, count, count),
                style = WheregoType.statValue,
                color = colors.ink,
            )
            Text(
                stringResource(R.string.csv_success_helper),
                style = WheregoType.helper,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun HeaderToggle(selected: Boolean, onClick: () -> Unit) {
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
        Text(
            stringResource(R.string.csv_toggle_headers),
            style = WheregoType.settingLabel,
            color = colors.ink,
        )
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

@Composable
private fun MapField(
    label: String,
    selectedIndex: Int,
    columnCount: Int,
    headers: List<String>,
    skipHeader: Boolean,
    onSelect: (Int) -> Unit,
) {
    val colors = WheregoTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = WheregoType.settingLabel, color = colors.ink)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(columnCount) { index ->
                val selected = index == selectedIndex
                val chipLabel = columnLabel(headers, index, skipHeader)
                Text(
                    chipLabel,
                    style = WheregoType.stepText,
                    color = if (selected) Color.White else colors.ink,
                    modifier = Modifier
                        .clip(Pill)
                        .background(if (selected) colors.teal else colors.tealSoft)
                        .border(
                            BorderStroke(if (selected) 2.5.dp else 2.dp, if (selected) colors.outlineStrong else colors.tealSoft),
                            Pill,
                        )
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun SubtleAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = WheregoType.chip,
        color = WheregoTheme.colors.muted,
        modifier = Modifier
            .clip(Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

private fun looksLikeHeader(row: List<String>): Boolean =
    row.any { it.trim().lowercase() in HeaderNames }

@Composable
private fun columnLabel(headers: List<String>, index: Int, skipHeader: Boolean): String {
    val raw = if (skipHeader) headers.getOrNull(index)?.trim().orEmpty() else ""
    return raw.ifBlank { stringResource(R.string.csv_column_n, index + 1) }
}

@Composable
private fun kindLabel(kind: String): String = when (kind.trim().lowercase()) {
    TransactionKind.INCOME -> stringResource(R.string.kind_income)
    TransactionKind.ADJUSTMENT -> stringResource(R.string.kind_adjustment)
    else -> stringResource(R.string.kind_expense)
}

private fun previewEmoji(kind: String): String =
    if (kind.trim().lowercase() == TransactionKind.INCOME) "💼" else "✨"

private fun previewAmount(row: CsvRow): String {
    val minor = row.amount.toLongOrNull() ?: return row.amount
    val currency = row.currency.ifBlank { "IDR" }
    return MoneyFormatter.format(minor, currency)
}

private fun csvDisplayName(context: Context, uri: Uri): String {
    val fallback = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
    val queried = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
    }
    return queried?.takeIf { it.isNotBlank() } ?: fallback.ifBlank { "file.csv" }
}
