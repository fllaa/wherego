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
)
