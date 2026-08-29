package app.wherego.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.wherego.core.designsystem.theme.WheregoTheme

@Composable
fun WheregoGoAvatar(modifier: Modifier = Modifier) {
    val colors = WheregoTheme.colors
    Box(
        modifier
            .size(40.dp)
            .border(2.5.dp, colors.ink, CircleShape)
            .clip(CircleShape)
            .background(colors.mascotFill)
            .semantics { contentDescription = "Go" },
    )
}
