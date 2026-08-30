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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flla.wherego.core.designsystem.theme.WheregoTheme

enum class GoMood { Idle, Happy, Sleepy }

@Composable
fun WheregoGoAvatar(
    modifier: Modifier = Modifier,
    mood: GoMood = GoMood.Idle,
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
            .size(54.dp)
            .border(2.5.dp, colors.ink, CircleShape)
            .clip(CircleShape)
            .background(fill)
            .semantics { contentDescription = goCd },
        contentAlignment = Alignment.Center,
    ) {
        when (mood) {
            GoMood.Idle -> {
                WheregoWaypointMark(modifier = Modifier.size(38.dp))
            }
            GoMood.Happy -> {
                Text("😄", fontSize = 26.sp)
            }
            GoMood.Sleepy -> {
                Text("😴", fontSize = 26.sp)
            }
        }
    }
}
