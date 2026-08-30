package com.flla.wherego.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flla.wherego.core.database.CaptureDraft
import com.flla.wherego.core.database.DueItem
import com.flla.wherego.core.database.HomeTx
import com.flla.wherego.core.database.LedgerStore
import com.flla.wherego.core.database.PlanStore
import com.flla.wherego.core.database.UserProfileStore
import com.flla.wherego.core.database.zoneOf
import com.flla.wherego.core.model.BudgetBar
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.PresetCategories
import com.flla.wherego.core.model.Recurrence
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.UserProfile
import com.flla.wherego.core.sync.CloudDot
import com.flla.wherego.core.sync.CloudStatus
import com.flla.wherego.core.sync.DueReminder
import com.flla.wherego.core.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TxRowUi(
    val id: String,
    val note: String,
    val categoryId: String,
    val categoryName: String?,
    val time: String?,
    val amountLabel: String,
    val emoji: String,
    val badgeSoftHex: String,
    val transaction: Transaction,
    val hasReceipt: Boolean = false,
)

data class HomeUiState(
    val greetingName: String? = null,
    val weekday: DayOfWeek? = null,
    val weekLoggedCount: Int = 0,
    val monthIncomeMinor: Long? = null,
    val monthLeftMinor: Long? = null,
    val monthSpentLabel: String = MoneyFormatter.format(0L, UserProfile.DEFAULT_CURRENCY),
    val todayTotalLabel: String = MoneyFormatter.format(0L, UserProfile.DEFAULT_CURRENCY),
    val today: List<TxRowUi> = emptyList(),
    val earlierThisWeek: List<TxRowUi> = emptyList(),
    val undoId: String? = null,
    val streakDays: Int = 0,
    val hasTxToday: Boolean = false,
    val cloudDot: CloudDot = CloudDot.Offline,
    val budgetBars: List<BudgetBar> = emptyList(),
    val due: List<DueItem> = emptyList(),
    val currency: String = UserProfile.DEFAULT_CURRENCY,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ledger: LedgerStore,
    private val plan: PlanStore,
    private val profiles: UserProfileStore,
    cloudStatus: CloudStatus,
    private val syncScheduler: SyncScheduler,
    private val reminder: DueReminder,
) : ViewModel() {
    private val undoId = MutableStateFlow<String?>(null)
    private var undoJob: Job? = null
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    @Volatile private var lastZone: ZoneId = ZoneId.of(UserProfile.DEFAULT_ZONE)

    val state: StateFlow<HomeUiState> = combine(profiles.profile, undoId, cloudStatus.dot) { profile, undo, dot ->
        Triple(profile, undo, dot)
    }.flatMapLatest { (profile, undo, dot) ->
        val zone = zoneOf(profile)
        lastZone = zone
        val currency = profile?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY
        val greeting = profile?.displayName?.substringBefore(" ")?.takeIf { it.isNotBlank() }
        val todayDate = LocalDate.now(zone)
        val today = todayDate.toString()
        val ym = YearMonth.from(todayDate).toString()
        combine(
            ledger.observeHome(zone),
            plan.observeBars(ym, zone),
            plan.observeDue(today),
        ) { home, bars, due ->
            HomeUiState(
                greetingName = greeting,
                weekday = todayDate.dayOfWeek,
                weekLoggedCount = home.weekLoggedCount,
                monthIncomeMinor = home.monthIncomeMinor.takeIf { it > 0L },
                monthLeftMinor = home.monthIncomeMinor.takeIf { it > 0L }?.let { it - home.monthSpentMinor },
                monthSpentLabel = MoneyFormatter.format(home.monthSpentMinor, currency),
                todayTotalLabel = MoneyFormatter.format(home.todayExpenseMinor, currency),
                today = home.today.map { it.toRow(zone, currency) },
                earlierThisWeek = home.earlierThisWeek.map { it.toRow(zone, currency) },
                undoId = undo,
                streakDays = home.streakDays,
                hasTxToday = home.hasTxToday,
                cloudDot = dot,
                budgetBars = bars,
                due = due,
                currency = currency,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun delete(id: String) {
        viewModelScope.launch {
            ledger.softDelete(id)
            undoId.value = id
            syncScheduler.enqueueNow()
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
            syncScheduler.enqueueNow()
        }
    }

    fun duplicateNow(id: String) {
        viewModelScope.launch {
            ledger.duplicateNow(id, lastZone)
            syncScheduler.enqueueNow()
        }
    }

    fun confirmDue(item: DueItem) {
        viewModelScope.launch {
            val old = plan.confirmDue(item.rule.id, lastZone) ?: return@launch
            ledger.save(
                CaptureDraft(
                    kind = old.kind,
                    amountMinor = old.amountMinor,
                    currency = old.currency,
                    categoryId = old.categoryId,
                    note = old.note,
                    occurredOn = old.nextOn,
                    occurredAt = ledger.occurredAtForDate(old.nextOn, lastZone),
                    recurringId = old.id,
                ),
                editingId = null,
            )
            val next = Recurrence.advance(
                LocalDate.parse(old.nextOn),
                old.freq,
                old.interval,
                old.dayOfMonth,
            )
            reminder.schedule(old.copy(nextOn = next.toString()), lastZone)
            syncScheduler.enqueueNow()
        }
    }

    private fun HomeTx.toRow(zone: ZoneId, currency: String): TxRowUi {
        val time = tx.occurredAt?.let {
            Instant.ofEpochMilli(it).atZone(zone).toLocalTime().format(timeFormat)
        }
        return TxRowUi(
            id = tx.id,
            note = tx.note,
            categoryId = tx.categoryId,
            categoryName = category?.name,
            time = time,
            amountLabel = MoneyFormatter.format(tx.amountMinor, currency),
            emoji = category?.emoji ?: "📦",
            badgeSoftHex = category?.softColorHex ?: PresetCategories.softHex(tx.categoryId),
            transaction = tx,
            hasReceipt = tx.receiptId != null,
        )
    }
}
