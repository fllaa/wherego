package com.flla.wherego.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.designsystem.component.WheregoCard
import com.flla.wherego.core.designsystem.component.WheregoOnboardTopBar
import com.flla.wherego.core.designsystem.component.WheregoPrimaryButton
import com.flla.wherego.core.designsystem.component.wheregoHardShadow
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.model.CategoryPack
import com.flla.wherego.core.model.CurrencyScale
import com.flla.wherego.core.model.DigitBuffer
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.PresetCategories
import com.flla.wherego.core.model.UserProfile

private const val STEP_COUNT = 4

private data class CurrencyOption(val code: String, val label: String, val symbol: String)

private val CURRENCIES = listOf(
    CurrencyOption("IDR", "Indonesian Rupiah", "Rp"),
    CurrencyOption("USD", "US Dollar", "$"),
    CurrencyOption("SGD", "Singapore Dollar", "S$"),
    CurrencyOption("MYR", "Malaysian Ringgit", "RM"),
    CurrencyOption("EUR", "Euro", "€"),
)

@Composable
fun OnboardingRoute(
    onBackToWelcome: () -> Unit,
    onFinish: (openCapture: Boolean) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreen(
        categoriesReady = state.categories.isNotEmpty(),
        onBackToWelcome = onBackToWelcome,
        onFinish = { currency, balanceMinor, keptCategoryIds, openCapture ->
            viewModel.completeOnboarding(currency, balanceMinor, keptCategoryIds)
            onFinish(openCapture)
        },
    )
}

/**
 * The four-card first-run tour from `pencil-new.pen`:
 * `Onboarding 1 · Welcome`, `2 · Currency`, `3 · Categories`, `4 · First log`.
 * Every step can be skipped — the tour writes defaults rather than blocking capture.
 */
