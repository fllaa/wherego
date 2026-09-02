package com.flla.wherego.feature.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flla.wherego.core.common.UlidGenerator
import com.flla.wherego.core.database.CaptureDraft
import com.flla.wherego.core.database.FxRateStore
import com.flla.wherego.core.database.LedgerStore
import com.flla.wherego.core.database.ReceiptStore
import com.flla.wherego.core.database.UserProfileStore
import com.flla.wherego.core.database.zoneOf
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.DigitBuffer
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.OcrAmount
import com.flla.wherego.core.model.PresetCategories
import com.flla.wherego.core.model.ReceiptSource
import com.flla.wherego.core.model.SlipParser
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.TransactionKind
import com.flla.wherego.core.model.UserProfile
import com.flla.wherego.core.sync.ReceiptUploadScheduler
import com.flla.wherego.core.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
data class CaptureUiState(
    val draftId: String = "",
    val editingId: String? = null,
    val receiptId: String? = null,
    /** Where the attached image came from, or null when none is attached. */
    val receiptSource: ReceiptSource? = null,
    val isReadingOcr: Boolean = false,
    val ocrSuggestion: OcrAmount? = null,
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
    val hasReceipt: Boolean get() = receiptId != null
    val amountMinor: Long get() = DigitBuffer.amountMinor(digits)
    /** An assertion of the pot's total: no category to pick, and no receipt or quick spend chips. */
    val isReconcile: Boolean get() = kind == TransactionKind.RECONCILE

    /**
     * A blank buffer never saves, so a stray tap on the `Balance` tab cannot park an anchor that
     * silently stops every earlier row counting. An explicit `0` is a real claim and does save.
     */
    val canSave: Boolean
        get() = if (isReconcile) digits.isNotBlank() else amountMinor > 0L && categoryId != null
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
    private val receipts: ReceiptStore,
    private val ocr: ReceiptOcr,
    private val upload: ReceiptUploadScheduler,
    private val ulid: UlidGenerator,
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

    fun beginCreate(
        initialReceiptUri: Uri? = null,
        source: ReceiptSource = ReceiptSource.OWN,
    ) {
        viewModelScope.launch {
            val profile = profiles.get()
            val zone = zoneOf(profile)
            val currency = profile?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY
            val today = ledger.todayOn(zone)
            val recent = ledger.recentCategoryIds(TransactionKind.EXPENSE)
            val newDraftId = ulid.next()
            _state.update {
                CaptureUiState(
                    draftId = newDraftId,
                    editingId = null,
                    receiptId = null,
                    isReadingOcr = initialReceiptUri != null,
                    ocrSuggestion = null,
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
            if (initialReceiptUri != null) {
                attachReceipt(initialReceiptUri, autoApplyAmount = true, source = source)
            }
        }
    }

    fun beginEdit(tx: Transaction) {
        viewModelScope.launch {
            val profile = profiles.get()
            val zone = zoneOf(profile)
            val recent = ledger.recentCategoryIds(tx.kind)
            _state.update {
                CaptureUiState(
                    draftId = tx.id,
                    editingId = tx.id,
                    receiptId = tx.receiptId,
                    isReadingOcr = false,
                    ocrSuggestion = null,
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

    fun attachReceipt(
        uri: Uri,
        autoApplyAmount: Boolean = false,
        source: ReceiptSource = ReceiptSource.OWN,
    ) {
        val currentDraftId = _state.value.draftId.ifBlank { ulid.next() }
        _state.update { it.copy(draftId = currentDraftId, isReadingOcr = true) }
        viewModelScope.launch {
            val row = receipts.ingest(currentDraftId, uri)
            if (row == null) {
                _state.update { it.copy(isReadingOcr = false) }
                return@launch
            }
            // A screen shared in from another app stays on this phone: it carries the account
            // number and the remaining balance beside the amount, and backup was never asked
            // about those.
            if (source == ReceiptSource.OWN) upload.enqueue(row.id)
            val read = ocr.read(File(row.localPath))
            val before = _state.value
            val todayOn = ledger.todayOn(before.zoneId)
            val slip = SlipParser.parse(read, before.currency, LocalDate.parse(todayOn))
            receipts.recordOcr(row.id, read.raw, slip.amount?.minor)
            // Reading the recents for a new kind suspends, so it happens before the atomic update
            // rather than inside it.
            val slipKind = slip.kind?.takeIf { it != before.kind }
            val recentForKind = slipKind?.let { ledger.recentCategoryIds(it) }
            _state.update { s ->
                val parsed = slip.amount
                // A foreign screen is never filled in, however empty the buffer is. Its largest
                // money number is usually the balance, not the spend, so the read is offered and
                // the user says yes — an anchored read only earns the amount on a real receipt.
                val wantsFill = source == ReceiptSource.OWN &&
                    (autoApplyAmount || s.digits.isBlank() || s.amountMinor == 0L)
                // An unanchored read is only the largest number left once the reference numbers
                // were thrown out — a guess. It may be offered, never written into the amount,
                // no matter how empty the buffer is or how the sheet was opened.
                val fill = if (parsed != null && parsed.anchored && wantsFill) parsed.minor else null
                val offer = if (fill == null && parsed != null && parsed.minor != s.amountMinor) {
                    parsed
                } else {
                    null
                }

                // Kind, date and counterparty are prefills rather than writes to money: each lands
                // only where the user has not already said otherwise, and all three sit visible in
                // the sheet, so a bad read costs a glance instead of a wrong row.
                val kind = slipKind ?: s.kind
                val categoryId = when {
                    // Untouched kind, untouched category — including before the categories have
                    // loaded, when no choice can be validated yet.
                    slipKind == null -> s.categoryId
                    s.categoryId != null && s.categories.any { it.id == s.categoryId && it.matches(kind) } ->
                        s.categoryId
                    else -> null
                }
                // Only while the draft still shows today: any other date is one the user picked, or
                // the date of the row being edited.
                val slipDate = slip.occurredOn
                    ?.toString()
                    ?.takeIf { s.occurredOn == todayOn && it != s.occurredOn }
                val note = slip.counterparty?.takeIf { s.note.isBlank() }

                s.copy(
                    receiptId = row.id,
                    receiptSource = source,
                    isReadingOcr = false,
                    digits = if (fill != null) DigitBuffer.replace(fill) else s.digits,
                    ocrSuggestion = offer,
                    kind = kind,
                    categoryId = categoryId,
                    recentIds = recentForKind ?: s.recentIds,
                    occurredOn = slipDate ?: s.occurredOn,
                    note = note ?: s.note,
                    noteOpen = s.noteOpen || note != null,
                )
            }
        }
    }

    fun applySuggestedOcrAmount() {
        _state.update { s ->
            val suggestion = s.ocrSuggestion ?: return@update s
            s.copy(
                digits = DigitBuffer.replace(suggestion.minor),
                ocrSuggestion = null,
            )
        }
    }

    fun dismissSuggestedOcrAmount() {
        _state.update { it.copy(ocrSuggestion = null) }
    }

    fun removeReceipt() {
        _state.update { it.copy(receiptId = null, receiptSource = null, ocrSuggestion = null) }
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
            try {
                val categoryId = snapshot.categoryId
                    ?: PresetCategories.OTHER.takeIf { snapshot.isReconcile }
                    ?: return@launch
                val row = ledger.save(
                    CaptureDraft(
                        kind = snapshot.kind,
                        amountMinor = snapshot.amountMinor,
                        currency = snapshot.currency,
                        categoryId = categoryId,
                        note = snapshot.note.trim(),
                        occurredOn = snapshot.occurredOn.ifBlank { ledger.todayOn(snapshot.zoneId) },
                        occurredAt = ledger.occurredAtForDate(snapshot.occurredOn, snapshot.zoneId),
                        recurringId = null,
                        receiptId = snapshot.receiptId,
                        fxRateToBase = snapshot.fxRate,
                        baseCurrency = snapshot.baseCurrency,
                    ),
                    editingId = snapshot.editingId,
                    draftId = snapshot.draftId.ifBlank { null },
                )
                syncScheduler.enqueueNow()
                onDone(row)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}
