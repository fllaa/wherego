package com.flla.wherego.feature.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.common.MonthPdfWriter
import com.flla.wherego.core.designsystem.component.ParkItButton
import com.flla.wherego.core.designsystem.component.WheregoBadge
import com.flla.wherego.core.designsystem.component.WheregoNumpad
import com.flla.wherego.core.designsystem.component.WheregoSectionLabel
import com.flla.wherego.core.designsystem.component.WheregoSettingDivider
import com.flla.wherego.core.designsystem.component.WheregoSettingRow
import com.flla.wherego.core.designsystem.component.WheregoSettingsCard
import com.flla.wherego.core.designsystem.component.WheregoStatsCard
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.designsystem.theme.parseHexColor
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.DigitBuffer
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.ThemeMode
import com.flla.wherego.feature.auth.AuthScreen
import kotlinx.coroutines.launch

private val Palette = listOf(
    "#121826", "#163A8A", "#2157C7", "#4B86FF", "#5A6A80",
    "#8FB0FF", "#D7E3F8", "#E1E7F0", "#E24B4B", "#F4D6D6",
)

/** The base currencies `Me → YOUR MONEY → Currency` offers, mirroring `Onboarding 2/3`. */
private val Currencies = listOf(
    "IDR" to "Indonesian Rupiah",
    "USD" to "US Dollar",
    "SGD" to "Singapore Dollar",
    "MYR" to "Malaysian Ringgit",
    "EUR" to "Euro",
)

@Composable
fun MeScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val balance by viewModel.balanceNow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCats by remember { mutableStateOf(false) }
    var showAuth by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    when {
        showAuth -> AuthScreen(onBack = { showAuth = false })
        showCats -> CategoryManagerScreen(
            categories = state.categories,
            onBack = { showCats = false },
            onSave = viewModel::updateCategory,
            onArchive = viewModel::archiveCategory,
        )
        showImport -> CsvImportScreen(
            onBack = { showImport = false },
            onCommit = { text, mapping, skip -> viewModel.importCsv(text, mapping, skip) },
        )
        else -> SettingsScreen(
            state = state,
            balanceMinor = balance,
            onDisplayName = viewModel::onDisplayName,
            onTheme = viewModel::onTheme,
            onCurrency = viewModel::onCurrency,
            onBalanceDigit = viewModel::onBalanceDigits,
            onBalanceBackspace = viewModel::onBalanceBackspace,
            onSetBalance = viewModel::setBalanceTo,
            onCategories = { showCats = true },
            onAccount = { showAuth = true },
            onSignOut = viewModel::signOut,
            onExport = {
                scope.launch {
                    val csv = viewModel.exportCsv()
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_TEXT, csv)
                        putExtra(Intent.EXTRA_SUBJECT, "Wherego export")
                    }
                    context.startActivity(Intent.createChooser(send, "Export CSV"))
                }
            },
            onImport = { showImport = true },
            onMonthPdf = {
                scope.launch {
                    val uri = MonthPdfWriter.write(
                        context,
                        "wherego-${state.yearMonth}.pdf",
                        viewModel.monthPdfLines(),
                    )
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, "Share month"))
                }
            },
        )
    }
}

