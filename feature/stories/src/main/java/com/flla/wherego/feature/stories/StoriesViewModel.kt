package com.flla.wherego.feature.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flla.wherego.core.database.LedgerStore
import com.flla.wherego.core.database.UserProfileStore
import com.flla.wherego.core.database.zoneOf
import com.flla.wherego.core.model.BalancePoint
import com.flla.wherego.core.model.BalanceSeries
import com.flla.wherego.core.model.CategorySpend
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.MonthPdf
import com.flla.wherego.core.model.MonthStory
import com.flla.wherego.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class StoriesUiState(
    val title: String = "",
    val yearMonth: String = "",
    val totalLabel: String = MoneyFormatter.format(0L, UserProfile.DEFAULT_CURRENCY),
    val bars: List<CategorySpend> = emptyList(),
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
    private val titleFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID"))

    val state: StateFlow<StoriesUiState> = combine(profiles.profile, month) { profile, picked ->
        val zone = zoneOf(profile)
        val current = YearMonth.from(LocalDate.now(zone))
        Triple(profile, picked ?: current, current)
    }.flatMapLatest { (profile, ym, current) ->
        val currency = profile?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY
        val starting = profile?.startingBalanceMinor ?: 0L
        combine(ledger.observeMonth(ym), ledger.observeActive()) { spends, txs ->
            val top = spends.take(3)
            val start = ym.atDay(1)
            val end = ym.atEndOfMonth()
            val inMonth = txs.filter { it.occurredOn >= start.toString() && it.occurredOn <= end.toString() }
            val txLines = inMonth.map { tx ->
                val cat = spends.firstOrNull { it.categoryId == tx.categoryId }?.name ?: tx.categoryId
                "${tx.occurredOn}  ${tx.kind}  ${MoneyFormatter.format(tx.amountMinor, tx.currency)}  $cat  ${tx.note}"
            }
            val barPairs = top.map { "${it.emoji} ${it.name}" to MoneyFormatter.format(it.amountMinor, currency) }
            val title = ym.format(titleFmt).replaceFirstChar { it.titlecase(Locale("id", "ID")) }
            val total = MoneyFormatter.format(spends.sumOf { it.amountMinor }, currency)
            StoriesUiState(
                title = title,
                yearMonth = ym.toString(),
                totalLabel = total,
                bars = top,
                sentence = MonthStory.sentence(spends),
                canGoNext = ym < current,
                balance = BalanceSeries.points(starting, txs, start, end),
                currency = currency,
                pdfLines = MonthPdf.lines(title, total, barPairs, txLines),
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
}
