package com.flla.wherego.feature.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flla.wherego.core.database.LedgerStore
import com.flla.wherego.core.database.PlanStore
import com.flla.wherego.core.database.UserProfileStore
import com.flla.wherego.core.database.zoneOf
import com.flla.wherego.core.datastore.AppLock
import com.flla.wherego.core.datastore.ThemePreferences
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.CsvImport
import com.flla.wherego.core.model.CsvMapping
import com.flla.wherego.core.model.DigitBuffer
import com.flla.wherego.core.model.LogStreak
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.MonthPdf
import com.flla.wherego.core.model.PresetCategories
import com.flla.wherego.core.model.RecurringRule
import com.flla.wherego.core.model.ThemeMode
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.UserProfile
import com.flla.wherego.core.sync.AuthRepository
import com.flla.wherego.core.sync.AccountEraser
import com.flla.wherego.core.sync.AuthState
import com.flla.wherego.core.sync.SignInException
import com.flla.wherego.core.sync.SignInFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One row of the read-only `Me → Recurring` list. Plan still owns rule editing. */
data class RecurringSummary(
    val emoji: String,
    val note: String,
    val categoryId: String,
    val categoryName: String?,
    val amountMinor: Long,
    val currency: String,
    val freq: String,
    val nextOn: String,
)

data class SettingsUiState(
    val displayName: String = "",
    val themeMode: String = ThemeMode.SYSTEM,
    val currency: String = UserProfile.DEFAULT_CURRENCY,
    val localeTag: String = UserProfile.DEFAULT_LANGUAGE,
    val timeZoneId: String = UserProfile.DEFAULT_ZONE,
    val photoUrl: String? = null,
    val balanceLabel: String = MoneyFormatter.format(0L, UserProfile.DEFAULT_CURRENCY),
    val balanceDigits: String = "",
    val categories: List<Category> = emptyList(),
    val signedIn: Boolean = false,
    val accountLine: String? = null,
    val initial: String = "?",
    val email: String? = null,
    val streakDays: Int = 0,
    val logsThisMonth: Int = 0,
    val daysLogged: Int = 0,
    val daysInMonth: Int = YearMonth.now().lengthOfMonth(),
    val yearMonth: String = YearMonth.now().toString(),
    val categoryCount: Int = 0,
    val recurringActiveCount: Int = 0,
    val recurringRules: List<RecurringSummary> = emptyList(),
    /** `Me → APP → Hide amounts`; masks every figure Wherego renders while browsing. */
    val amountsHidden: Boolean = false,
    /** `Me → APP → App lock`; whether a PIN gates the app on launch. */
    val appLockOn: Boolean = false,
    /**
     * `DueReminder` is enqueued unconditionally for every recurring rule the user
     * creates or confirms, and nothing persists an opt-out, so the row always reads On.
     */
    val remindersOn: Boolean = true,
)

sealed interface EraseState {
    data object Idle : EraseState
    data object Busy : EraseState
    /** [cancelled] distinguishes a dismissed Google re-auth sheet from a real failure. */
    data class Failed(val cancelled: Boolean) : EraseState
}

