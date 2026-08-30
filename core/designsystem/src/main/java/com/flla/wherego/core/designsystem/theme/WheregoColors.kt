package com.flla.wherego.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class WheregoColors(
    val paper: Color = Color(0xFFF2F4F8),
    val ink: Color = Color(0xFF121826),
    val muted: Color = Color(0xFF5A6A80),
    val white: Color = Color(0xFFFFFFFF),
    val mascotFill: Color = Color(0xFFE2EAF8),
    val teal: Color = Color(0xFF2157C7),
    val tealDeep: Color = Color(0xFF163A8A),
    val tealSoft: Color = Color(0xFFD7E3F8),
    val coral: Color = Color(0xFFE24B4B),
    val peach: Color = Color(0xFFF4D6D6),
    val blue: Color = Color(0xFF2157C7),
    val blueSoft: Color = Color(0xFFD7E3F8),
    val greenSoft: Color = Color(0xFFD7E3F8),
    val violet: Color = Color(0xFF2157C7),
    val violetSoft: Color = Color(0xFFD7E3F8),
    val pinkSoft: Color = Color(0xFFD7E3F8),
    val track: Color = Color(0xFFE1E7F0),
    val chipIdle: Color = Color(0xFFE8EDF4),
    val key: Color = Color(0xFFEEF2F7),
    val noteChip: Color = Color(0xFFF6F8FB),
    val sheet: Color = Color(0xFFFFFFFF),
    val amber: Color = Color(0xFF2157C7),
    val amberSoft: Color = Color(0xFFD7E3F8),
    val green: Color = Color(0xFF2157C7),
    val pink: Color = Color(0xFFE24B4B),
    val onGreenSoft: Color = Color(0xFF163A8A),
    val divider: Color = Color(0xFFE8EDF4),
    val capFill: Color = Color(0xFF163A8A),
    val capTrack: Color = Color(0xFF102A66),
    val capLabel: Color = Color(0xFFD7E3F8),
    val shadow: Color = Color(0xFF121826),
    val darkShadow: Color = Color(0xFF070A10),
    val darkPaper: Color = Color(0xFF10141C),
    val darkSurface: Color = Color(0xFF1A2230),
    val darkInk: Color = Color(0xFFE8EEF6),
    val darkMuted: Color = Color(0xFF8B9BB0),
    val darkTrack: Color = Color(0xFF2A3444),
    val darkTealDeep: Color = Color(0xFF8FB0FF),
) {
    companion object {
        val Default = WheregoColors()
    }
}

val LocalWheregoColors = staticCompositionLocalOf { WheregoColors.Default }
