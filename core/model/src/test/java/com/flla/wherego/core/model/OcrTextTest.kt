package com.flla.wherego.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrTextTest {
    /**
     * A label and the value beside it, as a two-column slip lays them out: labels down the left at
     * one size, values down the right at another, sharing a baseline band.
     */
    private fun row(top: Int, label: String, value: String) = listOf(
        OcrLine(text = label, left = 40, top = top, right = 400, bottom = top + 50),
        OcrLine(text = value, left = 700, top = top, right = 1040, bottom = top + 55),
    )

    /**
     * ML Kit emits blocks, and on this layout a block is a whole column — so every label arrives
     * before any value. Reading in that order is what made a Rp 125.000 transfer parse as the
     * Rp 1.847.300 balance: the labels that told them apart were nine lines away from their values.
     */
    private val columnFirstSlip: List<OcrLine> = buildList {
        val rows = listOf(
            300 to ("Nominal Transfer" to "Rp 125.000"),
            400 to ("Total" to "Rp 127.500"),
            500 to ("Nama Penerima" to "BUDI SANTOSO"),
            600 to ("Sisa Saldo" to "Rp 1.847.300"),
        )
        // Labels first, then values: the recognizer's order, not the page's.
        rows.forEach { (top, pair) -> add(row(top, pair.first, pair.second)[0]) }
        rows.forEach { (top, pair) -> add(row(top, pair.first, pair.second)[1]) }
    }

    @Test
    fun columnsAreLaidBackOutIntoRows() {
        val rows = OcrText(columnFirstSlip).rows().lines()

        assertEquals(
            listOf(
                "Nominal Transfer  Rp 125.000",
                "Total  Rp 127.500",
                "Nama Penerima  BUDI SANTOSO",
                "Sisa Saldo  Rp 1.847.300",
            ),
            rows,
        )
    }

    @Test
    fun rawKeepsTheRecognizersOwnOrderForAuditing() {
        val raw = OcrText(columnFirstSlip).raw.lines()

        assertEquals("Nominal Transfer", raw.first())
        assertEquals("Rp 1.847.300", raw.last())
    }

    /** A value on its own baseline is its own row; only a shared band merges. */
    @Test
    fun aValueTooFarDownStartsItsOwnRow() {
        val text = OcrText(
            listOf(
                OcrLine("Total", left = 40, top = 300, right = 200, bottom = 350),
                OcrLine("Rp 127.500", left = 700, top = 500, right = 1040, bottom = 550),
            ),
        )

        assertEquals(listOf("Total", "Rp 127.500"), text.rows().lines())
    }

    /** Without boxes there is nothing to regroup, so the order stands as read. */
    @Test
    fun unpositionedTextIsLeftAlone() {
        assertEquals("Total\nRp 10.000", OcrText.of("Total\nRp 10.000").rows())
    }

    /**
     * A missing box must not cost the text. The recognizer returns none now and then, and dropping
     * the line would quietly lose an amount.
     */
    @Test
    fun aLineWithNoBoxIsKeptRatherThanDropped() {
        val text = OcrText(
            listOf(
                OcrLine("Total", left = 40, top = 300, right = 200, bottom = 350),
                OcrLine("Rp 127.500", left = 700, top = 300, right = 1040, bottom = 355),
                OcrLine("Catatan tanpa kotak"),
            ),
        )

        assertEquals(listOf("Total  Rp 127.500", "Catatan tanpa kotak"), text.rows().lines())
    }
}
