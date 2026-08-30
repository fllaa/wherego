package com.flla.wherego.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.flla.wherego.core.i18n.R
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType

private val DockShape = RoundedCornerShape(24.dp)

enum class WheregoTab(
    @StringRes val labelRes: Int,
    val outlined: ImageVector,
    val filled: ImageVector,
) {
    Home(R.string.ds_tab_home, Icons.Outlined.Home, Icons.Filled.Home),
    Stories(R.string.ds_tab_stories, Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    Plan(R.string.ds_tab_plan, Icons.Outlined.DateRange, Icons.Filled.DateRange),
    Me(R.string.ds_tab_me, Icons.Outlined.Person, Icons.Filled.Person),
}

@Composable
fun WheregoTabBar(
    selected: WheregoTab,
    onSelect: (WheregoTab) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .background(colors.paper)
            .navigationBarsPadding(),
    ) {
        Box(
            Modifier
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)
                .fillMaxWidth()
                .height(58.dp)
                .wheregoHardShadow(shape = DockShape, color = colors.shadow, offsetY = 4.dp)
                .clip(DockShape)
                .background(colors.sheet)
                .border(BorderStroke(2.5.dp, colors.ink), DockShape),
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabItem(WheregoTab.Home, selected, onSelect)
                TabItem(WheregoTab.Stories, selected, onSelect)
                Spacer(Modifier.width(64.dp))
                TabItem(WheregoTab.Plan, selected, onSelect)
                TabItem(WheregoTab.Me, selected, onSelect)
            }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-10).dp)
                .size(64.dp)
                .wheregoHardShadow(shape = CircleShape, color = colors.shadow, offsetY = 4.dp)
                .border(2.5.dp, colors.ink, CircleShape)
                .clip(CircleShape)
                .background(colors.teal)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = stringResource(R.string.ds_cd_add),
                tint = colors.white,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun TabItem(
    tab: WheregoTab,
    selected: WheregoTab,
    onSelect: (WheregoTab) -> Unit,
) {
    val colors = WheregoTheme.colors
    val active = tab == selected
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(80),
        label = "tabPress",
    )
    val badgeFill by animateColorAsState(
        targetValue = if (active) colors.tealSoft else colors.sheet,
        animationSpec = tween(160),
        label = "tabBadge",
    )
    val iconTint by animateColorAsState(
        targetValue = if (active) colors.tealDeep else colors.muted,
        animationSpec = tween(160),
        label = "tabIcon",
    )
    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = { onSelect(tab) },
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(badgeFill)
                .then(
                    if (active) Modifier.border(BorderStroke(2.dp, colors.ink), CircleShape)
                    else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (active) tab.filled else tab.outlined,
                contentDescription = stringResource(tab.labelRes),
                modifier = Modifier.size(18.dp),
                tint = iconTint,
            )
        }
        Text(
            text = stringResource(tab.labelRes),
            style = WheregoType.streakNum.copy(fontSize = 11.sp),
            color = iconTint,
        )
    }
}
