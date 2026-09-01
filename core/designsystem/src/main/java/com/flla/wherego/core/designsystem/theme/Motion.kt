package com.flla.wherego.core.designsystem.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Whether this device has asked for animation to be taken away.
 *
 * Android has no `prefers-reduced-motion`. The platform signal is the animator duration scale,
 * which is what both `Accessibility → Remove animations` and the developer options write to, so a
 * value of zero is the user saying they do not want movement.
 *
 * A composition local, not a helper each component calls, for the same reason `LocalAmountsHidden`
 * is one: reading it costs a binder round trip to the settings provider, and the keypad alone would
 * make that call ten times per frame if every key resolved it itself. [WheregoTheme] resolves it
 * once and provides it here.
 *
 * Haptics are deliberately **not** gated on this. A user who turns off animation has not asked to
 * stop feeling their own keypresses, and the OS has a separate switch for that.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Resolves the platform animation scale. Called once by [WheregoTheme]; everything else reads
 * [LocalReducedMotion].
 *
 * Read once per resolver on purpose: changing the setting restarts activities, so there is nothing
 * to observe continuously.
 */
@Composable
internal fun resolveReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        runCatching {
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}
