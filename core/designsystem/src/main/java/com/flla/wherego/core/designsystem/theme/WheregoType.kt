package com.flla.wherego.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.flla.wherego.core.designsystem.R

object WheregoType {
    private val display = FontFamily(
        Font(R.font.fredoka_medium, FontWeight.Medium),
        Font(R.font.fredoka_semibold, FontWeight.SemiBold),
    )
    private val ui = FontFamily(
        Font(R.font.nunito_sans_semibold, FontWeight.SemiBold),
        Font(R.font.nunito_sans_bold, FontWeight.Bold),
        Font(R.font.nunito_sans_extrabold, FontWeight.ExtraBold),
    )

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
        fontFamily = ui,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
    )
    val kindTab = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    )
    val amountHuge = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 52.sp,
    )
    val currencyPrefix = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
    )
    val key = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
    )
    val cta = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
    )
    val txTitle = TextStyle(
        fontFamily = ui,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
    )
    val txAmount = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    )
    val chip = TextStyle(
        fontFamily = ui,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
    )
    val link = TextStyle(
        fontFamily = ui,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
    )
    val leftPill = TextStyle(
        fontFamily = ui,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
    )
    val streakNum = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    )

    fun typography(): Typography = Typography(
        headlineLarge = heroAmount,
        headlineSmall = greeting,
        titleLarge = cardTitle,
        bodyMedium = meta,
        labelLarge = kindTab,
        labelMedium = eyebrow,
    )
}
