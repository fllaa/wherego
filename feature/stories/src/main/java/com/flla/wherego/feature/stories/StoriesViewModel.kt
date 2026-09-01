package com.flla.wherego.feature.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flla.wherego.core.database.LedgerStore
import com.flla.wherego.core.database.UserProfileStore
import com.flla.wherego.core.database.zoneOf
import com.flla.wherego.core.model.BalanceSeries
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.MonthStory
import com.flla.wherego.core.model.StoryHeadline
import com.flla.wherego.core.model.PresetCategories
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.TransactionKind
import com.flla.wherego.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** One row of `Breakdown Card` — a category's share of the month. */
data class StoryBar(
    val categoryId: String,
    val emoji: String,
    val name: String,
    val amountLabel: String,
    val percentLabel: String,
    val fraction: Float,
    val colorHex: String,
)

/** One `WheregoTxRow` inside a day group. */
data class StoryTx(
    val id: String,
    val kind: String,
    val emoji: String,
    val note: String,
    val categoryId: String,
    val categoryName: String?,
    val time: String?,
    val amountLabel: String,
    val badgeSoftHex: String,
    val hasReceipt: Boolean = false,
)

/** One `Day List` group: header plus the day's transactions, newest first. */
data class StoryDay(
    val date: String,
    val dayTotalLabel: String,
    val dayIncomeLabel: String,
    val transactions: List<StoryTx>,
)

/**
 * The `Balance` card at the foot of Stories: the month's running balance normalised to its own
 * low/high by [BalanceSeries.spark], plus the three numbers that give the line a scale.
 */
data class StoryBalance(
    val fractions: List<Float>,
    val zeroFraction: Float?,
    val nowLabel: String,
    val lowLabel: String,
    val highLabel: String,
    val isFlat: Boolean,
)

