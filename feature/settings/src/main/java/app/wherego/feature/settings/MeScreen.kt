package app.wherego.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.wherego.core.designsystem.theme.WheregoTheme
import app.wherego.core.designsystem.theme.WheregoType

@Composable
fun MeScreen(modifier: Modifier = Modifier) {
    val colors = WheregoTheme.colors
    Box(
        modifier
            .fillMaxSize()
            .background(colors.paper),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Me", style = WheregoType.cardTitle, color = colors.ink)
            Spacer(Modifier.height(8.dp))
            Text("Guest · offline", style = WheregoType.meta, color = colors.muted)
        }
    }
}
