package com.flla.wherego.feature.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.designsystem.theme.parseHexColor
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.ThemeMode
import com.flla.wherego.feature.auth.AuthScreen

private val Palette = listOf(
    "#FF6B4A", "#0A7F70", "#4CA8FF", "#8B7CF6", "#E85A9B",
    "#C4A574", "#E07A5F", "#2A9D8F", "#10B5A0", "#E09F3E",
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
            onBalanceDigit = viewModel::onBalanceDigits,
            onBalanceBackspace = viewModel::onBalanceBackspace,
            onSetBalance = viewModel::setBalanceTo,
            onCategories = { showCats = true },
            onSignIn = { showAuth = true },
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
        )
    }
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    balanceMinor: Long,
    onDisplayName: (String) -> Unit,
    onTheme: (String) -> Unit,
    onBalanceDigit: (String) -> Unit,
    onBalanceBackspace: () -> Unit,
    onSetBalance: () -> Unit,
    onCategories: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val colors = WheregoTheme.colors
    var name by remember(state.displayName) { mutableStateOf(state.displayName) }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Me", style = WheregoType.cardTitle, color = colors.ink)
        Text(state.accountLine, style = WheregoType.meta, color = colors.muted)
        if (state.signedIn) {
            Text(
                "Sign out",
                style = WheregoType.cta,
                color = colors.tealDeep,
                modifier = Modifier.clickable(onClick = onSignOut),
            )
        } else {
            Text(
                "Sign in to backup",
                style = WheregoType.cta,
                color = colors.tealDeep,
                modifier = Modifier.clickable(onClick = onSignIn),
            )
        }
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
        Text("Appearance", style = WheregoType.chip, color = colors.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ThemeMode.SYSTEM to "System",
                ThemeMode.LIGHT to "Light",
                ThemeMode.DARK to "Dark",
            ).forEach { (value, label) ->
                val selected = state.themeMode == value
                Text(
                    text = label,
                    style = WheregoType.meta,
                    color = if (selected) colors.white else colors.ink,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (selected) colors.teal else colors.chipIdle)
                        .clickable { onTheme(value) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        Text("Set balance to", style = WheregoType.chip, color = colors.ink)
        Text(
            "Now ${MoneyFormatter.format(balanceMinor, state.currency)}",
            style = WheregoType.meta,
            color = colors.muted,
        )
        Text(
            MoneyFormatter.format(
                com.flla.wherego.core.model.DigitBuffer.amountMinor(state.balanceDigits),
                state.currency,
            ),
            style = WheregoType.heroAmount,
            color = colors.ink,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("1", "5", "0", "⌫").forEach { key ->
                Text(
                    key,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.chipIdle)
                        .clickable {
                            if (key == "⌫") onBalanceBackspace() else onBalanceDigit(key)
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    color = colors.ink,
                    style = WheregoType.chip,
                )
            }
        }
        Text(
            "Park it",
            color = colors.white,
            style = WheregoType.cta,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.teal)
                .clickable(onClick = onSetBalance)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        )
        Text(
            "Categories",
            style = WheregoType.cta,
            color = colors.tealDeep,
            modifier = Modifier.clickable(onClick = onCategories),
        )
        Text(
            "Export CSV",
            style = WheregoType.cta,
            color = colors.tealDeep,
            modifier = Modifier.clickable(onClick = onExport),
        )
        Text(
            "Import CSV",
            style = WheregoType.cta,
            color = colors.tealDeep,
            modifier = Modifier.clickable(onClick = onImport),
        )
    }
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