/** Which demoted control is currently open in a sheet. */
private enum class MeSheet { NONE, APPEARANCE, BALANCE, CURRENCY, RECURRING, REMINDERS }

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    balanceMinor: Long,
    onDisplayName: (String) -> Unit,
    onTheme: (String) -> Unit,
    onCurrency: (String) -> Unit,
    onBalanceDigit: (String) -> Unit,
    onBalanceBackspace: () -> Unit,
    onSetBalance: () -> Unit,
    onCategories: () -> Unit,
    onAccount: () -> Unit,
    onSignOut: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onMonthPdf: () -> Unit,
) {
    val colors = WheregoTheme.colors
    var sheet by remember { mutableStateOf(MeSheet.NONE) }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Me", style = WheregoType.pageTitle, color = colors.ink)
        ProfileCard(state = state, onClick = onAccount)
        WheregoStatsCard(
            listOf(
                state.streakDays.toString() to "day streak",
                state.logsThisMonth.toString() to "logs in ${state.monthShortLabel}",
                "${state.daysLogged}/${state.daysInMonth}" to "days logged",
            ),
        )
        SettingsGroup("YOUR MONEY") {
            WheregoSettingRow(
                icon = Icons.Outlined.Sell,
                badgeFill = colors.peach,
                label = "Categories",
                onClick = onCategories,
                value = state.categoryCount.toString(),
            )
            WheregoSettingDivider()
            WheregoSettingRow(
                icon = Icons.Outlined.Repeat,
                badgeFill = colors.blueSoft,
                label = "Recurring",
                onClick = { sheet = MeSheet.RECURRING },
                value = "${state.recurringActiveCount} active",
            )
            WheregoSettingDivider()
            WheregoSettingRow(
                icon = Icons.Outlined.Balance,
                badgeFill = colors.greenSoft,
                label = "Adjust balance",
                onClick = { sheet = MeSheet.BALANCE },
            )
            WheregoSettingDivider()
            WheregoSettingRow(
                icon = Icons.Outlined.Payments,
                badgeFill = colors.amberSoft,
                label = "Currency",
                onClick = { sheet = MeSheet.CURRENCY },
                value = state.currency,
            )
        }
        SettingsGroup("APP") {
            WheregoSettingRow(
                icon = Icons.Outlined.DarkMode,
                badgeFill = colors.violetSoft,
                label = "Appearance",
                onClick = { sheet = MeSheet.APPEARANCE },
                value = themeLabel(state.themeMode),
            )
            WheregoSettingDivider()
            WheregoSettingRow(
                icon = Icons.Outlined.Notifications,
                badgeFill = colors.amberSoft,
                label = "Reminders",
                onClick = { sheet = MeSheet.REMINDERS },
                value = if (state.remindersOn) "On" else "Off",
            )
        }
        SettingsGroup("DATA") {
            WheregoSettingRow(
                icon = Icons.Outlined.Download,
                badgeFill = colors.greenSoft,
                label = "Export CSV",
                onClick = onExport,
            )
            WheregoSettingDivider()
            WheregoSettingRow(
                icon = Icons.Outlined.Upload,
                badgeFill = colors.tealSoft,
                label = "Import CSV",
                onClick = onImport,
            )
            WheregoSettingDivider()
            WheregoSettingRow(
                icon = Icons.Outlined.Description,
                badgeFill = colors.blueSoft,
                label = "Month report PDF",
                onClick = onMonthPdf,
            )
        }
        Spacer(Modifier.height(2.dp))
        AccountRow(signedIn = state.signedIn, onSignOut = onSignOut, onSignIn = onAccount)
    }
    when (sheet) {
        MeSheet.NONE -> Unit
        MeSheet.APPEARANCE -> MeBottomSheet("Appearance", onDismiss = { sheet = MeSheet.NONE }) {
            listOf(
                ThemeMode.SYSTEM to "System",
                ThemeMode.LIGHT to "Light",
                ThemeMode.DARK to "Dark",
            ).forEach { (value, label) ->
                ChoiceRow(
                    label = label,
                    selected = state.themeMode == value,
                    onClick = { onTheme(value) },
                )
            }
        }
        MeSheet.CURRENCY -> MeBottomSheet("Currency", onDismiss = { sheet = MeSheet.NONE }) {
            Currencies.forEach { (code, name) ->
                ChoiceRow(
                    label = code,
                    selected = state.currency == code,
                    onClick = { onCurrency(code) },
                    hint = name,
                )
            }
        }
        MeSheet.BALANCE -> MeBottomSheet("Adjust balance", onDismiss = { sheet = MeSheet.NONE }) {
            BalanceSheetBody(
                state = state,
                balanceMinor = balanceMinor,
                onDisplayName = onDisplayName,
                onBalanceDigit = onBalanceDigit,
                onBalanceBackspace = onBalanceBackspace,
                onSetBalance = {
                    onSetBalance()
                    sheet = MeSheet.NONE
                },
            )
        }
        MeSheet.RECURRING -> MeBottomSheet("Recurring", onDismiss = { sheet = MeSheet.NONE }) {
            if (state.recurringRules.isEmpty()) {
                Text(
                    "Nothing repeating yet. Add rules in Plan.",
                    style = WheregoType.helper,
                    color = colors.muted,
                )
            } else {
                state.recurringRules.forEach { rule ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        WheregoBadge(fill = colors.blueSoft, size = 34.dp, cornerRadius = 17.dp) {
                            Text(rule.emoji, fontSize = 16.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(rule.label, style = WheregoType.txTitle, color = colors.ink)
                            Text(rule.detail, style = WheregoType.meterDetail, color = colors.muted)
                        }
                    }
                }
                Text(
                    "Plan owns editing — open Plan to change a rule.",
                    style = WheregoType.helper,
                    color = colors.muted,
                )
            }
        }
        MeSheet.REMINDERS -> MeBottomSheet("Reminders", onDismiss = { sheet = MeSheet.NONE }) {
            Text(
                "On. Wherego pings you the morning a recurring bill is due.",
                style = WheregoType.helper,
                color = colors.muted,
            )
            Text(
                "Every rule you add in Plan schedules its own reminder.",
                style = WheregoType.helper,
                color = colors.muted,
            )
        }
    }
}

