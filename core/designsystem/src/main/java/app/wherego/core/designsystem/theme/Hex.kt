package app.wherego.core.designsystem.theme

import androidx.compose.ui.graphics.Color

fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLongOrNull(16) ?: return Color.Gray
    return when (cleaned.length) {
        6 -> Color(0xFF000000L or value)
        8 -> Color(value)
        else -> Color.Gray
    }
}
