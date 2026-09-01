package com.flla.wherego.core.designsystem.component

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
import androidx.compose.ui.res.stringResource
import com.flla.wherego.core.i18n.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flla.wherego.core.designsystem.theme.WheregoTheme

enum class GoMood { Idle, Happy, Sleepy }

/**
 * Go, the app's face. [size] defaults to the 54dp Home uses; the lock gate sizes him up, and the
 * inner mark and emoji scale with him so the proportions hold at any size.
 */
@Composable
fun WheregoGoAvatar(
    modifier: Modifier = Modifier,
    mood: GoMood = GoMood.Idle,
    size: Dp = 54.dp,
) {
    val goCd = stringResource(R.string.ds_cd_go)
    val colors = WheregoTheme.colors
    val fill = when (mood) {
        GoMood.Idle -> colors.mascotFill
        GoMood.Happy -> colors.tealSoft
        GoMood.Sleepy -> colors.track
    }
    Box(
        modifier
            .size(size)
            .border(2.5.dp, colors.outline, CircleShape)
            .clip(CircleShape)
            .background(fill)
            .semantics { contentDescription = goCd },
        contentAlignment = Alignment.Center,
    ) {
        when (mood) {
            GoMood.Idle -> WheregoWaypointMark(modifier = Modifier.size(size * 0.70f))
            GoMood.Happy -> Text("😄", fontSize = (size.value * 0.48f).sp)
            GoMood.Sleepy -> Text("😴", fontSize = (size.value * 0.48f).sp)
        }
    }
}
