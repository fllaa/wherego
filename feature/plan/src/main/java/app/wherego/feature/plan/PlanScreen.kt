package app.wherego.feature.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.wherego.core.designsystem.theme.WheregoTheme
import app.wherego.core.designsystem.theme.WheregoType

@Composable
fun PlanScreen(modifier: Modifier = Modifier) {
    val colors = WheregoTheme.colors
    Box(
        modifier
            .fillMaxSize()
            .background(colors.paper),
        contentAlignment = Alignment.Center,
    ) {
        Text("Soon", style = WheregoType.cardTitle, color = colors.muted)
    }
}
