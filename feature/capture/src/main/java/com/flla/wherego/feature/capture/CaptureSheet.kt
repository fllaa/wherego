package com.flla.wherego.feature.capture

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.ui.graphics.graphicsLayer
import com.flla.wherego.core.designsystem.component.wheregoHardShadow
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.designsystem.component.ParkItButton
import com.flla.wherego.core.designsystem.component.WheregoNumpad
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.i18n.R
import com.flla.wherego.core.i18n.categoryDisplayName
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.ReceiptSource
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
    initialReceiptUri: Uri? = null,
    /**
     * How far [initialReceiptUri] is trusted: an image shared in from another app is offered, never
     * filled in, and never queued for backup.
     */
    initialReceiptSource: ReceiptSource = ReceiptSource.OWN,
    onParked: (Transaction) -> Unit = {},
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSourcePicker by remember { mutableStateOf(false) }
    var showAttachedOptions by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val pickGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.attachReceipt(uri)
    }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        val uri = cameraUri
        if (ok && uri != null) viewModel.attachReceipt(uri)
    }

    val requestCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = cameraCaptureUri(context)
            cameraUri = uri
            takePicture.launch(uri)
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val uri = cameraCaptureUri(context)
            cameraUri = uri
            takePicture.launch(uri)
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(editing?.id, initialReceiptUri) {
        if (editing == null) {
            viewModel.beginCreate(initialReceiptUri, initialReceiptSource)
        } else {
            viewModel.beginEdit(editing)
        }
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
            onPhotoClick = {
                if (state.hasReceipt) {
                    showAttachedOptions = true
                } else {
                    showSourcePicker = true
                }
            },
            onApplyOcrAmount = viewModel::applySuggestedOcrAmount,
            onDismissOcrAmount = viewModel::dismissSuggestedOcrAmount,
            onCycleCurrency = viewModel::cycleCurrency,
            onFxRate = viewModel::onFxRate,
            onSave = { viewModel.save { parked -> onParked(parked); onDismiss() } },
        )
    }
    if (showSourcePicker) {
        AlertDialog(
            onDismissRequest = { showSourcePicker = false },
            title = { Text(stringResource(R.string.receipt_photo_source_title)) },
            text = { Text(stringResource(R.string.receipt_attach_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSourcePicker = false
                        launchCamera()
                    },
                ) { Text(stringResource(R.string.receipt_camera)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSourcePicker = false
                        pickGallery.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ) { Text(stringResource(R.string.receipt_gallery)) }
            },
        )
    }
    if (showAttachedOptions) {
        AlertDialog(
            onDismissRequest = { showAttachedOptions = false },
            title = { Text(stringResource(R.string.receipt_photo_change_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAttachedOptions = false
                        showSourcePicker = true
                    },
                ) { Text(stringResource(R.string.receipt_photo_replace)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAttachedOptions = false
                        viewModel.removeReceipt()
                    },
                ) { Text(stringResource(R.string.receipt_photo_remove)) }
            },
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
                ) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onPickDismissed) { Text(stringResource(R.string.dialog_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
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
    onPhotoClick: () -> Unit,
    onApplyOcrAmount: () -> Unit,
    onDismissOcrAmount: () -> Unit,
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
        if (state.isReconcile) {
            Text(
                stringResource(R.string.capture_balance_helper),
                style = WheregoType.helper,
                color = colors.muted,
            )
        }
        AmountDisplay(state = state)
        val suggestion = state.ocrSuggestion
        if (suggestion != null) {
            val label = MoneyFormatter.format(suggestion.minor, state.currency)
            // A guess says so. Only a read anchored to an amount label claims the receipt.
            val title = if (suggestion.anchored) {
                stringResource(R.string.receipt_ocr_banner_title, label)
            } else {
                stringResource(R.string.receipt_ocr_banner_unsure, label)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.tealSoft)
                    .border(1.5.dp, colors.accentText, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = WheregoType.meta,
                    color = colors.ink,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.receipt_use_it),
                        style = WheregoType.txAmount,
                        color = colors.accentText,
                        modifier = Modifier
                            .clickable(onClick = onApplyOcrAmount)
                            .padding(4.dp),
                    )
                    Text(
                        text = "✕",
                        style = WheregoType.txAmount,
                        color = colors.muted,
                        modifier = Modifier
                            .clickable(onClick = onDismissOcrAmount)
                            .padding(4.dp),
                    )
                }
            }
        }
        if (state.receiptSource == ReceiptSource.SHARED) {
            // Said out loud, because the image is a bank screen: it carries an account number and a
            // balance, and the guarantee that none of it leaves the phone is worth one line.
            Text(
                stringResource(R.string.receipt_shared_local),
                style = WheregoType.helper,
                color = colors.muted,
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateChip(
                stringResource(R.string.capture_chip_today),
                selected = today,
                showCalendar = true,
                onClick = onToday,
            )
            DateChip(stringResource(R.string.capture_chip_yesterday), selected = yesterday, onClick = onYesterday)
            DateChip(
                stringResource(R.string.capture_chip_pick),
                selected = !today && !yesterday && state.occurredOn.isNotEmpty(),
                showCalendar = true,
                onClick = onPickRequested,
            )
            if (!state.isReconcile) {
                QuickChip("10rb", onClick = { onQuickAmount(10_000L) })
                QuickChip("15rb", onClick = { onQuickAmount(15_000L) })
                QuickChip("25rb", onClick = { onQuickAmount(25_000L) })
            }
            QuickChip(stringResource(R.string.capture_chip_note), selected = state.noteOpen, onClick = onToggleNote)
            QuickChip(state.currency, selected = state.currency != state.baseCurrency, onClick = onCycleCurrency)
            if (!state.isReconcile) {
                val photoLabel = when {
                    state.isReadingOcr -> "⏳ " + stringResource(R.string.receipt_reading_title)
                    state.hasReceipt -> "✓ " + stringResource(R.string.capture_chip_photo)
                    else -> stringResource(R.string.capture_chip_photo)
                }
                QuickChip(
                    photoLabel,
                    selected = state.hasReceipt,
                    onClick = onPhotoClick,
                )
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
                placeholder = { Text(stringResource(R.string.capture_hint_fx, state.baseCurrency), color = colors.muted) },
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
                placeholder = { Text(stringResource(R.string.capture_hint_note), color = colors.muted) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
            )
        }
        if (!state.isReconcile) {
            CategoryRow(
                chips = state.chipCategories,
                selectedId = state.categoryId,
                onCategory = onCategory,
                onMore = onToggleMore,
            )
            AnimatedVisibility(
                visible = state.showAllCategories,
                enter = expandVertically(spring()) + fadeIn(),
                exit = shrinkVertically(spring()) + fadeOut(),
            ) {
                CategoryGrid(
                    categories = state.matchingCategories,
                    selectedId = state.categoryId,
                    onCategory = onCategory,
                )
            }
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
        listOf(
            TransactionKind.EXPENSE to stringResource(R.string.capture_tab_expense),
            TransactionKind.INCOME to stringResource(R.string.capture_tab_income),
            TransactionKind.RECONCILE to stringResource(R.string.capture_tab_balance),
        ).forEach { (value, label) ->
            val selected = kind == value
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) colors.teal else colors.sheet)
                    .then(
                        if (selected) Modifier.border(2.5.dp, colors.outlineStrong, RoundedCornerShape(14.dp))
                        else Modifier,
                    )
                    .clickable { onKind(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = WheregoType.kindTab,
                    color = if (selected) colors.onAccent else colors.ink,
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
private fun DateChip(label: String, selected: Boolean, onClick: () -> Unit, showCalendar: Boolean = false) {
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
        if (showCalendar) {
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
                .background(colors.white)
                .border(BorderStroke(2.dp, colors.outline), RoundedCornerShape(99.dp))
                .clickable(onClick = onMore),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.MoreHoriz,
                contentDescription = stringResource(R.string.capture_cd_more_categories),
                tint = colors.ink,
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selectedId: String?,
    onCategory: (String) -> Unit,
) {
    val colors = WheregoTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wheregoHardShadow(cornerRadius = 20.dp, offsetY = 3.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.paper)
            .border(BorderStroke(2.dp, colors.outline), RoundedCornerShape(20.dp))
            .padding(10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 160.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { cat ->
                        Box(modifier = Modifier.weight(1f)) {
                            CategoryChip(
                                cat = cat,
                                selected = cat.id == selectedId,
                                onClick = { onCategory(cat.id) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    // Fill remaining slots in row if less than 3
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
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
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "chipScale",
    )

    val fill = if (selected) colors.teal else colors.tealSoft
    val labelColor = if (selected) colors.onAccent else colors.ink

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(40.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(fill)
            .then(
                if (selected) {
                    Modifier.border(BorderStroke(2.5.dp, colors.outlineStrong), RoundedCornerShape(99.dp))
                } else {
                    Modifier.border(BorderStroke(1.5.dp, colors.outline.copy(alpha = 0.15f)), RoundedCornerShape(99.dp))
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(cat.emoji, fontSize = 16.sp)
        Text(
            text = categoryDisplayName(cat.id, cat.name),
            style = WheregoType.chip,
            color = labelColor,
            maxLines = 1,
        )
    }
}
