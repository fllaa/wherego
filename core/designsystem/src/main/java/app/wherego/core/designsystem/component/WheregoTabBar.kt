package app.wherego.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.wherego.core.designsystem.theme.WheregoTheme
import app.wherego.core.designsystem.theme.WheregoType

enum class WheregoTab(
    val label: String,
    val icon: ImageVector,
) {
    Home("Home", Icons.Outlined.Home),
    Stories("Stories", Icons.AutoMirrored.Outlined.MenuBook),
    Plan("Plan", Icons.Outlined.DateRange),
    Me("Me", Icons.Outlined.Person),
}

@Composable
fun WheregoTabBar(
    selected: WheregoTab,
    onSelect: (WheregoTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WheregoTab.entries.forEach { tab ->
            val active = tab == selected
            Column(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    modifier = Modifier.size(22.dp),
                    tint = if (active) colors.tealDeep else colors.muted,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    style = WheregoType.tabLabel,
                    color = if (active) colors.tealDeep else colors.muted,
                )
            }
        }
    }
}
