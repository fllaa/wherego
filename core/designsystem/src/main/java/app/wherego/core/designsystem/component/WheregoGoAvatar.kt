package app.wherego.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wherego.core.designsystem.theme.WheregoTheme

enum class GoMood { Idle, Happy, Sleepy }

@Composable
fun WheregoGoAvatar(
    modifier: Modifier = Modifier,
    mood: GoMood = GoMood.Idle,
) {
    val colors = WheregoTheme.colors
    val fill = when (mood) {
        GoMood.Idle -> colors.mascotFill
        GoMood.Happy -> colors.tealSoft
        GoMood.Sleepy -> colors.track
    }
    val face = when (mood) {
        GoMood.Idle -> "🪙"
        GoMood.Happy -> "😄"
        GoMood.Sleepy -> "😴"
    }
    Box(
        modifier
            .size(40.dp)
            .border(2.5.dp, colors.ink, CircleShape)
            .clip(CircleShape)
            .background(fill)
            .semantics { contentDescription = "Go" },
        contentAlignment = Alignment.Center,
    ) {
        Text(face, fontSize = 18.sp)
    }
}
