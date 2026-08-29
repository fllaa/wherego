package app.wherego.feature.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wherego.core.database.LedgerStore
import app.wherego.core.database.UserProfileStore
import app.wherego.core.database.zoneOf
import app.wherego.core.model.CategorySpend
import app.wherego.core.model.MoneyFormatter
import app.wherego.core.model.MonthStory
import app.wherego.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class StoriesUiState(
    val title: String = "",
    val totalLabel: String = MoneyFormatter.format(0L, UserProfile.DEFAULT_CURRENCY),
    val bars: List<CategorySpend> = emptyList(),
    val sentence: String = MonthStory.sentence(emptyList()),
    val canGoNext: Boolean = false,
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
        ledger.observeMonth(ym).map { spends ->
            val top = spends.take(3)
            StoriesUiState(
                title = ym.format(titleFmt).replaceFirstChar { it.titlecase(Locale("id", "ID")) },
                totalLabel = MoneyFormatter.format(spends.sumOf { it.amountMinor }, currency),
                bars = top,
                sentence = MonthStory.sentence(spends),
                canGoNext = ym < current,
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