@Composable
fun OnboardingScreen(
    categoriesReady: Boolean,
    onBackToWelcome: () -> Unit,
    onFinish: (
        currency: String,
        startingBalanceMinor: Long,
        keptCategoryIds: Set<String>?,
        openCapture: Boolean,
    ) -> Unit,
) {
    val colors = WheregoTheme.colors
    var step by remember { mutableIntStateOf(0) }
    var currency by remember { mutableStateOf(UserProfile.DEFAULT_CURRENCY) }
    var digits by remember { mutableStateOf("") }
    var packId by remember { mutableStateOf(PresetCategories.packs.first().id) }
    var kept by remember {
        mutableStateOf(PresetCategories.packs.first().categoryIds.toSet())
    }

    fun finish(openCapture: Boolean) =
        onFinish(currency, DigitBuffer.amountMinor(digits), kept, openCapture)

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .systemBarsPadding(),
    ) {
        WheregoOnboardTopBar(
            stepIndex = step,
            stepCount = STEP_COUNT,
            onBack = { if (step == 0) onBackToWelcome() else step-- },
            onSkip = { finish(false) },
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = 18.dp, start = 26.dp, end = 26.dp, bottom = 42.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            when (step) {
                0 -> WelcomeStep()
                1 -> CurrencyStep(
                    currency = currency,
                    digits = digits,
                    onCurrency = { currency = it },
                    onDigits = { digits = it },
                )
                2 -> CategoriesStep(
                    packId = packId,
                    kept = kept,
                    onPack = { pack ->
                        packId = pack.id
                        kept = pack.categoryIds.toSet()
                    },
                    onCustom = { packId = PresetCategories.CUSTOM_PACK_ID },
                    onToggle = { id ->
                        packId = PresetCategories.CUSTOM_PACK_ID
                        kept = if (id in kept) kept - id else kept + id
                    },
                )
                else -> FirstLogStep()
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                when (step) {
                    0 -> WheregoPrimaryButton("Let’s go", onClick = { step = 1 })
                    1 -> {
                        WheregoPrimaryButton("Continue", onClick = { step = 2 })
                        SubtleAction("I don’t know — skip this") {
                            digits = ""
                            step = 2
                        }
                    }
                    2 -> WheregoPrimaryButton(
                        "Continue",
                        onClick = { step = 3 },
                        enabled = categoriesReady && kept.isNotEmpty(),
                    )
                    else -> {
                        WheregoPrimaryButton("Log it now", onClick = { finish(true) })
                        SubtleAction("Take me to Home, I’ll log later") { finish(false) }
                    }
                }
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
            .clip(RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun WelcomeStep() {
    val colors = WheregoTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(
            Modifier
                .size(72.dp)
                .wheregoHardShadow(shape = CircleShape, color = colors.shadow, offsetY = 4.dp)
                .clip(CircleShape)
                .background(colors.teal)
                .border(BorderStroke(2.5.dp, colors.ink), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("🪙", fontSize = 32.sp, color = colors.ink)
        }
        Text(
            "You don’t need a spreadsheet. You need 20 seconds.",
            style = WheregoType.onboardTitleLarge,
            color = colors.ink,
        )
        Text(
            "Wherego is a pocket notebook, not a filing cabinet.",
            style = WheregoType.onboardSub,
            color = colors.muted,
        )
        WheregoCard(gap = 14.dp) {
            val steps = listOf(
                "Tap the big + the moment you pay.",
                "Type the amount. Tap a picture.",
                "That’s it. No forms, no folders.",
            )
            steps.forEachIndexed { index, line ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(colors.tealSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", style = WheregoType.streakNum, color = colors.tealDeep)
                    }
                    Text(line, style = WheregoType.stepText, color = colors.ink)
                }
            }
        }
    }
}

@Composable
private fun CurrencyStep(
    currency: String,
    digits: String,
    onCurrency: (String) -> Unit,
    onDigits: (String) -> Unit,
) {
    val colors = WheregoTheme.colors
    val selected = CURRENCIES.firstOrNull { it.code == currency } ?: CURRENCIES.first()
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("What are we counting?", style = WheregoType.onboardTitle, color = colors.ink)
        WheregoCard(cornerRadius = 24.dp, padding = 14.dp, gap = 0.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.tealSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(selected.symbol, style = WheregoType.cardTitle, color = colors.tealDeep)
                }
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(selected.label, style = WheregoType.txTitle, color = colors.ink)
                    Text(
                        currencyMeta(selected.code),
                        style = WheregoType.helper,
                        color = colors.muted,
                    )
                }
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.teal),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CURRENCIES.filter { it.code != selected.code }.take(4).forEach { option ->
                Text(
                    option.code,
                    style = WheregoType.link,
                    color = colors.muted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(colors.sheet)
                        .border(BorderStroke(2.dp, colors.track), RoundedCornerShape(99.dp))
                        .clickable { onCurrency(option.code) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            Text(
                "Roughly how much is in your pocket and bank, together?",
                style = WheregoType.stepText,
                color = colors.muted,
            )
            BalanceField(
                symbol = selected.symbol,
                digits = digits,
                currency = selected.code,
                onDigits = onDigits,
            )
            Text(
                "A rough number is fine. Fix it anytime in Me → Adjust balance.",
                style = WheregoType.helper,
                color = colors.muted,
            )
        }
    }
}

private fun currencyMeta(code: String): String {
    val scale = CurrencyScale.scale(code)
    val decimals = if (scale == 0) "no decimals" else "$scale decimals"
    val example = MoneyFormatter.number(if (scale == 0) 1_250_000L else 125_000L, code)
    return "$code · $decimals · $example"
}

/**
 * Renders the raw minor-unit digits with locale grouping, e.g. `4250000` → `4.250.000`.
 * Both offset directions clamp to the end of the value: this field is typed and
 * backspaced as a whole amount, so the caret always belongs after the last digit —
 * which is exactly where `pencil-new.pen` → `Balance Field / Caret` draws it.
 */
private class GroupedAmount(private val currency: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val grouped = MoneyFormatter.number(DigitBuffer.amountMinor(text.text), currency)
        return TransformedText(
            AnnotatedString(grouped),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = grouped.length
                override fun transformedToOriginal(offset: Int): Int = text.text.length
            },
        )
    }
}

