package com.flla.wherego.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.model.DigitBuffer
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.PresetCategories
import com.flla.wherego.core.model.UserProfile

@Composable
fun OnboardingRoute(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreen(
        categoriesReady = state.categories.isNotEmpty(),
        onFinish = viewModel::completeOnboarding,
    )
}

@Composable
fun OnboardingScreen(
    categoriesReady: Boolean,
    onFinish: (currency: String, startingBalanceMinor: Long, displayName: String?) -> Unit,
) {
    val colors = WheregoTheme.colors
    var step by remember { mutableIntStateOf(0) }
    var currency by remember { mutableStateOf(UserProfile.DEFAULT_CURRENCY) }
    var digits by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (step == 0) {
            Text("Wherego", style = WheregoType.heroAmount, color = colors.ink)
            Text("One pot. Park spends as they happen.", style = WheregoType.meta, color = colors.muted)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Name (optional)") },
            )
            Text("Currency", style = WheregoType.chip, color = colors.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("IDR", "USD", "SGD").forEach { code ->
                    val selected = currency == code
                    Text(
                        code,
                        color = if (selected) colors.white else colors.ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (selected) colors.teal else colors.chipIdle)
                            .clickable { currency = code }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Text("Starting balance (optional)", style = WheregoType.chip, color = colors.ink)
            Text(
                MoneyFormatter.format(DigitBuffer.amountMinor(digits), currency),
                style = WheregoType.cardTitle,
                color = colors.ink,
            )
            OutlinedTextField(
                value = digits,
                onValueChange = { raw ->
                    digits = raw.filter { it.isDigit() }.take(DigitBuffer.MAX_DIGITS)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Amount") },
            )
            Text(
                "Continue",
                color = colors.white,
                style = WheregoType.cta,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.teal)
                    .clickable { step = 1 }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        } else {
            Text("Your categories", style = WheregoType.cardTitle, color = colors.ink)
            Text("You can rename these later in Me.", style = WheregoType.meta, color = colors.muted)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PresetCategories.all.forEach { cat ->
                    Text("${cat.emoji}  ${cat.name}", style = WheregoType.chip, color = colors.ink)
                }
            }
            Text(
                if (categoriesReady) "Let’s go" else "Loading…",
                color = colors.white,
                style = WheregoType.cta,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (categoriesReady) colors.teal else colors.muted)
                    .clickable(enabled = categoriesReady) {
                        onFinish(currency, DigitBuffer.amountMinor(digits), name.ifBlank { null })
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
    }
}
