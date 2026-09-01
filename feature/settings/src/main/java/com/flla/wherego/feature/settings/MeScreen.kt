package com.flla.wherego.feature.settings

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
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
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.common.MonthPdfWriter
import com.flla.wherego.core.designsystem.component.ParkItButton
import com.flla.wherego.core.designsystem.component.WheregoBadge
import com.flla.wherego.core.designsystem.component.WheregoBottomSheet
import com.flla.wherego.core.designsystem.component.WheregoNumpad
import com.flla.wherego.core.designsystem.component.WheregoSectionLabel
import com.flla.wherego.core.designsystem.component.WheregoSettingDivider
import com.flla.wherego.core.designsystem.component.WheregoSettingRow
import com.flla.wherego.core.designsystem.component.WheregoSettingsCard
import com.flla.wherego.core.designsystem.component.WheregoStatsCard
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.designsystem.theme.parseHexColor
import com.flla.wherego.core.i18n.R
import com.flla.wherego.core.i18n.categoryDisplayName
import com.flla.wherego.core.i18n.dayTitle
import com.flla.wherego.core.i18n.displayAmount
import com.flla.wherego.core.i18n.monthShort
import com.flla.wherego.core.i18n.monthYear
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.CategoryKind
import com.flla.wherego.core.model.DigitBuffer
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.Recurrence
import com.flla.wherego.core.model.ThemeMode
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

private val Palette = listOf(
    "#121826", "#163A8A", "#2157C7", "#4B86FF", "#5A6A80",
    "#8FB0FF", "#D7E3F8", "#E1E7F0", "#E24B4B", "#F4D6D6",
)

/** The base currencies `Me → YOUR MONEY → Currency` offers, mirroring `Onboarding 2/3`. */
private val Currencies = listOf(
    "IDR" to R.string.currency_idr,
    "USD" to R.string.currency_usd,
    "SGD" to R.string.currency_sgd,
    "MYR" to R.string.currency_myr,
    "EUR" to R.string.currency_eur,
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
    val page = when {
        showAuth -> MePage.Profile
        showCats -> MePage.Categories
        showImport -> MePage.Import
        else -> MePage.Root
    }
    val shareSubject = stringResource(R.string.me_share_subject)
    val shareCsvTitle = stringResource(R.string.me_share_csv_title)
    val shareMonthTitle = stringResource(R.string.me_share_month_title)
    val pdfTitleLine = stringResource(
        R.string.pdf_title,
        monthYear(YearMonth.parse(state.yearMonth)),
    )
    val pdfSpentFormat = stringResource(R.string.pdf_spent)
    val pdfNoBars = stringResource(R.string.pdf_no_bars)
    val pdfNoTxs = stringResource(R.string.pdf_no_transactions)
    AnimatedContent(
        targetState = page,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val deeper = initialState == MePage.Root && targetState != MePage.Root
            val back = initialState != MePage.Root && targetState == MePage.Root
            val spec = tween<IntOffset>(220, easing = FastOutSlowInEasing)
            when {
                deeper ->
                    slideInHorizontally(spec) { it } togetherWith
                        slideOutHorizontally(spec) { -it / 4 }
                back ->
                    slideInHorizontally(spec) { -it / 4 } togetherWith
                        slideOutHorizontally(spec) { it }
                else -> EnterTransition.None togetherWith ExitTransition.None
            }.using(SizeTransform(clip = true))
        },
        label = "mePush",
    ) { current ->
        when (current) {
            MePage.Profile -> ProfileScreen(onBack = { showAuth = false })
            MePage.Categories -> CategoryManagerScreen(
                categories = state.categories,
                onBack = { showCats = false },
                onSave = { id, name, emoji, color, kind ->
                    viewModel.updateCategory(id, name, emoji, color, kind)
                },
                onCreate = viewModel::createCategory,
                onPin = viewModel::pinCategory,
                onArchive = viewModel::archiveCategory,
            )
            MePage.Import -> CsvImportScreen(
                onBack = { showImport = false },
                onCommit = { text, mapping, skip -> viewModel.importCsv(text, mapping, skip) },
            )
            MePage.Root -> SettingsScreen(
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
                            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                        }
                        context.startActivity(Intent.createChooser(send, shareCsvTitle))
                    }
                },
                onImport = { showImport = true },
                onMonthPdf = {
                    scope.launch {
                        val uri = MonthPdfWriter.write(
                            context,
                            "wherego-${state.yearMonth}.pdf",
                            viewModel.monthPdfLines(
                                titleLine = pdfTitleLine,
                                totalLine = pdfSpentFormat,
                                emptyBars = pdfNoBars,
                                emptyTxs = pdfNoTxs,
                            ),
                        )
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(send, shareMonthTitle))
                    }
                },
                onToggleAmounts = viewModel::toggleAmountsHidden,
            )
        }
    }
}

