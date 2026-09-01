package com.flla.wherego.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flla.wherego.core.designsystem.component.ParkItButton
import com.flla.wherego.core.designsystem.component.WheregoCard
import com.flla.wherego.core.designsystem.component.wheregoHardShadow
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.designsystem.theme.parseHexColor
import com.flla.wherego.core.i18n.R
import com.flla.wherego.core.i18n.categoryDisplayName
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.CategoryKind
import com.flla.wherego.core.model.PresetCategories

enum class CategoryTab {
    EXPENSES,
    INCOME,
    VAULT,
}

sealed interface StickerSheetMode {
    data object None : StickerSheetMode
    data object New : StickerSheetMode
    data class Edit(val category: Category) : StickerSheetMode
}

private val EMOJI_PALETTE = listOf(
    "🍜", "🛒", "🚕", "🏠", "🎬", "🛍️",
    "💊", "🎁", "✨", "📄", "💼", "🛠️",
    "↩️", "☕", "🎮", "🐾", "✈️", "📚",
    "💻", "🎨", "⚡", "🏋️", "🍕", "🎵",
)

private val COLOR_SWATCHES = listOf(
    "#2157C7", // Cobalt
    "#E24B4B", // Coral
    "#F59E0B", // Amber
    "#10B981", // Emerald
    "#8B5CF6", // Violet
    "#F43F5E", // Rose
    "#06B6D4", // Cyan
    "#64748B", // Slate
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(
    categories: List<Category>,
    onBack: () -> Unit,
    onSave: (String, String, String, String, String?) -> Unit,
    onCreate: (String, String, String, String) -> Unit,
    onPin: (String) -> Unit,
    onArchive: (String, Boolean) -> Unit,
) {
    val colors = WheregoTheme.colors
    var currentTab by remember { mutableStateOf(CategoryTab.EXPENSES) }
    var sheetMode by remember { mutableStateOf<StickerSheetMode>(StickerSheetMode.None) }

    val activeExpenses = remember(categories) {
        categories.filter { !it.archived && it.kind == CategoryKind.EXPENSE }
    }
    val activeIncome = remember(categories) {
        categories.filter { !it.archived && it.kind == CategoryKind.INCOME }
    }
    val archivedList = remember(categories) {
        categories.filter { it.archived }
    }

    val displayedCategories = when (currentTab) {
        CategoryTab.EXPENSES -> activeExpenses
        CategoryTab.INCOME -> activeIncome
        CategoryTab.VAULT -> archivedList
    }

    // Top 6 active categories in each kind are currently in the quick capture bar
    val quickPinnedIds = remember(activeExpenses, activeIncome, currentTab) {
        when (currentTab) {
            CategoryTab.EXPENSES -> activeExpenses.take(6).map { it.id }.toSet()
            CategoryTab.INCOME -> activeIncome.take(6).map { it.id }.toSet()
            CategoryTab.VAULT -> emptySet()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding(),
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Squishy Back Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .wheregoHardShadow(cornerRadius = 14.dp, offsetY = 3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.white)
                        .border(BorderStroke(2.5.dp, colors.outline), RoundedCornerShape(14.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.me_categories_back),
                        tint = colors.ink,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Text(
                    text = stringResource(R.string.me_categories_back),
                    style = WheregoType.cardTitle,
                    color = colors.ink,
                )
            }

            // Mint Sticker Button
            Box(
                modifier = Modifier
                    .wheregoHardShadow(cornerRadius = 99.dp, offsetY = 3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(colors.teal)
                    .border(BorderStroke(2.5.dp, colors.outlineStrong), RoundedCornerShape(99.dp))
                    .clickable { sheetMode = StickerSheetMode.New }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        tint = colors.onAccent,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.me_category_new_title),
                        style = WheregoType.chip,
                        color = colors.onAccent,
                    )
                }
            }
        }

        // Segmented Tabs
        CategorySegmentedTabs(
            selectedTab = currentTab,
            expenseCount = activeExpenses.size,
            incomeCount = activeIncome.size,
            vaultCount = archivedList.size,
            onSelectTab = { currentTab = it },
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )

        // Sticker Album Page Content
        if (displayedCategories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                WheregoCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    padding = 24.dp,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (currentTab == CategoryTab.VAULT) "✨" else "📦",
                        style = WheregoType.wordmark.copy(fontSize = 44.sp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (currentTab == CategoryTab.VAULT) {
                            stringResource(R.string.me_category_empty_vault)
                        } else {
                            stringResource(R.string.stories_empty_bars)
                        },
                        style = WheregoType.onboardSub,
                        color = colors.muted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(displayedCategories, key = { it.id }) { cat ->
                    StickerTile(
                        category = cat,
                        isPinned = cat.id in quickPinnedIds,
                        onClick = { sheetMode = StickerSheetMode.Edit(cat) },
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet for Minting / Customizing Stickers
    val activeSheet = sheetMode
    if (activeSheet !is StickerSheetMode.None) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { sheetMode = StickerSheetMode.None },
            sheetState = sheetState,
            containerColor = colors.white,
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 42.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(colors.track),
                )
            },
        ) {
            StickerEditorSheetContent(
                mode = activeSheet,
                onDismiss = { sheetMode = StickerSheetMode.None },
                onSaveEdit = { id, name, emoji, color, kind ->
                    onSave(id, name, emoji, color, kind)
                    sheetMode = StickerSheetMode.None
                },
                onCreateNew = { name, emoji, color, kind ->
                    onCreate(name, emoji, color, kind)
                    sheetMode = StickerSheetMode.None
                },
                onPin = { id ->
                    onPin(id)
                    sheetMode = StickerSheetMode.None
                },
                onArchive = { id, archived ->
                    onArchive(id, archived)
                    sheetMode = StickerSheetMode.None
                },
            )
        }
    }
}

