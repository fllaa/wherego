package com.flla.wherego.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.flla.wherego.core.model.MoneyFormatter

/**
 * Whether `Me → Hide amounts` is on for this device.
 *
 * A composition local rather than a field on each screen's ui state: amounts are rendered from
 * five feature modules, and a flag threaded through five view models is a flag one of them
 * forgets. `WheregoTheme` provides it once at the root, the way the palette is provided.
 */
val LocalAmountsHidden = staticCompositionLocalOf { false }

/**
 * The amount to actually render: [label] as formatted, or bullets while amounts are hidden.
 *
 * Call this at the point of display and pass only the money itself — for a sentence like
 * `Rp 190rb left`, wrap the amount argument, not the finished sentence, so the wording survives.
 *
 * Deliberately *not* applied inside [MoneyFormatter]: the same formatter also writes the CSV and
 * PDF exports and drives the amount the user is currently typing, and none of those may be
 * masked. Masking is a property of a display, not of a number.
 */
@Composable
@ReadOnlyComposable
fun displayAmount(label: String): String =
    if (LocalAmountsHidden.current) MoneyFormatter.HIDDEN else label
