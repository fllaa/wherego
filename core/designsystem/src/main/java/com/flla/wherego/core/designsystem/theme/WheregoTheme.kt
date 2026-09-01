package com.flla.wherego.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * The Material scheme exists for the framework components the app does not draw itself: text-field
 * cursors and indicators, menus, snackbars, ripples, the sheet scrim. Everything the design system
 * draws reads [WheregoColors] instead.
 *
 * Both schemes are built from their own palette instance. Reading light constants inside the dark
 * scheme is the failure this replaces: it put `#2157C7` primary and `#5A6A80` on-surface-variant on
 * a near-black surface (2.5:1 and 2.9:1).
 *
 * `surfaceTint` is transparent in both modes: depth here is a contour plus a hard slab, so M3's
 * tonal-elevation overlay would only muddy the surface ladder.
 */
private val LightScheme = with(WheregoColors.Light) {
    lightColorScheme(
        primary = teal,
        onPrimary = onAccent,
        primaryContainer = tealSoft,
        onPrimaryContainer = tealDeep,
        secondary = tealDeep,
        onSecondary = onAccent,
        secondaryContainer = tealSoft,
        onSecondaryContainer = tealDeep,
        tertiary = coral,
        onTertiary = onAlarm,
        background = paper,
        onBackground = ink,
        surface = white,
        onSurface = ink,
        surfaceVariant = chipIdle,
        onSurfaceVariant = muted,
        surfaceContainerLowest = white,
        surfaceContainerLow = noteChip,
        surfaceContainer = key,
        surfaceContainerHigh = chipIdle,
        surfaceContainerHighest = track,
        surfaceTint = Color.Transparent,
        inverseSurface = ink,
        inverseOnSurface = paper,
        outline = outline,
        outlineVariant = track,
        error = coral,
        onError = onAlarm,
        errorContainer = peach,
        onErrorContainer = ink,
        scrim = Color.Black,
    )
}

private val DarkScheme = with(WheregoColors.Dark) {
    darkColorScheme(
        primary = teal,
        onPrimary = onAccent,
        primaryContainer = tealSoft,
        onPrimaryContainer = tealDeep,
        secondary = tealDeep,
        onSecondary = outlineStrong,
        secondaryContainer = tealSoft,
        onSecondaryContainer = tealDeep,
        tertiary = coral,
        onTertiary = onAlarm,
        background = paper,
        onBackground = ink,
        surface = white,
        onSurface = ink,
        surfaceVariant = chipIdle,
        onSurfaceVariant = muted,
        surfaceContainerLowest = paper,
        surfaceContainerLow = noteChip,
        surfaceContainer = key,
        surfaceContainerHigh = chipIdle,
        surfaceContainerHighest = track,
        surfaceTint = Color.Transparent,
        inverseSurface = ink,
        inverseOnSurface = paper,
        outline = outline,
        outlineVariant = track,
        error = coral,
        onError = onAlarm,
        errorContainer = peach,
        onErrorContainer = ink,
        scrim = Color.Black,
    )
}

object WheregoTheme {
    val colors: WheregoColors
        @Composable
        @ReadOnlyComposable
        get() = LocalWheregoColors.current
}

@Composable
fun WheregoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) WheregoColors.Dark else WheregoColors.Light
    CompositionLocalProvider(LocalWheregoColors provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = WheregoType.typography(),
            shapes = WheregoShapes.shapes,
            content = content,
        )
    }
}
