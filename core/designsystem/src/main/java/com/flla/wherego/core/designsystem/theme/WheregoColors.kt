package com.flla.wherego.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class WheregoColors(
    val paper: Color = Color(0xFFFFF3E2),
    val ink: Color = Color(0xFF0F2E2C),
    val muted: Color = Color(0xFF52706D),
    val white: Color = Color(0xFFFFFFFF),
    val mascotFill: Color = Color(0xFFFFEECC),
    val teal: Color = Color(0xFF10B5A0),
    val tealDeep: Color = Color(0xFF076358),
    val tealSoft: Color = Color(0xFFD5F4EE),
    val coral: Color = Color(0xFFFF6B4A),
    val peach: Color = Color(0xFFFFE1D8),
    val blue: Color = Color(0xFF4CA8FF),
    val blueSoft: Color = Color(0xFFDBECFF),
    val greenSoft: Color = Color(0xFFDAF6E9),
    val violet: Color = Color(0xFF8B7CF6),
    val violetSoft: Color = Color(0xFFE7E3FE),
    val pinkSoft: Color = Color(0xFFFFDFEC),
    val track: Color = Color(0xFFEDE4D5),
    val chipIdle: Color = Color(0xFFF5EFE4),
    val key: Color = Color(0xFFF7F2E8),
    val noteChip: Color = Color(0xFFFBF7F0),
    val sheet: Color = Color(0xFFFFFFFF),
    val amber: Color = Color(0xFFFFB020),
    val amberSoft: Color = Color(0xFFFFEECC),
    val green: Color = Color(0xFF3FC98A),
    val pink: Color = Color(0xFFFF5FA2),
    val onGreenSoft: Color = Color(0xFF0E6B46),
    val divider: Color = Color(0xFFF2EADC),
    val capFill: Color = Color(0xFF0A6F62),
    val capTrack: Color = Color(0xFF085147),
    val capLabel: Color = Color(0xFFC7EFE8),
    val shadow: Color = Color(0xFF0F2E2C),
    val darkShadow: Color = Color(0xFF080D0C),
    val darkPaper: Color = Color(0xFF141C1B),
    val darkSurface: Color = Color(0xFF1E2A28),
    val darkInk: Color = Color(0xFFF2EFE7),
    val darkMuted: Color = Color(0xFF9DB3AF),
    val darkTrack: Color = Color(0xFF2E3B39),
    val darkTealDeep: Color = Color(0xFF8FE8D8),
) {
    companion object {
        val Default = WheregoColors()
    }
}

val LocalWheregoColors = staticCompositionLocalOf { WheregoColors.Default }
