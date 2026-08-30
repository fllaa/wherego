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
