package app.wherego.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = WheregoColors.Default.teal,
    onPrimary = Color.White,
    secondary = WheregoColors.Default.tealDeep,
    onSecondary = Color.White,
    background = WheregoColors.Default.paper,
    onBackground = WheregoColors.Default.ink,
    surface = WheregoColors.Default.white,
    onSurface = WheregoColors.Default.ink,
    surfaceVariant = WheregoColors.Default.chipIdle,
    onSurfaceVariant = WheregoColors.Default.muted,
    outline = WheregoColors.Default.ink,
    error = WheregoColors.Default.coral,
)

private val DarkScheme = darkColorScheme(
    primary = WheregoColors.Default.teal,
    onPrimary = WheregoColors.Default.ink,
    secondary = WheregoColors.Default.tealSoft,
    onSecondary = WheregoColors.Default.ink,
    background = WheregoColors.Default.darkPaper,
    onBackground = WheregoColors.Default.darkInk,
    surface = WheregoColors.Default.darkSurface,
    onSurface = WheregoColors.Default.darkInk,
    surfaceVariant = WheregoColors.Default.darkSurface,
    onSurfaceVariant = WheregoColors.Default.muted,
    outline = WheregoColors.Default.darkInk,
    error = WheregoColors.Default.coral,
)

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
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val palette = if (darkTheme) {
        WheregoColors.Default.copy(
            paper = WheregoColors.Default.darkPaper,
            ink = WheregoColors.Default.darkInk,
            white = WheregoColors.Default.darkSurface,
            sheet = WheregoColors.Default.darkSurface,
            chipIdle = WheregoColors.Default.darkSurface,
            key = WheregoColors.Default.darkSurface,
            mascotFill = WheregoColors.Default.darkSurface,
            noteChip = WheregoColors.Default.darkSurface,
        )
    } else {
        WheregoColors.Default
    }
    CompositionLocalProvider(LocalWheregoColors provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = WheregoType.typography(),
            shapes = WheregoShapes.shapes,
            content = content,
        )
    }
}
