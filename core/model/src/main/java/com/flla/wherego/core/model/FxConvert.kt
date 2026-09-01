package com.flla.wherego.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

object FxConvert {
    fun toBase(
        amountMinor: Long,
        fromCurrency: String,
        rateToBase: String,
        baseCurrency: String,
    ): Long {
        if (fromCurrency == baseCurrency) return amountMinor
        val rate = rateToBase.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: BigDecimal.ONE
        val fromScale = CurrencyScale.scale(fromCurrency)
        val toScale = CurrencyScale.scale(baseCurrency)
        return BigDecimal.valueOf(amountMinor)
            .movePointLeft(fromScale)
            .multiply(rate)
            .movePointRight(toScale)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }
}

data class BalancePoint(
    val occurredOn: String,
    val balanceMinor: Long,
)

/**
 * A [BalancePoint] series normalised against its own low and high.
 *
 * A real balance sits far from zero, so a zero-based axis squeezes a whole month of movement
 * into a hairline pinned to the top of the box. [fractions] runs `0f` (the month's low) to
 * `1f` (its high), one entry per plotted day, so the card draws the shape of the month.
 */
data class BalanceSpark(
    val fractions: List<Float>,
    val lowMinor: Long,
    val highMinor: Long,
    val lastMinor: Long,
    /** Where `0` falls on [fractions]' scale, or `null` when the balance never changes sign. */
    val zeroFraction: Float?,
) {
    /** A month with no movement: [fractions] is a flat mid-line, not a shape. */
    val isFlat: Boolean get() = lowMinor == highMinor
}

object BalanceSeries {
    fun signedBase(kind: String, amountBaseMinor: Long): Long = when (kind) {
        TransactionKind.EXPENSE -> -amountBaseMinor
        else -> amountBaseMinor
    }

    fun points(
        startingBalanceMinor: Long,
        txs: List<Transaction>,
        from: LocalDate,
        to: LocalDate,
    ): List<BalancePoint> {
        val active = txs.filter { it.deletedAt == null }.sortedWith(
            compareBy({ it.occurredOn }, { it.createdAt }),
        )
        var balance = startingBalanceMinor
        val fromStr = from.toString()
        for (tx in active) {
            if (tx.occurredOn < fromStr) balance += signedBase(tx.kind, tx.amountBaseMinor)
        }
        val byDate = active.groupBy { it.occurredOn }
        val out = ArrayList<BalancePoint>()
        var day = from
        while (!day.isAfter(to)) {
            val key = day.toString()
            byDate[key].orEmpty().forEach { balance += signedBase(it.kind, it.amountBaseMinor) }
            out += BalancePoint(key, balance)
            day = day.plusDays(1)
        }
        return out
    }

    /** `null` when there is nothing to draw — a line needs at least two days. */
    fun spark(points: List<BalancePoint>): BalanceSpark? {
        if (points.size < 2) return null
        var low = points.first().balanceMinor
        var high = low
        for (point in points) {
            if (point.balanceMinor < low) low = point.balanceMinor
            if (point.balanceMinor > high) high = point.balanceMinor
        }
        val span = (high - low).toFloat()
        return BalanceSpark(
            fractions = if (span == 0f) {
                List(points.size) { 0.5f }
            } else {
                points.map { (it.balanceMinor - low) / span }
            },
            lowMinor = low,
            highMinor = high,
            lastMinor = points.last().balanceMinor,
            zeroFraction = if (low < 0L && high > 0L) -low / span else null,
        )
    }
}

data class Goal(
    val id: String,
    val name: String,
    val allocatedMinor: Long,
    val currency: String,
    val updatedAt: Long,
    /**
     * What the earmark is aiming at. `pencil-new.pen` → `Plan / Set aside` shows
     * "Rp 1.200.000 of Rp 3.000.000" and a percent pill, so progress is measured against
     * this goal's own target — not against the shared pot. `0` means no target set.
     */
    val targetMinor: Long = 0L,
) {
    /** Progress toward [targetMinor] in `0f..1f`; `0f` when no target is set. */
    val fraction: Float
        get() = if (targetMinor <= 0L) 0f else (allocatedMinor.toFloat() / targetMinor).coerceIn(0f, 1f)

    val percent: Int
        get() = if (targetMinor <= 0L) 0 else ((allocatedMinor * 100.0) / targetMinor).toInt().coerceIn(0, 999)
}