data class StoriesUiState(
    val month: YearMonth = YearMonth.now(),
    val prevMonth: YearMonth = YearMonth.now().minusMonths(1),
    val currentMonth: YearMonth = YearMonth.now(),
    val yearMonth: String = "",
    val totalLabel: String = MoneyFormatter.format(0L, UserProfile.DEFAULT_CURRENCY),
    /** Signed `prevSpent - thisSpent`; null when the previous month has nothing to compare against. */
    val deltaMinor: Long? = null,
    val deltaIsLess: Boolean = true,
    val logCount: Int = 0,
    val bars: List<StoryBar> = emptyList(),
    val days: List<StoryDay> = emptyList(),
    val headline: StoryHeadline = StoryHeadline.Empty,
    val canGoNext: Boolean = false,
    val balance: StoryBalance? = null,
    val currency: String = UserProfile.DEFAULT_CURRENCY,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val ledger: LedgerStore,
    profiles: UserProfileStore,
) : ViewModel() {
    private val month = MutableStateFlow<YearMonth?>(null)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    val state: StateFlow<StoriesUiState> = combine(profiles.profile, month) { profile, picked ->
        val zone = zoneOf(profile)
        val current = YearMonth.from(LocalDate.now(zone))
        Triple(profile, picked ?: current, current)
    }.flatMapLatest { (profile, ym, current) ->
        val zone = zoneOf(profile)
        val currency = profile?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY
        val starting = profile?.startingBalanceMinor ?: 0L
        val prevYm = ym.minusMonths(1)
        combine(
            ledger.observeMonth(ym),
            ledger.observeMonth(prevYm),
            ledger.observeActive(),
            ledger.allCategories,
        ) { spends, prevSpends, txs, cats ->
            val start = ym.atDay(1)
            val end = ym.atEndOfMonth()
            // The current month's unspent future days would plot as a flat tail to the 31st.
            val plotEnd = minOf(end, LocalDate.now(zone))
            val startKey = start.toString()
            val endKey = end.toString()
            val inMonth = txs.filter { it.occurredOn >= startKey && it.occurredOn <= endKey }
            val catById = cats.associateBy { it.id }
            val total = spends.sumOf { it.amountMinor }
            val totalLabel = MoneyFormatter.format(total, currency)
            val top = spends.take(5)
            val delta = prevSpends.sumOf { it.amountMinor } - total
            StoriesUiState(
                month = ym,
                prevMonth = prevYm,
                currentMonth = current,
                yearMonth = ym.toString(),
                totalLabel = totalLabel,
                deltaMinor = if (prevSpends.isEmpty()) null else delta,
                deltaIsLess = delta >= 0L,
                logCount = inMonth.size,
                bars = top.map { spend ->
                    StoryBar(
                        categoryId = spend.categoryId,
                        emoji = spend.emoji,
                        name = spend.name,
                        amountLabel = MoneyFormatter.format(spend.amountMinor, currency),
                        percentLabel = "${percentOf(spend.amountMinor, total)}%",
                        fraction = if (total > 0L) spend.amountMinor.toFloat() / total.toFloat() else 0f,
                        colorHex = spend.colorHex,
                    )
                },
                days = dayGroups(inMonth, catById, currency, zone),
                headline = MonthStory.headline(spends),
                canGoNext = ym < current,
                balance = storyBalance(starting, txs, start, plotEnd, currency),
                currency = currency,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoriesUiState())

    fun prevMonth() {
        month.update { (it ?: YearMonth.now()).minusMonths(1) }
    }

    fun nextMonth() {
        month.update { current ->
            val ym = current ?: YearMonth.now()
            ym.plusMonths(1)
        }
    }

    /**
     * `null` when there is nothing worth drawing: fewer than two plotted days, or a balance that
     * never left zero — a fresh profile that skipped the starting balance has no line to show.
     */
    private fun storyBalance(
        startingMinor: Long,
        txs: List<Transaction>,
        from: LocalDate,
        to: LocalDate,
        currency: String,
    ): StoryBalance? {
        val points = BalanceSeries.points(startingMinor, txs, from, to)
        val spark = BalanceSeries.spark(points) ?: return null
        if (spark.isFlat && spark.lastMinor == 0L) return null
        return StoryBalance(
            fractions = spark.fractions,
            zeroFraction = spark.zeroFraction,
            nowLabel = MoneyFormatter.format(spark.lastMinor, currency),
            lowLabel = MoneyFormatter.format(spark.lowMinor, currency),
            highLabel = MoneyFormatter.format(spark.highMinor, currency),
            isFlat = spark.isFlat,
        )
    }

    private fun percentOf(amountMinor: Long, totalMinor: Long): Int =
        if (totalMinor <= 0L) 0 else (amountMinor * 100.0 / totalMinor).roundToInt()

    private fun dayGroups(
        txs: List<Transaction>,
        catById: Map<String, Category>,
        currency: String,
        zone: ZoneId,
    ): List<StoryDay> = txs
        .groupBy { it.occurredOn }
        .entries
        .sortedByDescending { it.key }
        .map { (date, rows) ->
            val ordered = rows.sortedByDescending { it.occurredAt ?: it.createdAt }
            StoryDay(
                date = date,
                dayTotalLabel = MoneyFormatter.format(sumOfKind(ordered, TransactionKind.EXPENSE), currency),
                dayIncomeLabel = MoneyFormatter.format(sumOfKind(ordered, TransactionKind.INCOME), currency),
                transactions = ordered.map { tx -> tx.toStoryTx(catById[tx.categoryId], currency, zone) },
            )
        }

    private fun sumOfKind(txs: List<Transaction>, kind: String): Long =
        txs.filter { it.kind == kind }.sumOf { it.amountBaseMinor }

    private fun Transaction.toStoryTx(category: Category?, currency: String, zone: ZoneId): StoryTx {
        val time = occurredAt?.let {
            Instant.ofEpochMilli(it).atZone(zone).toLocalTime().format(timeFmt)
        }
        return StoryTx(
            id = id,
            kind = kind,
            emoji = category?.emoji ?: "📦",
            note = note,
            categoryId = categoryId,
            categoryName = category?.name,
            time = time,
            amountLabel = MoneyFormatter.format(amountMinor, currency),
            badgeSoftHex = category?.softColorHex ?: PresetCategories.softHex(categoryId),
            hasReceipt = receiptId != null,
        )
    }
}
