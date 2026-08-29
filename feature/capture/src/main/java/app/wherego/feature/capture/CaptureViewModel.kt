package app.wherego.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wherego.core.database.CaptureDraft
import app.wherego.core.database.FxRateStore
import app.wherego.core.database.LedgerStore
import app.wherego.core.database.UserProfileStore
import app.wherego.core.database.zoneOf
import app.wherego.core.sync.SyncScheduler
import app.wherego.core.model.Category
import app.wherego.core.model.DigitBuffer
import app.wherego.core.model.MoneyFormatter
import app.wherego.core.model.Transaction
import app.wherego.core.model.TransactionKind
import app.wherego.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CaptureUiState(
    val editingId: String? = null,
    val kind: String = TransactionKind.EXPENSE,
    val digits: String = "",
    val categoryId: String? = null,
    val note: String = "",
    val occurredOn: String = "",
    val noteOpen: Boolean = false,
    val showDatePicker: Boolean = false,
    val showAllCategories: Boolean = false,
    val categories: List<Category> = emptyList(),
    val recentIds: List<String> = emptyList(),
    val zoneId: ZoneId = ZoneId.of(UserProfile.DEFAULT_ZONE),
    val currency: String = UserProfile.DEFAULT_CURRENCY,
    val baseCurrency: String = UserProfile.DEFAULT_CURRENCY,
    val fxRate: String = "1",
) {
    val amountMinor: Long get() = DigitBuffer.amountMinor(digits)
    val canSave: Boolean get() = amountMinor > 0L && categoryId != null
    val amountLabel: String get() = MoneyFormatter.format(amountMinor, currency)
    val matchingCategories: List<Category>
        get() = categories.filter { it.matches(kind) }
    val chipCategories: List<Category>
        get() {
            val matching = matchingCategories
            if (recentIds.isNotEmpty()) {
                val byId = matching.associateBy { it.id }
                return recentIds.mapNotNull { byId[it] }.take(6)
            }
            return matching.take(6)
        }
}
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val ledger: LedgerStore,
    private val profiles: UserProfileStore,
    private val syncScheduler: SyncScheduler,
    private val fxRates: FxRateStore,
) : ViewModel() {
    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            ledger.categories.collect { cats ->
                _state.update { it.copy(categories = cats) }
            }
        }
    }

    fun beginCreate() {
        viewModelScope.launch {
            val profile = profiles.profile.first()
            val zone = zoneOf(profile)
            val currency = profile?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY
            val today = ledger.todayOn(zone)
            val recent = ledger.recentCategoryIds(TransactionKind.EXPENSE)
            _state.update {
                CaptureUiState(
                    editingId = null,
                    kind = TransactionKind.EXPENSE,
                    digits = "",
                    categoryId = null,
                    note = "",
                    occurredOn = today,
                    categories = it.categories,
                    recentIds = recent,
                    zoneId = zone,
                    currency = currency,
                    baseCurrency = currency,
                    fxRate = "1",
                )
            }
        }
    }

    fun beginEdit(tx: Transaction) {
        viewModelScope.launch {
            val profile = profiles.profile.first()
            val zone = zoneOf(profile)
            val recent = ledger.recentCategoryIds(tx.kind)
            _state.update {
                CaptureUiState(
                    editingId = tx.id,
                    kind = tx.kind,
                    digits = DigitBuffer.replace(tx.amountMinor),
                    categoryId = tx.categoryId,
                    note = tx.note,
                    occurredOn = tx.occurredOn,
                    noteOpen = tx.note.isNotBlank(),
                    categories = it.categories,
                    recentIds = recent,
                    zoneId = zone,
                    currency = tx.currency,
                    baseCurrency = profile?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY,
                    fxRate = tx.fxRateToBase,
                )
            }
        }
    }

    fun onKind(kind: String) {
        viewModelScope.launch {
            val recent = ledger.recentCategoryIds(kind)
            _state.update { s ->
                val keep = s.categoryId != null &&
                    s.categories.any { it.id == s.categoryId && it.matches(kind) }
                s.copy(
                    kind = kind,
                    categoryId = if (keep) s.categoryId else null,
                    recentIds = recent,
                )
            }
        }
    }

    fun onDigit(chunk: String) {
        _state.update { it.copy(digits = DigitBuffer.append(it.digits, chunk)) }
    }

    fun onBackspace() {
        _state.update { it.copy(digits = DigitBuffer.backspace(it.digits)) }
    }

    fun onQuickAmount(amountMinor: Long) {
        _state.update { it.copy(digits = DigitBuffer.replace(amountMinor)) }
    }

    fun onCategory(id: String) {
        _state.update { it.copy(categoryId = id, showAllCategories = false) }
    }

    fun onNote(note: String) {
        _state.update { it.copy(note = note.take(80).replace("\n", "")) }
    }

    fun toggleNote() {
        _state.update { it.copy(noteOpen = !it.noteOpen) }
    }

    fun onToday() {
        _state.update { it.copy(occurredOn = ledger.todayOn(it.zoneId)) }
    }

    fun onYesterday() {
        _state.update { it.copy(occurredOn = ledger.yesterdayOn(it.zoneId)) }
    }

    fun onPickRequested() {
        _state.update { it.copy(showDatePicker = true) }
    }

    fun onPickDismissed() {
        _state.update { it.copy(showDatePicker = false) }
    }

    fun onDatePicked(occurredOn: String) {
        _state.update { it.copy(occurredOn = occurredOn, showDatePicker = false) }
    }

    fun onToggleMore() {
        _state.update { it.copy(showAllCategories = !it.showAllCategories) }
    }

    fun cycleCurrency() {
        viewModelScope.launch {
            val codes = listOf("IDR", "USD", "SGD", "EUR")
            val i = codes.indexOf(_state.value.currency)
            val next = codes[(if (i < 0) 0 else i + 1) % codes.size]
            val rate = fxRates.rateToBase(next, _state.value.baseCurrency)
            _state.update { it.copy(currency = next, fxRate = rate) }
        }
    }

    fun onFxRate(raw: String) {
        _state.update { it.copy(fxRate = raw.filter { ch -> ch.isDigit() || ch == '.' }.take(12).ifBlank { "1" }) }
    }


    fun save(onDone: (Transaction) -> Unit) {
        val snapshot = _state.value
        if (!snapshot.canSave) return
        viewModelScope.launch {
            val categoryId = snapshot.categoryId ?: return@launch
            val row = ledger.save(
                CaptureDraft(
                    kind = snapshot.kind,
                    amountMinor = snapshot.amountMinor,
                    currency = snapshot.currency,
                    categoryId = categoryId,
                    note = snapshot.note.trim(),
                    occurredOn = snapshot.occurredOn,
                    occurredAt = ledger.occurredAtForDate(snapshot.occurredOn, snapshot.zoneId),
                    fxRateToBase = snapshot.fxRate,
                    baseCurrency = snapshot.baseCurrency,
                ),
                editingId = snapshot.editingId,
            )
            syncScheduler.enqueueNow()
            onDone(row)
        }
    }
}