/** The five flows that only describe the profile/theme/account half of `Me`. */
private data class Account(
    val profile: UserProfile?,
    val theme: String,
    val categories: List<Category>,
    val digits: String,
    val auth: AuthState,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profiles: UserProfileStore,
    private val ledger: LedgerStore,
    private val plan: PlanStore,
    private val themePreferences: ThemePreferences,
    private val appLock: AppLock,
    private val auth: AuthRepository,
    private val eraser: AccountEraser,
) : ViewModel() {
    private val balanceDigits = MutableStateFlow("")

    private val account = combine(
        profiles.profile,
        themePreferences.mode,
        ledger.allCategories,
        balanceDigits,
        auth.state,
    ) { profile, theme, cats, digits, authState ->
        Account(profile, theme, cats, digits, authState)
    }

    val state: StateFlow<SettingsUiState> = combine(
        account,
        ledger.observeActive(),
        plan.observeRules(),
        themePreferences.amountsHidden,
        appLock.enabled,
    ) { acc, txs, rules, hidden, lockOn ->
        val profile = acc.profile
        val currency = profile?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY
        val accountLine = if (acc.auth.signedIn) {
            listOf(acc.auth.displayName, acc.auth.email)
                .firstOrNull { !it.isNullOrBlank() }
        } else {
            null
        }
        val name = profile?.displayName?.takeIf { it.isNotBlank() }
            ?: acc.auth.displayName?.takeIf { it.isNotBlank() }
        val today = LocalDate.now(zoneOf(profile))
        val ym = YearMonth.from(today)
        val start = ym.atDay(1).toString()
        val end = ym.atEndOfMonth().toString()
        val inMonth = txs.filter { it.occurredOn >= start && it.occurredOn <= end }
        val todayIso = today.toString()
        val activeRules = rules.filter { rule ->
            val endOn = rule.endOn
            endOn == null || endOn >= todayIso
        }
        SettingsUiState(
            displayName = name.orEmpty(),
            themeMode = acc.theme,
            currency = currency,
            localeTag = profile?.localeTag ?: UserProfile.DEFAULT_LANGUAGE,
            timeZoneId = profile?.timeZoneId ?: UserProfile.DEFAULT_ZONE,
            photoUrl = acc.auth.photoUrl ?: profile?.photoUrl,
            balanceLabel = MoneyFormatter.format(0L, currency),
            balanceDigits = acc.digits,
            categories = acc.categories,
            signedIn = acc.auth.signedIn,
            accountLine = accountLine,
            initial = name?.trim()?.firstOrNull()?.uppercase() ?: "?",
            email = acc.auth.email ?: profile?.email,
            streakDays = LogStreak.distinctDays(txs.map { it.occurredOn }),
            logsThisMonth = inMonth.size,
            daysLogged = LogStreak.distinctDays(inMonth.map { it.occurredOn }),
            daysInMonth = ym.lengthOfMonth(),
            yearMonth = ym.toString(),
            categoryCount = acc.categories.count { !it.archived },
            recurringActiveCount = activeRules.size,
            recurringRules = activeRules.map { rule -> summarise(rule, acc.categories) },
            amountsHidden = hidden,
            appLockOn = lockOn,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private fun summarise(rule: RecurringRule, categories: List<Category>): RecurringSummary {
        val category = categories.firstOrNull { it.id == rule.categoryId }
        return RecurringSummary(
            emoji = category?.emoji ?: "🔁",
            note = rule.note,
            categoryId = rule.categoryId,
            categoryName = category?.name,
            amountMinor = rule.amountMinor,
            currency = rule.currency,
            freq = rule.freq,
            nextOn = rule.nextOn,
        )
    }

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

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }

    private val _erase = MutableStateFlow<EraseState>(EraseState.Idle)
    val erase: StateFlow<EraseState> = _erase.asStateFlow()

    fun eraseAccount(activity: Activity?) {
        if (_erase.value == EraseState.Busy) return
        if (activity == null) {
            _erase.value = EraseState.Failed(cancelled = false)
            return
        }
        viewModelScope.launch {
            _erase.value = EraseState.Busy
            eraser.erase(activity)
                .onSuccess { _erase.value = EraseState.Idle }
                .onFailure { e ->
                    val failure = (e as? SignInException)?.failure
                    _erase.value = EraseState.Failed(failure == SignInFailure.CANCELLED)
                }
        }
    }

    fun onDisplayName(name: String) {
        viewModelScope.launch { profiles.updateDisplayName(name) }
    }

    fun onLocale(tag: String) {
        viewModelScope.launch { profiles.updateLocale(tag) }
    }

    fun onTimeZone(id: String) {
        viewModelScope.launch { profiles.updateTimeZone(id) }
    }

    fun onTheme(mode: String) {
        viewModelScope.launch { themePreferences.setMode(mode) }
    }

    fun toggleAmountsHidden() {
        viewModelScope.launch { themePreferences.toggleAmountsHidden() }
    }

    /**
     * `Me → YOUR MONEY → Currency`. `completeOnboarding` is the only write path to
     * `baseCurrency`, so it is reused with the balance the profile already carries.
     */
    fun onCurrency(code: String) {
        viewModelScope.launch {
            val profile = profiles.profile.first() ?: return@launch
            if (profile.baseCurrency == code) return@launch
            profiles.completeOnboarding(code, null)
        }
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
                currency = profile.baseCurrency,
                zoneId = zoneOf(profile),
            )
            _balanceNow.value = ledger.currentBalance(profile.startingBalanceMinor)
            balanceDigits.value = ""
        }
    }

    fun updateCategory(id: String, name: String, emoji: String, colorHex: String, kind: String? = null) {
        viewModelScope.launch { ledger.updateCategory(id, name, emoji, colorHex, kind) }
    }

    fun createCategory(name: String, emoji: String, colorHex: String, kind: String) {
        viewModelScope.launch { ledger.createCategory(name, emoji, colorHex, kind) }
    }

    fun pinCategory(id: String) {
        viewModelScope.launch { ledger.pinCategoryToTop(id) }
    }

    fun archiveCategory(id: String, archived: Boolean) {
        viewModelScope.launch { ledger.archiveCategory(id, archived) }
    }

    /**
     * Finish `pencil-new.pen` → `Onboarding 2/3`: persist currency + starting balance
     * and reconcile the bucket picker. Unticked expense presets are archived, never
     * deleted, so Me → Categories can bring them back.
     */
    fun completeOnboarding(
        currency: String,
        startingBalanceMinor: Long,
        keptCategoryIds: Set<String>?,
    ) {
        viewModelScope.launch {
            if (keptCategoryIds != null) {
                for (preset in PresetCategories.expense) {
                    ledger.archiveCategory(preset.id, preset.id !in keptCategoryIds)
                }
            }
            profiles.completeOnboarding(currency, null)
            if (startingBalanceMinor != 0L) {
                ledger.setBalanceTo(
                    targetMinor = startingBalanceMinor,
                    currency = currency,
                    zoneId = zoneOf(profiles.profile.first()),
                )
            }
        }
    }

    suspend fun exportCsv(): String = plan.exportCsv()

    /**
     * `Me → DATA → Month report PDF`. Same report Stories shares, built for the
     * month the device is in, ready for `MonthPdfWriter.write`.
     */
    suspend fun monthPdfLines(
        titleLine: String,
        totalLine: String,
        emptyBars: String,
        emptyTxs: String,
    ): List<String> {
        val profile = profiles.profile.first()
        val currency = profile?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY
        val ym = YearMonth.from(LocalDate.now(zoneOf(profile)))
        val spends = ledger.observeMonth(ym).first()
        val txs = ledger.observeActive().first()
        val start = ym.atDay(1).toString()
        val end = ym.atEndOfMonth().toString()
        val txLines = txs
            .filter { it.occurredOn >= start && it.occurredOn <= end }
            .map { tx -> pdfLine(tx, spends.firstOrNull { it.categoryId == tx.categoryId }?.name) }
        val barPairs = spends.take(3).map { spend ->
            "${spend.emoji} ${spend.name}" to MoneyFormatter.format(spend.amountMinor, currency)
        }
        val total = MoneyFormatter.format(spends.sumOf { it.amountMinor }, currency)
        return MonthPdf.lines(
            titleLine = titleLine,
            totalLine = totalLine.format(total),
            bars = barPairs,
            txs = txLines,
            emptyBars = emptyBars,
            emptyTxs = emptyTxs,
        )
    }

    private fun pdfLine(tx: Transaction, categoryName: String?): String {
        val amount = MoneyFormatter.format(tx.amountMinor, tx.currency)
        val category = categoryName ?: tx.categoryId
        return "${tx.occurredOn}  ${tx.kind}  $amount  $category  ${tx.note}"
    }

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
