package com.flla.wherego.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colour roles for the sticker-sheet look. Components consume roles, never raw hex, so a
 * mode swap is a single instance swap in [WheregoTheme].
 *
 * The light mode is the reference: a **dark contour** drawn around a **light fill**, with a solid
 * offset copy of the shape underneath ([shadow]). Three roles collapse onto one hex there
 * ([ink] == [outline] == [outlineStrong] == `#121826`), which is exactly why a mechanical
 * light→dark remap breaks: on a dark paper the text must go light while the contour must NOT.
 * They are separate roles here so each can move independently.
 *
 * ### Dark mode construction
 * Built in OKLCH at hue ~262 (the cobalt family), then chroma-clamped into sRGB. Every text pair
 * clears WCAG AA (worst case 4.65:1) and every contour clears 3:1 against `paper`/`card`.
 * The surface ladder mirrors the light mode's six steps away from the card plane, scaled 1.35x
 * because a dark surface has no ambient shading cue to help it separate.
 */
@Immutable
data class WheregoColors(
    // ── Surfaces, ascending distance from the card plane ────────────────────────────────────────
    /** App background. */
    val paper: Color = Color(0xFFF2F4F8),
    /** Card plane. `white` is the historical name; it is a *surface*, never an on-accent colour. */
    val white: Color = Color(0xFFFFFFFF),
    /** Bottom-sheet background; same plane as [white]. */
    val sheet: Color = Color(0xFFFFFFFF),
    /** Add-note chip fill: one step off the card. */
    val noteChip: Color = Color(0xFFF6F8FB),
    /** Numpad key fill. */
    val key: Color = Color(0xFFEEF2F7),
    /** Quick-amount / idle chip fill. */
    val chipIdle: Color = Color(0xFFE8EDF4),
    /** Go avatar + streak pill fill; carries a hint of accent tint. */
    val mascotFill: Color = Color(0xFFE2EAF8),
    /** Budget track, hairline borders, grabber. */
    val track: Color = Color(0xFFE1E7F0),
    /** Setting-row divider. */
    val divider: Color = Color(0xFFE8EDF4),

    // ── Content ─────────────────────────────────────────────────────────────────────────────────
    /** Primary text and icons. Text only: borders use [outline]. */
    val ink: Color = Color(0xFF121826),
    /** Secondary text, inactive icons. */
    val muted: Color = Color(0xFF5A6A80),

    // ── Contour and depth: the two cues that carry the whole look ───────────────────────────────
    /**
     * The 2.5dp drawn contour on paper/card/soft-tint surfaces. Darker than its fill in light mode,
     * lighter than its fill in dark mode; in both it reads as a deliberate stroke rather than a
     * glow, because it stays a mid-tone instead of racing to the opposite end of the ramp.
     */
    val outline: Color = Color(0xFF121826),
    /**
     * Contour on **bright** fills (accent buttons, the FAB, category colour swatches). Stays dark
     * in both modes, since those fills are light in both modes.
     */
    val outlineStrong: Color = Color(0xFF121826),
    /**
     * Hard-shadow slab: a solid, un-blurred, un-spread copy of the shape pushed straight down.
     * Equal to [outline] on purpose, so the slab reads as the contour thickened along the bottom.
     */
    val shadow: Color = Color(0xFF121826),

    // ── Accent: cobalt, split by role ───────────────────────────────────────────────────────────
    /** Accent **fill**: primary CTA, FAB, selected tab/chip, budget bar. Holds [onAccent] at AA. */
    val teal: Color = Color(0xFF2157C7),
    /** Accent **content**: links, accent icons, accent numerals on a paper/card surface. */
    val accentText: Color = Color(0xFF2157C7),
    /** Text/icons on a [teal] fill. */
    val onAccent: Color = Color(0xFFFFFFFF),
    /** Text on a [tealSoft] fill: the "left" amount, Today label, active tab label. */
    val tealDeep: Color = Color(0xFF163A8A),
    /** Soft accent fill: left-amount pill, emoji badge, idle category chip. */
    val tealSoft: Color = Color(0xFFD7E3F8),

    // ── Alarm ───────────────────────────────────────────────────────────────────────────────────
    /** Over-budget, flame, destructive icons, error. */
    val coral: Color = Color(0xFFE24B4B),
    /** Text/icons on a [coral] fill (swipe-to-delete reveal). */
    val onAlarm: Color = Color(0xFFFFFFFF),
    /** Soft alarm fill: more-spend pill, archive action. */
    val peach: Color = Color(0xFFF4D6D6),

    // ── Plan cap hero: a deep cobalt slab in both modes ──────────────────────────────────────────
    val capFill: Color = Color(0xFF163A8A),
    val capTrack: Color = Color(0xFF102A66),
    val capLabel: Color = Color(0xFFD7E3F8),

    // ── Legacy hue aliases. One accent plus one alarm; emoji and label carry category identity. ──
    val blue: Color = Color(0xFF2157C7),
    val blueSoft: Color = Color(0xFFD7E3F8),
    val green: Color = Color(0xFF2157C7),
    val greenSoft: Color = Color(0xFFD7E3F8),
    val onGreenSoft: Color = Color(0xFF163A8A),
    val violet: Color = Color(0xFF2157C7),
    val violetSoft: Color = Color(0xFFD7E3F8),
    val amber: Color = Color(0xFF2157C7),
    val amberSoft: Color = Color(0xFFD7E3F8),
    val pink: Color = Color(0xFFE24B4B),
    val pinkSoft: Color = Color(0xFFD7E3F8),
) {
    companion object {
        /** Cobalt on cool off-white. Defaults are the light mode. */
        val Light = WheregoColors()

        /** Cobalt on cool off-black. See the class doc for how these were derived. */
        val Dark = WheregoColors(
            // surfaces: L 15.0 → 35.4 in OKLCH, hue 262
            paper = Color(0xFF060B14),
            white = Color(0xFF1C232F),
            sheet = Color(0xFF1C232F),
            noteChip = Color(0xFF242A36),
            key = Color(0xFF29303C),
            chipIdle = Color(0xFF2E3643),
            mascotFill = Color(0xFF243450),
            track = Color(0xFF343C49),
            divider = Color(0xFF343C49),

            ink = Color(0xFFE5ECF5),
            muted = Color(0xFF9BA9BB),

            // contour lifts to a mid-slate; the slab follows it, as in light mode
            outline = Color(0xFF727B8A),
            outlineStrong = Color(0xFF010E32),
            shadow = Color(0xFF727B8A),

            // the accent splits: a deeper fill that still holds white text, a brighter content tone
            teal = Color(0xFF386FDC),
            accentText = Color(0xFF71A3FF),
            onAccent = Color(0xFFFFFFFF),
            tealDeep = Color(0xFF91B7FE),
            tealSoft = Color(0xFF25385D),

            coral = Color(0xFFFC746F),
            onAlarm = Color(0xFF240204),
            peach = Color(0xFF5B2126),

            capFill = Color(0xFF092866),
            capTrack = Color(0xFF000B34),
            capLabel = Color(0xFFA4BEEF),

            blue = Color(0xFF386FDC),
            blueSoft = Color(0xFF25385D),
            green = Color(0xFF386FDC),
            greenSoft = Color(0xFF25385D),
            onGreenSoft = Color(0xFF91B7FE),
            violet = Color(0xFF386FDC),
            violetSoft = Color(0xFF25385D),
            amber = Color(0xFF386FDC),
            amberSoft = Color(0xFF25385D),
            pink = Color(0xFFFC746F),
            pinkSoft = Color(0xFF25385D),
        )
    }
}

val LocalWheregoColors = staticCompositionLocalOf { WheregoColors.Light }
