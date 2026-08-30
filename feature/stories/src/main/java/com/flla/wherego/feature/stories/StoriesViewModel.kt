package com.flla.wherego.feature.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flla.wherego.core.database.LedgerStore
import com.flla.wherego.core.database.UserProfileStore
import com.flla.wherego.core.database.zoneOf
import com.flla.wherego.core.model.BalancePoint
import com.flla.wherego.core.model.BalanceSeries
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.MonthPdf
import com.flla.wherego.core.model.MonthStory
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
import kotlin.math.abs
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
    val title: String,
    val subtitle: String,
    val amountLabel: String,
    val badgeSoftHex: String,
)

/** One `Day List` group: header plus the day's transactions, newest first. */
data class StoryDay(
    val date: String,
    val dayTitle: String,
    val dayTotalLabel: String,
    val dayIncomeLabel: String,
    val transactions: List<StoryTx>,
)

data class StoriesUiState(
    /** `MMMM yyyy` — the PDF report title, not shown on screen. */
    val title: String = "",
    /** Bare month name, e.g. `August`. */
    val monthLabel: String = "",
    /** Bare month name of the month before the shown one, e.g. `July`. */
    val prevMonthLabel: String = "",
    val yearMonth: String = "",
    val totalLabel: String = MoneyFormatter.format(0L, UserProfile.DEFAULT_CURRENCY),
    /** e.g. `Rp 412.000 less`; null when the previous month has nothing to compare against. */
    val deltaLabel: String? = null,
    val deltaIsLess: Boolean = true,
    val logCount: Int = 0,
    val bars: List<StoryBar> = emptyList(),
    val days: List<StoryDay> = emptyList(),
    val sentence: String = MonthStory.sentence(emptyList()),
    val canGoNext: Boolean = false,
    val balance: List<BalancePoint> = emptyList(),
    val currency: String = UserProfile.DEFAULT_CURRENCY,
    val pdfLines: List<String> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val ledger: LedgerStore,
    profiles: UserProfileStore,
) : ViewModel() {
    private val month = MutableStateFlow<YearMonth?>(null)
    private val titleFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val monthFmt = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
    private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

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
            val startKey = start.toString()
            val endKey = end.toString()
            val inMonth = txs.filter { it.occurredOn >= startKey && it.occurredOn <= endKey }
            val catById = cats.associateBy { it.id }
            val total = spends.sumOf { it.amountMinor }
            val totalLabel = MoneyFormatter.format(total, currency)
            val title = ym.format(titleFmt)
            val top = spends.take(5)
            val txLines = inMonth.map { tx ->
                val name = catById[tx.categoryId]?.name ?: tx.categoryId
                "${tx.occurredOn}  ${tx.kind}  ${MoneyFormatter.format(tx.amountMinor, tx.currency)}  $name  ${tx.note}"
            }
            val barPairs = top.map { "${it.emoji} ${it.name}" to MoneyFormatter.format(it.amountMinor, currency) }
            val delta = prevSpends.sumOf { it.amountMinor } - total
            StoriesUiState(
                title = title,
                monthLabel = ym.format(monthFmt),
                prevMonthLabel = prevYm.format(monthFmt),
                yearMonth = ym.toString(),
                totalLabel = totalLabel,
                deltaLabel = if (prevSpends.isEmpty()) {
                    null
                } else {
                    MoneyFormatter.format(abs(delta), currency) + (if (delta >= 0L) " less" else " more")
                },
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
                sentence = MonthStory.sentence(spends),
                canGoNext = ym < current,
                balance = BalanceSeries.points(starting, txs, start, end),
                currency = currency,
                pdfLines = MonthPdf.lines(title, totalLabel, barPairs, txLines),
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
                dayTitle = LocalDate.parse(date).format(dayFmt),
                dayTotalLabel = MoneyFormatter.format(sumOfKind(ordered, TransactionKind.EXPENSE), currency),
                dayIncomeLabel = MoneyFormatter.format(sumOfKind(ordered, TransactionKind.INCOME), currency),
                transactions = ordered.map { tx -> tx.toStoryTx(catById[tx.categoryId], currency, zone) },
            )
        }

    private fun sumOfKind(txs: List<Transaction>, kind: String): Long =
        txs.filter { it.kind == kind }.sumOf { it.amountBaseMinor }

    private fun Transaction.toStoryTx(category: Category?, currency: String, zone: ZoneId): StoryTx {
        val name = category?.name ?: "Other"
        val time = occurredAt?.let {
            Instant.ofEpochMilli(it).atZone(zone).toLocalTime().format(timeFmt)
        }
        return StoryTx(
            id = id,
            kind = kind,
            emoji = category?.emoji ?: "📦",
            title = note.ifBlank { name },
            subtitle = if (time != null) "$time · $name" else name,
            amountLabel = MoneyFormatter.format(amountMinor, currency),
            badgeSoftHex = category?.softColorHex ?: PresetCategories.softHex(categoryId),
        )
    }
}
