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
    val wordmark = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 42.sp,
    )
    val onboardTitle = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 37.sp,
    )
    val onboardTitleLarge = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 39.sp,
    )
    val onboardSub = TextStyle(
        fontFamily = ui,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )
    val stepText = TextStyle(
        fontFamily = ui,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    )
    val helper = TextStyle(
        fontFamily = ui,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    )
    val balanceValue = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
    )
    val buttonLabel = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
    )
    val pageTitle = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
    )
    val monthLabel = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    )
    val groupLabel = TextStyle(
        fontFamily = ui,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        letterSpacing = 1.1.sp,
    )
    val settingLabel = TextStyle(
        fontFamily = ui,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
    )
    val statValue = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    )
    val statLabel = TextStyle(
        fontFamily = ui,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
    )
    val meterDetail = TextStyle(
        fontFamily = ui,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
    )
    val barAmount = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
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
