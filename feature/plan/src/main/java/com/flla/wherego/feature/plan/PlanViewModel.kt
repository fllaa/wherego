package com.flla.wherego.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flla.wherego.core.database.LedgerStore
import com.flla.wherego.core.database.PlanStore
import com.flla.wherego.core.database.UserProfileStore
import com.flla.wherego.core.database.zoneOf
import com.flla.wherego.core.model.Budget
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.Goal
import com.flla.wherego.core.model.Recurrence
import com.flla.wherego.core.model.RecurringRule
import com.flla.wherego.core.model.TransactionKind
import com.flla.wherego.core.model.UserProfile
import com.flla.wherego.core.sync.DueReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanUiState(
    val yearMonth: String = YearMonth.now().toString(),
    val budgets: List<Budget> = emptyList(),
    val rules: List<RecurringRule> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val categories: List<Category> = emptyList(),
    val currency: String = UserProfile.DEFAULT_CURRENCY,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlanViewModel @Inject constructor(
    private val plan: PlanStore,
    private val ledger: LedgerStore,
    profiles: UserProfileStore,
    private val reminder: DueReminder,
) : ViewModel() {
    @Volatile private var zone: ZoneId = ZoneId.of(UserProfile.DEFAULT_ZONE)
    @Volatile private var currency: String = UserProfile.DEFAULT_CURRENCY

    val state: StateFlow<PlanUiState> = profiles.profile.flatMapLatest { profile ->
        zone = zoneOf(profile)
        currency = profile?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY
        val ym = YearMonth.from(LocalDate.now(zone)).toString()
        combine(
            plan.observeBudgets(ym),
            plan.observeRules(),
            plan.observeGoals(),
            ledger.categories,
        ) { budgets, rules, goals, cats ->
            PlanUiState(ym, budgets, rules, goals, cats.filter { it.kind != "income" }, currency)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun addBudget(categoryId: String?, amountMinor: Long) {
        viewModelScope.launch {
            val ym = YearMonth.from(LocalDate.now(zone)).toString()
            plan.upsertBudget(categoryId, amountMinor, currency, ym)
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch { plan.deleteBudget(id) }
    }

    fun addRule(
        amountMinor: Long,
        categoryId: String,
        note: String,
        freq: String,
        dayOfMonth: Int?,
    ) {
        viewModelScope.launch {
            val rule = plan.newRule(
                kind = TransactionKind.EXPENSE,
                amountMinor = amountMinor,
                currency = currency,
                categoryId = categoryId,
                note = note,
                freq = freq,
                dayOfMonth = if (freq == Recurrence.MONTHLY) dayOfMonth else null,
                weekday = null,
                zoneId = zone,
            )
            reminder.schedule(rule, zone)
        }
    }

    fun deleteRule(id: String) {
        viewModelScope.launch { plan.deleteRule(id) }
    }

    fun addGoal(name: String, allocatedMinor: Long) {
        viewModelScope.launch { plan.addGoal(name, allocatedMinor, currency) }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch { plan.deleteGoal(id) }
    }
}
