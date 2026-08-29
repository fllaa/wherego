package app.wherego.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wherego.core.database.LedgerStore
import app.wherego.core.database.PlanStore
import app.wherego.core.database.UserProfileStore
import app.wherego.core.database.zoneOf
import app.wherego.core.datastore.ThemePreferences
import app.wherego.core.model.Category
import app.wherego.core.model.CsvImport
import app.wherego.core.model.CsvMapping
import app.wherego.core.model.DigitBuffer
import app.wherego.core.model.MoneyFormatter
import app.wherego.core.model.ThemeMode
import app.wherego.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val displayName: String = "",
    val themeMode: String = ThemeMode.SYSTEM,
    val currency: String = UserProfile.DEFAULT_CURRENCY,
    val balanceLabel: String = MoneyFormatter.format(0L, UserProfile.DEFAULT_CURRENCY),
    val balanceDigits: String = "",
    val categories: List<Category> = emptyList(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profiles: UserProfileStore,
    private val ledger: LedgerStore,
    private val plan: PlanStore,
    private val themePreferences: ThemePreferences,
) : ViewModel() {
    private val balanceDigits = MutableStateFlow("")

    val state: StateFlow<SettingsUiState> = combine(
        profiles.profile,
        themePreferences.mode,
        ledger.allCategories,
        balanceDigits,
    ) { profile, theme, cats, digits ->
        val currency = profile?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY
        SettingsUiState(
            displayName = profile?.displayName.orEmpty(),
            themeMode = theme,
            currency = currency,
            balanceLabel = MoneyFormatter.format(0L, currency),
            balanceDigits = digits,
            categories = cats,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _balanceNow = MutableStateFlow(0L)
    val balanceNow: StateFlow<Long> = _balanceNow

    init {
        viewModelScope.launch {
            profiles.profile.collect { profile ->
                if (profile != null) {
                    _balanceNow.value = ledger.currentBalance(profile.startingBalanceMinor)
                }
            }
        }
    }

    fun onDisplayName(name: String) {
        viewModelScope.launch { profiles.updateDisplayName(name) }
    }

    fun onTheme(mode: String) {
        viewModelScope.launch { themePreferences.setMode(mode) }
    }

    fun onBalanceDigits(chunk: String) {
        balanceDigits.value = DigitBuffer.append(balanceDigits.value, chunk)
    }

    fun onBalanceBackspace() {
        balanceDigits.value = DigitBuffer.backspace(balanceDigits.value)
    }

    fun setBalanceTo() {
        viewModelScope.launch {
            val profile = profiles.profile.first() ?: return@launch
            val target = DigitBuffer.amountMinor(balanceDigits.value)
            ledger.setBalanceTo(
                targetMinor = target,
                startingBalanceMinor = profile.startingBalanceMinor,
                currency = profile.baseCurrency,
                zoneId = zoneOf(profile),
            )
            _balanceNow.value = ledger.currentBalance(profile.startingBalanceMinor)
            balanceDigits.value = ""
        }
    }

    fun updateCategory(id: String, name: String, emoji: String, colorHex: String) {
        viewModelScope.launch { ledger.updateCategory(id, name, emoji, colorHex) }
    }

    fun archiveCategory(id: String, archived: Boolean) {
        viewModelScope.launch { ledger.archiveCategory(id, archived) }
    }

    fun completeOnboarding(currency: String, startingBalanceMinor: Long, displayName: String?) {
        viewModelScope.launch {
            profiles.completeOnboarding(currency, startingBalanceMinor, displayName)
        }
    }

    suspend fun exportCsv(): String = plan.exportCsv()

    suspend fun importCsv(text: String, mapping: CsvMapping, skipHeader: Boolean): Int {
        val parsed = CsvImport.parse(text)
        val rows = CsvImport.apply(parsed, mapping, skipHeader)
        val profile = profiles.profile.first()
        return ledger.importRows(
            rows,
            profile?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY,
            zoneOf(profile),
        )
    }
}
