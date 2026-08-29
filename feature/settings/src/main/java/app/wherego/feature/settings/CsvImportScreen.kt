package app.wherego.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.wherego.core.designsystem.theme.WheregoTheme
import app.wherego.core.designsystem.theme.WheregoType
import app.wherego.core.model.CsvImport
import app.wherego.core.model.CsvMapping
import kotlinx.coroutines.launch

@Composable
fun CsvImportScreen(
    onBack: () -> Unit,
    onCommit: suspend (String, CsvMapping, Boolean) -> Int,
) {
    val colors = WheregoTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var raw by remember { mutableStateOf("") }
    var mapping by remember { mutableStateOf(CsvMapping()) }
    var message by remember { mutableStateOf("") }
    val parsed = remember(raw) { if (raw.isBlank()) emptyList() else CsvImport.parse(raw) }
    val preview = remember(parsed, mapping) {
        if (parsed.isEmpty()) emptyList() else CsvImport.preview(parsed, mapping, skipHeader = true)
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
        raw = text
        CsvImport.parse(text).firstOrNull()?.let { mapping = CsvImport.guessMapping(it) }
        message = ""
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Back", color = colors.tealDeep, style = WheregoType.cta, modifier = Modifier.clickable(onClick = onBack))
        Text("Import CSV", style = WheregoType.cardTitle, color = colors.ink)
        Text("Pick a file", color = colors.tealDeep, style = WheregoType.cta, modifier = Modifier.clickable {
            pick.launch("text/*")
        })
        if (parsed.isNotEmpty()) {
            Text("Map columns (0-based)", style = WheregoType.chip, color = colors.ink)
            MapField("date", mapping.date) { mapping = mapping.copy(date = it) }
            MapField("kind", mapping.kind) { mapping = mapping.copy(kind = it) }
            MapField("amount", mapping.amount) { mapping = mapping.copy(amount = it) }
            MapField("currency", mapping.currency) { mapping = mapping.copy(currency = it) }
            MapField("category", mapping.category) { mapping = mapping.copy(category = it) }
            MapField("note", mapping.note) { mapping = mapping.copy(note = it) }
            Text("Preview", style = WheregoType.chip, color = colors.ink)
            preview.forEach { row ->
                Text(
                    "${row.date}  ${row.kind}  ${row.amount}  ${row.category}  ${row.note}",
                    style = WheregoType.meta,
                    color = colors.ink,
                )
            }
            Text(
                "Commit",
                color = colors.tealDeep,
                style = WheregoType.cta,
                modifier = Modifier.clickable {
                    scope.launch {
                        val n = onCommit(raw, mapping, true)
                        message = "Parked $n rows."
                    }
                },
            )
        }
        if (message.isNotEmpty()) {
            Text(message, style = WheregoType.meta, color = colors.tealDeep)
        }
    }
}

@Composable
private fun MapField(label: String, value: Int, onValue: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onValue(it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
    )
}
