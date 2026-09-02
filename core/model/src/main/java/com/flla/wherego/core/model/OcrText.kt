package com.flla.wherego.core.model

import kotlin.math.abs

/**
 * One recognized line and where it sat on the image.
 *
 * A line with no box — the recognizer occasionally returns none — has [top] equal to [bottom] and
 * is reported as not [positioned].
 */
data class OcrLine(
    val text: String,
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
) {
    val height: Int get() = (bottom - top).coerceAtLeast(0)

    val centerY: Int get() = (top + bottom) / 2

    val positioned: Boolean get() = bottom > top
}

/**
 * What OCR read, with the geometry kept.
 *
 * Keeping it is the whole point. ML Kit walks a two-column slip **column-first**, so a bank's
 *
 * ```
 * Nominal Transfer          Rp 125.000
 * Sisa Saldo              Rp 1.847.300
 * ```
 *
 * arrives as every label followed by every value, and any rule that reads a label as owning the
 * value beside it has nothing left to hold on to — which is how a Rp 125.000 transfer once parsed
 * as the Rp 1.847.300 balance sitting under it. [rows] puts the columns back together.
 */
data class OcrText(val lines: List<OcrLine>) {
    /** Exactly what the recognizer concatenated. This is what gets persisted, for auditing. */
    val raw: String get() = lines.joinToString("\n") { it.text }

    /**
     * Lines regrouped by vertical position: everything sharing a horizontal band becomes one line,
     * ordered left to right, so a label and its value end up adjacent again.
     *
     * Falls back to [raw] ordering when there is nothing to regroup. Lines the recognizer gave no
     * box for cannot be placed, so they are appended rather than dropped — losing text would be a
     * worse failure than mis-ordering it.
     */
    fun rows(): String {
        val placed = lines.filter { it.positioned && it.text.isNotBlank() }
        if (placed.size < 2) return raw
        val loose = lines.filter { !it.positioned && it.text.isNotBlank() }

        val bands = mutableListOf<MutableList<OcrLine>>()
        for (line in placed.sortedBy { it.centerY }) {
            val open = bands.lastOrNull()
            if (open != null && open.any { sharesBand(it, line) }) open += line else bands += mutableListOf(line)
        }

        val rows = bands.map { band -> band.sortedBy { it.left }.joinToString("  ") { it.text } }
        return (rows + loose.map { it.text }).joinToString("\n")
    }

    /**
     * Two lines belong to the same row when their vertical centres are closer than most of a line
     * height. A label is often set a size or two smaller than the value it sits beside, so the
     * smaller of the two heights sets the tolerance; comparing spans instead would split the row.
     */
    private fun sharesBand(a: OcrLine, b: OcrLine): Boolean =
        abs(a.centerY - b.centerY) <= minOf(a.height, b.height) * 3 / 5

    companion object {
        val EMPTY = OcrText(emptyList())

        /**
         * Text with no geometry — one line per newline. [rows] then returns it untouched, which is
         * what the flat-text callers and the test double want.
         */
        fun of(text: String): OcrText =
            OcrText(text.lineSequence().map { OcrLine(it.trim()) }.toList())
    }
}