@Composable
private fun BalanceField(
    symbol: String,
    digits: String,
    currency: String,
    onDigits: (String) -> Unit,
) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.sheet)
            .border(BorderStroke(2.5.dp, colors.ink), shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(symbol, style = WheregoType.currencyPrefix.copy(fontSize = 20.sp), color = colors.muted)
        BasicTextField(
            value = digits,
            onValueChange = { raw ->
                onDigits(raw.filter { it.isDigit() }.take(DigitBuffer.MAX_DIGITS))
            },
            modifier = Modifier.weight(1f),
            textStyle = WheregoType.balanceValue.copy(color = colors.ink),
            singleLine = true,
            cursorBrush = SolidColor(colors.coral),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = GroupedAmount(currency),
        )
    }
}

@Composable
private fun CategoriesStep(
    packId: String,
    kept: Set<String>,
    onPack: (CategoryPack) -> Unit,
    onCustom: () -> Unit,
    onToggle: (String) -> Unit,
) {
    val colors = WheregoTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Pick your buckets", style = WheregoType.onboardTitle, color = colors.ink)
        Text(
            "Six is plenty to start. Tap to add or drop any.",
            style = WheregoType.stepText.copy(fontWeight = WheregoType.onboardSub.fontWeight),
            color = colors.muted,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(99.dp))
                .background(colors.chipIdle)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PresetCategories.packs.forEach { pack ->
                PackTab(
                    label = pack.label,
                    selected = packId == pack.id,
                    modifier = Modifier.weight(1f),
                    onClick = { onPack(pack) },
                )
            }
            PackTab(
                label = PresetCategories.CUSTOM_PACK_LABEL,
                selected = packId == PresetCategories.CUSTOM_PACK_ID,
                modifier = Modifier.weight(1f),
                onClick = onCustom,
            )
        }
        FlowRow(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            PresetCategories.expense.forEach { preset ->
                CategoryChip(
                    emoji = preset.emoji,
                    label = preset.name,
                    softHex = preset.softColorHex,
                    selected = preset.id in kept,
                    onClick = { onToggle(preset.id) },
                )
            }
        }
        Text(
            "Rename, recolor or archive them anytime in Me → Categories.",
            style = WheregoType.helper,
            color = colors.muted,
        )
    }
}

@Composable
private fun PackTab(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = WheregoTheme.colors
    Box(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) colors.ink else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = WheregoType.kindTab.copy(fontSize = 14.sp),
            color = if (selected) colors.paper else colors.muted,
        )
    }
}

@Composable
private fun CategoryChip(
    emoji: String,
    label: String,
    softHex: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(99.dp)
    Row(
        Modifier
            .clip(shape)
            .background(if (selected) colors.tealSoft else colors.sheet)
            .border(
                BorderStroke(if (selected) 2.5.dp else 2.dp, if (selected) colors.ink else colors.track),
                shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(emoji, fontSize = 15.sp, color = colors.ink)
        Text(label, style = WheregoType.stepText, color = if (selected) colors.ink else colors.muted)
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun FirstLogStep() {
    val colors = WheregoTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("One last thing.", style = WheregoType.onboardTitle, color = colors.ink)
            Text(
                "Log something from today. Even Rp 2.000 parking counts — the habit is the point, not the amount.",
                style = WheregoType.onboardSub,
                color = colors.muted,
            )
        }
        WheregoCard(
            cornerRadius = 30.dp,
            gap = 14.dp,
            strokeColor = colors.muted,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.height(200.dp),
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .wheregoHardShadow(shape = CircleShape, color = colors.shadow, offsetY = 5.dp)
                    .clip(CircleShape)
                    .background(colors.teal)
                    .border(BorderStroke(2.5.dp, colors.ink), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
            Text("Log your first spend", style = WheregoType.buttonLabel, color = colors.ink)
            Text("Takes about 20 seconds", style = WheregoType.link, color = colors.muted)
        }
    }
}
