package com.flla.wherego.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** The locale the UI is currently rendered in. */
@Composable
@ReadOnlyComposable
fun appLocale(): Locale {
    val locales = LocalConfiguration.current.locales
    return if (locales.isEmpty) Locale.getDefault() else locales[0]
}

@Composable
@ReadOnlyComposable
fun weekdayFull(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, appLocale())

@Composable
@ReadOnlyComposable
fun monthShort(ym: YearMonth): String = ym.month.getDisplayName(TextStyle.SHORT, appLocale())

/** `August` in the shown year, `August 2025` otherwise. */
@Composable
@ReadOnlyComposable
fun monthLabel(ym: YearMonth, current: YearMonth): String {
    val pattern = if (ym.year == current.year) "MMMM" else "MMMM yyyy"
    return ym.format(DateTimeFormatter.ofPattern(pattern, appLocale()))
}

@Composable
@ReadOnlyComposable
fun monthYear(ym: YearMonth): String =
    ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", appLocale()))

/** `Wed 6 Aug` — the Stories day-group header. */
@Composable
@ReadOnlyComposable
fun dayTitle(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE d MMM", appLocale()))