@Composable
private fun CategorySegmentedTabs(
    selectedTab: CategoryTab,
    expenseCount: Int,
    incomeCount: Int,
    vaultCount: Int,
    onSelectTab: (CategoryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wheregoHardShadow(cornerRadius = 20.dp, offsetY = 3.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.white)
            .border(BorderStroke(2.5.dp, colors.outline), RoundedCornerShape(20.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TabPill(
            label = stringResource(R.string.me_category_tab_expense),
            count = expenseCount,
            selected = selectedTab == CategoryTab.EXPENSES,
            onClick = { onSelectTab(CategoryTab.EXPENSES) },
            modifier = Modifier.weight(1f),
        )
        TabPill(
            label = stringResource(R.string.me_category_tab_income),
            count = incomeCount,
            selected = selectedTab == CategoryTab.INCOME,
            onClick = { onSelectTab(CategoryTab.INCOME) },
            modifier = Modifier.weight(1f),
        )
        TabPill(
            label = stringResource(R.string.me_category_tab_vault),
            count = vaultCount,
            selected = selectedTab == CategoryTab.VAULT,
            onClick = { onSelectTab(CategoryTab.VAULT) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TabPill(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    val fill by animateColorAsState(
        targetValue = if (selected) colors.teal else Color.Transparent,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "tabFill",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) colors.onAccent else colors.muted,
        animationSpec = tween(180),
        label = "tabText",
    )

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fill)
            .then(
                if (selected) {
                    Modifier.border(BorderStroke(2.dp, colors.outlineStrong), RoundedCornerShape(16.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = WheregoType.chip,
                color = textColor,
            )
            if (count > 0) {
                Text(
                    text = "($count)",
                    style = WheregoType.meta,
                    color = textColor.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun StickerTile(
    category: Category,
    isPinned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "stickerScale",
    )

    val backgroundTint = remember(category.id, category.colorHex) {
        val baseColor = parseHexColor(category.colorHex)
        if (category.softColorHex.isNotBlank() && category.softColorHex != PresetCategories.ACCENT_SOFT_HEX) {
            parseHexColor(category.softColorHex)
        } else {
            baseColor.copy(alpha = 0.14f)
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth()
            .wheregoHardShadow(cornerRadius = 22.dp, offsetY = 4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.white)
            .border(BorderStroke(2.5.dp, colors.outline), RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(14.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // Sticker Badge Circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(backgroundTint)
                        .border(BorderStroke(2.dp, colors.outline), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = category.emoji,
                        fontSize = 24.sp,
                    )
                }

                // Quick Bar Pinned Badge
                if (isPinned && !category.archived) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(colors.mascotFill)
                            .border(BorderStroke(1.5.dp, colors.outline), RoundedCornerShape(99.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.me_category_pinned_badge),
                            style = WheregoType.meta.copy(fontSize = 10.sp),
                            color = colors.tealDeep,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = categoryDisplayName(category.id, category.name),
                    style = WheregoType.buttonLabel.copy(fontSize = 15.sp),
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (category.archived) {
                        stringResource(R.string.me_category_archived)
                    } else if (category.kind == CategoryKind.INCOME) {
                        stringResource(R.string.kind_income)
                    } else {
                        stringResource(R.string.kind_expense)
                    },
                    style = WheregoType.meta,
                    color = if (category.archived) colors.coral else colors.muted,
                )
            }
        }
    }
}

@Composable
private fun StickerEditorSheetContent(
    mode: StickerSheetMode,
    onDismiss: () -> Unit,
    onSaveEdit: (String, String, String, String, String) -> Unit,
    onCreateNew: (String, String, String, String) -> Unit,
    onPin: (String) -> Unit,
    onArchive: (String, Boolean) -> Unit,
) {
    val colors = WheregoTheme.colors
    val existing = (mode as? StickerSheetMode.Edit)?.category
    val resolvedName = if (existing != null) categoryDisplayName(existing.id, existing.name) else ""

    var name by remember { mutableStateOf(resolvedName) }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "✨") }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: COLOR_SWATCHES.first()) }
    var kind by remember { mutableStateOf(existing?.kind ?: CategoryKind.EXPENSE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Title
        Text(
            text = if (existing != null) {
                stringResource(R.string.me_category_edit_sticker)
            } else {
                stringResource(R.string.me_category_new_title)
            },
            style = WheregoType.cardTitle,
            color = colors.ink,
        )

        // Live Sticker Preview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wheregoHardShadow(cornerRadius = 24.dp, offsetY = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.paper)
                .border(BorderStroke(2.5.dp, colors.outline), RoundedCornerShape(24.dp))
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val previewTint = parseHexColor(colorHex).copy(alpha = 0.18f)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(previewTint)
                        .border(BorderStroke(2.5.dp, colors.outline), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = emoji, fontSize = 32.sp)
                }

                Column {
                    Text(
                        text = name.ifBlank { stringResource(R.string.me_category_new_title) },
                        style = WheregoType.cardTitle,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (kind == CategoryKind.INCOME) {
                            stringResource(R.string.kind_income)
                        } else {
                            stringResource(R.string.kind_expense)
                        },
                        style = WheregoType.meta,
                        color = colors.muted,
                    )
                }
            }
        }

        // Name Input
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.me_field_name),
                style = WheregoType.chip,
                color = colors.ink,
            )
            BasicTextField(
                value = name,
                onValueChange = { name = it.take(24) },
                singleLine = true,
                textStyle = WheregoType.buttonLabel.copy(color = colors.ink, fontSize = 16.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.paper)
                    .border(BorderStroke(2.dp, colors.outline), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }

        // Category Kind Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.me_category_type),
                style = WheregoType.chip,
                color = colors.ink,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.paper)
                    .border(BorderStroke(2.dp, colors.outline), RoundedCornerShape(18.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TabPill(
                    label = stringResource(R.string.kind_expense),
                    count = 0,
                    selected = kind == CategoryKind.EXPENSE,
                    onClick = { kind = CategoryKind.EXPENSE },
                    modifier = Modifier.weight(1f),
                )
                TabPill(
                    label = stringResource(R.string.kind_income),
                    count = 0,
                    selected = kind == CategoryKind.INCOME,
                    onClick = { kind = CategoryKind.INCOME },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Emoji Palette Drawer
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.me_category_choose_emoji),
                style = WheregoType.chip,
                color = colors.ink,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.paper)
                    .border(BorderStroke(2.dp, colors.outline), RoundedCornerShape(18.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(EMOJI_PALETTE) { em ->
                    val selected = em == emoji
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) colors.mascotFill else Color.Transparent)
                            .then(
                                if (selected) {
                                    Modifier.border(BorderStroke(2.dp, colors.outline), RoundedCornerShape(12.dp))
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { emoji = em },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = em, fontSize = 20.sp)
                    }
                }
            }
        }

        // Color Palette Swatches
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.me_category_color_palette),
                style = WheregoType.chip,
                color = colors.ink,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                COLOR_SWATCHES.forEach { hex ->
                    val color = parseHexColor(hex)
                    val isSelected = hex.equals(colorHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                BorderStroke(if (isSelected) 2.5.dp else 1.5.dp, if (isSelected) colors.outlineStrong else colors.track),
                                CircleShape,
                            )
                            .clickable { colorHex = hex },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = colors.onAccent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        // Pin to quick bar and Archive actions (for existing categories)
        if (existing != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Pin button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.mascotFill)
                        .border(BorderStroke(2.dp, colors.outline), RoundedCornerShape(14.dp))
                        .clickable { onPin(existing.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Outlined.PushPin,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.me_category_pin_quick),
                            style = WheregoType.chip,
                            color = colors.ink,
                        )
                    }
                }

                // Archive / Unarchive button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (existing.archived) colors.tealSoft else colors.peach)
                        .border(BorderStroke(2.dp, colors.outline), RoundedCornerShape(14.dp))
                        .clickable { onArchive(existing.id, !existing.archived) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (existing.archived) {
                            stringResource(R.string.me_unarchive)
                        } else {
                            stringResource(R.string.me_archive)
                        },
                        style = WheregoType.chip,
                        color = if (existing.archived) colors.ink else colors.coral,
                    )
                }
            }
        }

        // Save / Park It CTA
        ParkItButton(
            enabled = name.isNotBlank(),
            onClick = {
                if (existing != null) {
                    onSaveEdit(existing.id, name, emoji, colorHex, kind)
                } else {
                    onCreateNew(name, emoji, colorHex, kind)
                }
            },
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
    }
}
