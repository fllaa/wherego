package app.wherego.feature.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wherego.core.database.ReceiptStore
import app.wherego.core.database.LedgerStore
import app.wherego.core.database.UserProfileStore
import app.wherego.core.model.OcrAmountParser
import app.wherego.core.model.UserProfile
import app.wherego.core.sync.ReceiptUploadScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiptUiState(
    val transactionId: String? = null,
    val busy: Boolean = false,
    val proposedAmount: Long? = null,
    val currency: String = UserProfile.DEFAULT_CURRENCY,
    val error: String? = null,
    val savedLocal: Boolean = false,
)

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val receipts: ReceiptStore,
    private val ledger: LedgerStore,
    private val ocr: ReceiptOcr,
    private val upload: ReceiptUploadScheduler,
    private val profiles: UserProfileStore,
) : ViewModel() {
    private val _state = MutableStateFlow(ReceiptUiState())
    val state: StateFlow<ReceiptUiState> = _state.asStateFlow()

    fun reset(transactionId: String) {
        _state.value = ReceiptUiState(transactionId = transactionId)
    }

    fun ingest(uri: Uri) {
        val txId = _state.value.transactionId ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, proposedAmount = null) }
            val row = receipts.ingest(txId, uri)
            if (row == null) {
                _state.update { it.copy(busy = false, error = "Couldn't save the photo") }
                return@launch
            }
            upload.enqueue(row.id)
            val raw = ocr.read(File(row.localPath))
            val currency = profiles.profile.first()?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY
            val amount = OcrAmountParser.parseLargest(raw, currency)
            receipts.recordOcr(row.id, raw, amount)
            _state.update {
                it.copy(
                    busy = false,
                    savedLocal = true,
                    proposedAmount = amount,
                    currency = currency,
                    error = if (amount == null) "Saved the photo. Couldn't read an amount." else null,
                )
            }
        }
    }

    fun confirmAmount() {
        val snapshot = _state.value
        val txId = snapshot.transactionId ?: return
        val amount = snapshot.proposedAmount ?: return
        viewModelScope.launch {
            ledger.applyOcrAmount(txId, amount)
            _state.update { it.copy(proposedAmount = null) }
        }
    }

    fun keepAmount() {
        _state.update { it.copy(proposedAmount = null) }
    }
}
