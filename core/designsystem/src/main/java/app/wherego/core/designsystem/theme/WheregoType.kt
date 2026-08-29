package app.wherego.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object WheregoType {
    // S0: platform sans. Fredoka + Nunito Sans bundle in S2 with mascot art.
    private val display = FontFamily.SansSerif
    private val ui = FontFamily.SansSerif

    val greeting = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
    )
    val meta = TextStyle(
        fontFamily = ui,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
    )
    val eyebrow = TextStyle(
        fontFamily = ui,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.6.sp,
    )
    val heroAmount = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        lineHeight = 53.sp,
    )
    val cardTitle = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
    )
    val tabLabel = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
    )

    fun typography(): Typography = Typography(
        headlineLarge = heroAmount,
        headlineSmall = greeting,
        titleLarge = cardTitle,
        bodyMedium = meta,
        labelLarge = tabLabel,
        labelMedium = eyebrow,
    )
}
