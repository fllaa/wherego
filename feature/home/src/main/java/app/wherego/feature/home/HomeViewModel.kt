package app.wherego.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wherego.core.database.HomeTx
import app.wherego.core.database.LedgerStore
import app.wherego.core.database.UserProfileStore
import app.wherego.core.database.zoneOf
import app.wherego.core.model.MoneyFormatter
import app.wherego.core.model.PresetCategories
import app.wherego.core.model.Transaction
import app.wherego.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TxRowUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val amountLabel: String,
    val emoji: String,
    val badgeSoftHex: String,
    val transaction: Transaction,
)

data class HomeUiState(
    val greetingName: String = "you",
    val monthSpentLabel: String = MoneyFormatter.format(0L, UserProfile.DEFAULT_CURRENCY),
    val todayTotalLabel: String = MoneyFormatter.format(0L, UserProfile.DEFAULT_CURRENCY),
    val today: List<TxRowUi> = emptyList(),
    val earlierThisWeek: List<TxRowUi> = emptyList(),
    val undoId: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ledger: LedgerStore,
    private val profiles: UserProfileStore,
) : ViewModel() {
    private val undoId = MutableStateFlow<String?>(null)
    private var undoJob: Job? = null
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    @Volatile private var lastZone: ZoneId = ZoneId.of(UserProfile.DEFAULT_ZONE)

    val state: StateFlow<HomeUiState> = combine(profiles.profile, undoId) { profile, undo ->
        profile to undo
    }.flatMapLatest { (profile, undo) ->
        val zone = zoneOf(profile)
        lastZone = zone
        val currency = profile?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY
        val greeting = profile?.displayName
            ?.substringBefore(" ")
            ?.takeIf { it.isNotBlank() }
            ?: "you"
        ledger.observeHome(zone).map { home ->
            HomeUiState(
                greetingName = greeting,
                monthSpentLabel = MoneyFormatter.format(home.monthSpentMinor, currency),
                todayTotalLabel = MoneyFormatter.format(home.todayExpenseMinor, currency),
                today = home.today.map { it.toRow(zone, currency) },
                earlierThisWeek = home.earlierThisWeek.map { it.toRow(zone, currency) },
                undoId = undo,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun delete(id: String) {
        viewModelScope.launch {
            ledger.softDelete(id)
            undoId.value = id
            undoJob?.cancel()
            undoJob = launch {
                delay(5_000)
                undoId.value = null
            }
        }
    }

    fun undoDelete() {
        val id = undoId.value ?: return
        undoJob?.cancel()
        viewModelScope.launch {
            ledger.restore(id)
            undoId.value = null
        }
    }

    fun duplicateNow(id: String) {
        viewModelScope.launch {
            ledger.duplicateNow(id, lastZone)
        }
    }

    private fun HomeTx.toRow(zone: ZoneId, currency: String): TxRowUi {
        val name = category?.name ?: "Other"
        val title = tx.note.ifBlank { name }
        val time = tx.occurredAt?.let {
            Instant.ofEpochMilli(it).atZone(zone).toLocalTime().format(timeFormat)
        }
        val subtitle = if (time != null) "$time · $name" else name
        return TxRowUi(
            id = tx.id,
            title = title,
            subtitle = subtitle,
            amountLabel = MoneyFormatter.format(tx.amountMinor, currency),
            emoji = category?.emoji ?: "📦",
            badgeSoftHex = category?.softColorHex ?: PresetCategories.softHex(tx.categoryId),
            transaction = tx,
        )
    }
}