private enum class MePage { Root, Profile, Categories, Import }

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
    onToggleAmounts: () -> Unit,
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
        Text(stringResource(R.string.me_title), style = WheregoType.pageTitle, color = colors.ink)
        ProfileCard(state = state, onClick = onAccount)
        WheregoStatsCard(
            listOf(
                state.streakDays.toString() to stringResource(R.string.me_stat_day_streak),
                state.logsThisMonth.toString() to stringResource(
                    R.string.me_stat_logs_in,
                    monthShort(YearMonth.parse(state.yearMonth)),
                ),
                "${state.daysLogged}/${state.daysInMonth}" to stringResource(R.string.me_stat_days_logged),
            ),
        )
        SettingsGroup(stringResource(R.string.me_section_your_money)) {
            WheregoSettingRow(
                icon = Icons.Outlined.Sell,
                badgeFill = colors.peach,
                label = stringResource(R.string.me_row_categories),
                onClick = onCategories,
                value = state.categoryCount.toString(),
            )
            WheregoSettingDivider()
            WheregoSettingRow(
                icon = Icons.Outlined.Repeat,
                badgeFill = colors.blueSoft,
                label = stringResource(R.string.me_row_recurring),
                onClick = { sheet = MeSheet.RECURRING },
                value = pluralStringResource(
                    R.plurals.me_value_active,
                    state.recurringActiveCount,
                    state.recurringActiveCount,
                ),
            )
            WheregoSettingDivider()
            WheregoSettingRow(
                icon = Icons.Outlined.Balance,
                badgeFill = colors.greenSoft,
                label = stringResource(R.string.me_row_adjust_balance),
                onClick = { sheet = MeSheet.BALANCE },
            )
            WheregoSettingDivider()
            WheregoSettingRow(
                icon = Icons.Outlined.Payments,
                badgeFill = colors.amberSoft,
                label = stringResource(R.string.me_row_currency),
                onClick = { sheet = MeSheet.CURRENCY },
                value = state.currency,
            )
        }
        SettingsGroup(stringResource(R.string.me_section_app)) {
            WheregoSettingRow(
                icon = Icons.Outlined.DarkMode,
                badgeFill = colors.violetSoft,
                label = stringResource(R.string.me_row_appearance),
                onClick = { sheet = MeSheet.APPEARANCE },
                value = themeLabel(state.themeMode),
            )
            WheregoSettingDivider()
            WheregoSettingRow(
                icon = Icons.Outlined.Notifications,
                badgeFill = colors.amberSoft,
                label = stringResource(R.string.me_row_reminders),
                onClick = { sheet = MeSheet.REMINDERS },
                value = if (state.remindersOn) {
                    stringResource(R.string.me_value_on)
                } else {
                    stringResource(R.string.me_value_off)
                },
            )
            WheregoSettingDivider()
            WheregoSettingRow(
                icon = Icons.Outlined.VisibilityOff,
                badgeFill = colors.blueSoft,
                label = stringResource(R.string.me_row_hide_amounts),
                onClick = onToggleAmounts,
                value = if (state.amountsHidden) {
                    stringResource(R.string.me_value_on)
                } else {
                    stringResource(R.string.me_value_off)
                },
            )
        }
        SettingsGroup(stringResource(R.string.me_section_data)) {
            WheregoSettingRow(
                icon = Icons.Outlined.Download,
                badgeFill = colors.greenSoft,
                label = stringResource(R.string.me_row_export_csv),
                onClick = onExport,
            )
            WheregoSettingDivider()
            WheregoSettingRow(
                icon = Icons.Outlined.Upload,
                badgeFill = colors.tealSoft,
                label = stringResource(R.string.me_row_import_csv),
                onClick = onImport,
            )
            WheregoSettingDivider()
            WheregoSettingRow(
                icon = Icons.Outlined.Description,
                badgeFill = colors.blueSoft,
                label = stringResource(R.string.me_row_month_pdf),
                onClick = onMonthPdf,
            )
        }
        Spacer(Modifier.height(2.dp))
        AccountRow(signedIn = state.signedIn, onSignOut = onSignOut, onSignIn = onAccount)
    }
    when (sheet) {
        MeSheet.NONE -> Unit
        MeSheet.APPEARANCE -> MeBottomSheet(
            stringResource(R.string.me_sheet_appearance),
            onDismiss = { sheet = MeSheet.NONE },
        ) {
            listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK).forEach { value ->
                ChoiceRow(
                    label = themeLabel(value),
                    selected = state.themeMode == value,
                    onClick = { onTheme(value) },
                )
            }
        }
        MeSheet.CURRENCY -> MeBottomSheet(
            stringResource(R.string.me_sheet_currency),
            onDismiss = { sheet = MeSheet.NONE },
        ) {
            Currencies.forEach { (code, nameRes) ->
                ChoiceRow(
                    label = code,
                    selected = state.currency == code,
                    onClick = { onCurrency(code) },
                    hint = stringResource(nameRes),
                )
            }
        }
        MeSheet.BALANCE -> MeBottomSheet(
            stringResource(R.string.me_sheet_adjust_balance),
            onDismiss = { sheet = MeSheet.NONE },
        ) {
            BalanceSheetBody(
                state = state,
                balanceMinor = balanceMinor,
                onBalanceDigit = onBalanceDigit,
                onBalanceBackspace = onBalanceBackspace,
                onSetBalance = {
                    onSetBalance()
                    sheet = MeSheet.NONE
                },
            )
        }
        MeSheet.RECURRING -> MeBottomSheet(
            stringResource(R.string.me_sheet_recurring),
            onDismiss = { sheet = MeSheet.NONE },
        ) {
            if (state.recurringRules.isEmpty()) {
                Text(
                    stringResource(R.string.me_empty_recurring),
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
                            Text(
                                recurringLabel(rule),
                                style = WheregoType.txTitle,
                                color = colors.ink,
                            )
                            Text(
                                recurringDetail(rule),
                                style = WheregoType.meterDetail,
                                color = colors.muted,
                            )
                        }
                    }
                }
                Text(
                    stringResource(R.string.me_recurring_plan_owns),
                    style = WheregoType.helper,
                    color = colors.muted,
                )
            }
        }
        MeSheet.REMINDERS -> MeBottomSheet(
            stringResource(R.string.me_sheet_reminders),
            onDismiss = { sheet = MeSheet.NONE },
        ) {
            Text(
                stringResource(R.string.me_reminders_on_body),
                style = WheregoType.helper,
                color = colors.muted,
            )
            Text(
                stringResource(R.string.me_reminders_body_2),
                style = WheregoType.helper,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun themeLabel(mode: String): String = when (mode) {
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
    else -> stringResource(R.string.theme_system)
}

@Composable
private fun recurringLabel(rule: RecurringSummary): String {
    if (rule.note.isNotBlank()) return rule.note
    val stored = rule.categoryName
    return if (stored != null) {
        categoryDisplayName(rule.categoryId, stored)
    } else {
        stringResource(R.string.recurring_fallback_label)
    }
}

@Composable
private fun recurringDetail(rule: RecurringSummary): String {
    val amount = displayAmount(MoneyFormatter.format(rule.amountMinor, rule.currency))
    val freq = if (rule.freq == Recurrence.WEEKLY) {
        stringResource(R.string.freq_weekly)
    } else {
        stringResource(R.string.freq_monthly)
    }
    val next = stringResource(R.string.me_recurring_next, dayTitle(LocalDate.parse(rule.nextOn)))
    return listOf(amount, freq, next).joinToString(" · ")
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
            .border(2.5.dp, colors.outline, shape)
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
                .border(2.5.dp, colors.outline, RoundedCornerShape(27.dp)),
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
                state.displayName.ifBlank { stringResource(R.string.me_greeting_fallback) },
                style = WheregoType.cardTitle,
                color = colors.ink,
            )
            Text(
                state.email?.takeIf { it.isNotBlank() }
                    ?: state.accountLine
                    ?: if (state.signedIn) {
                        stringResource(R.string.settings_account_signed_in)
                    } else {
                        stringResource(R.string.settings_account_guest)
                    },
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
            if (signedIn) stringResource(R.string.me_pill_synced) else stringResource(R.string.me_pill_offline),
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
    val tint = if (signedIn) colors.coral else colors.accentText
    val icon: ImageVector =
        if (signedIn) Icons.AutoMirrored.Outlined.Logout else Icons.AutoMirrored.Outlined.Login
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(colors.sheet)
            .border(2.5.dp, colors.outline, shape)
            .clickable(onClick = if (signedIn) onSignOut else onSignIn),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        Spacer(Modifier.size(8.dp))
        Text(
            if (signedIn) stringResource(R.string.me_sign_out) else stringResource(R.string.me_sign_in),
            style = WheregoType.settingLabel.copy(fontSize = 15.sp),
            color = tint,
        )
    }
}

@Composable
private fun MeBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    WheregoBottomSheet(title = title, onDismiss = onDismiss, content = content)
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

/** The `Set balance to` keypad, demoted off the main page. */
@Composable
private fun BalanceSheetBody(
    state: SettingsUiState,
    balanceMinor: Long,
    onBalanceDigit: (String) -> Unit,
    onBalanceBackspace: () -> Unit,
    onSetBalance: () -> Unit,
) {
    val colors = WheregoTheme.colors
    Text(
        stringResource(
            R.string.me_balance_now,
            displayAmount(MoneyFormatter.format(balanceMinor, state.currency)),
        ),
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
}