private fun themeLabel(mode: String): String = when (mode) {
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
    else -> "System"
}

/** `Content / Profile Card` — avatar initial, name + email, sync pill. */
@Composable
private fun ProfileCard(state: SettingsUiState, onClick: () -> Unit) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.sheet)
            .border(2.5.dp, colors.ink, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(colors.violetSoft)
                .border(2.5.dp, colors.ink, RoundedCornerShape(27.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                state.initial,
                style = WheregoType.pageTitle.copy(fontSize = 24.sp),
                color = colors.ink,
            )
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                state.displayName.ifBlank { "Hey you" },
                style = WheregoType.cardTitle,
                color = colors.ink,
            )
            Text(
                state.email ?: state.accountLine,
                style = WheregoType.helper,
                color = colors.muted,
            )
        }
        SyncPill(signedIn = state.signedIn, onClick = onClick)
    }
}

@Composable
private fun SyncPill(signedIn: Boolean, onClick: () -> Unit) {
    val colors = WheregoTheme.colors
    val pill = RoundedCornerShape(99.dp)
    val tint = if (signedIn) colors.onGreenSoft else colors.muted
    Row(
        Modifier
            .clip(pill)
            .background(if (signedIn) colors.greenSoft else colors.track)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tint),
        )
        Text(
            if (signedIn) "Synced" else "Offline",
            style = WheregoType.leftPill,
            color = tint,
        )
    }
}

/** A group label plus its ink-outlined, hairline-divided card. */
@Composable
private fun SettingsGroup(title: String, rows: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WheregoSectionLabel(title)
        WheregoSettingsCard { rows() }
    }
}

/** `Content / Sign Out` — coral when signed in, accent "Sign in" for a guest. */
@Composable
private fun AccountRow(signedIn: Boolean, onSignOut: () -> Unit, onSignIn: () -> Unit) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(22.dp)
    val tint = if (signedIn) colors.coral else colors.teal
    val icon: ImageVector =
        if (signedIn) Icons.AutoMirrored.Outlined.Logout else Icons.AutoMirrored.Outlined.Login
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(colors.sheet)
            .border(2.5.dp, colors.ink, shape)
            .clickable(onClick = if (signedIn) onSignOut else onSignIn),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        Spacer(Modifier.size(8.dp))
        Text(
            if (signedIn) "Sign out" else "Sign in",
            style = WheregoType.settingLabel.copy(fontSize = 15.sp),
            color = tint,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WheregoTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.sheet,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(width = 44.dp, height = 5.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(colors.track),
                )
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = WheregoType.cardTitle, color = colors.ink)
            content()
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    hint: String? = null,
) {
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
    ) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, style = WheregoType.settingLabel, color = colors.ink)
            if (hint != null) Text(hint, style = WheregoType.helper, color = colors.muted)
        }
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

/** The `Set balance to` keypad plus the display-name field, demoted off the main page. */
@Composable
private fun BalanceSheetBody(
    state: SettingsUiState,
    balanceMinor: Long,
    onDisplayName: (String) -> Unit,
    onBalanceDigit: (String) -> Unit,
    onBalanceBackspace: () -> Unit,
    onSetBalance: () -> Unit,
) {
    val colors = WheregoTheme.colors
    var name by remember(state.displayName) { mutableStateOf(state.displayName) }
    Text(
        "Now ${MoneyFormatter.format(balanceMinor, state.currency)}",
        style = WheregoType.helper,
        color = colors.muted,
    )
    Text(
        MoneyFormatter.format(DigitBuffer.amountMinor(state.balanceDigits), state.currency),
        style = WheregoType.heroAmount.copy(fontSize = 34.sp, lineHeight = 42.sp),
        color = colors.ink,
    )
    WheregoNumpad(onDigit = onBalanceDigit, onBackspace = onBalanceBackspace)
    ParkItButton(enabled = state.balanceDigits.isNotBlank(), onClick = onSetBalance)
    OutlinedTextField(
        value = name,
        onValueChange = {
            name = it.take(40)
            onDisplayName(name)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Display name") },
    )
}

@Composable
fun CategoryManagerScreen(
    categories: List<Category>,
    onBack: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    onArchive: (String, Boolean) -> Unit,
) {
    val colors = WheregoTheme.colors
    var editing by remember { mutableStateOf<Category?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .padding(18.dp),
    ) {
        Text(
            "← Categories",
            style = WheregoType.cardTitle,
            color = colors.ink,
            modifier = Modifier.clickable(onClick = onBack),
        )
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            categories.forEach { cat ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.white)
                        .clickable { editing = cat }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(cat.emoji, style = WheregoType.cardTitle)
                    Column(Modifier.weight(1f)) {
                        Text(cat.name, style = WheregoType.chip, color = colors.ink)
                        Text(
                            if (cat.archived) "Archived" else cat.kind,
                            style = WheregoType.meta,
                            color = colors.muted,
                        )
                    }
                }
            }
        }
    }
    val target = editing
    if (target != null) {
        CategoryEditDialog(
            category = target,
            onDismiss = { editing = null },
            onSave = { name, emoji, color ->
                onSave(target.id, name, emoji, color)
                editing = null
            },
            onArchive = {
                onArchive(target.id, !target.archived)
                editing = null
            },
        )
    }
}

@Composable
private fun CategoryEditDialog(
    category: Category,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onArchive: () -> Unit,
) {
    var name by remember { mutableStateOf(category.name) }
    var emoji by remember { mutableStateOf(category.emoji) }
    var color by remember { mutableStateOf(category.colorHex) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(24) }, singleLine = true, label = { Text("Name") })
                OutlinedTextField(value = emoji, onValueChange = { emoji = it.take(4) }, singleLine = true, label = { Text("Emoji") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Palette.forEach { hex ->
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .then(
                                    if (hex.equals(color, ignoreCase = true)) {
                                        Modifier.border(2.dp, WheregoTheme.colors.ink, CircleShape)
                                    } else Modifier,
                                )
                                .clickable { color = hex },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, emoji, color) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onArchive) {
                    Text(if (category.archived) "Unarchive" else "Archive")
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}
