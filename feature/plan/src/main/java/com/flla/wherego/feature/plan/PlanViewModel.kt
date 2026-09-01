package com.flla.wherego.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flla.wherego.core.database.LedgerStore
import com.flla.wherego.core.database.PlanStore
import com.flla.wherego.core.database.UserProfileStore
import com.flla.wherego.core.database.zoneOf
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.CategoryKind
import com.flla.wherego.core.model.MoneyFormatter
import com.flla.wherego.core.model.MonthSpend
import com.flla.wherego.core.model.PresetCategories
import com.flla.wherego.core.model.Recurrence
import com.flla.wherego.core.model.RecurringRule
import com.flla.wherego.core.model.TransactionKind
import com.flla.wherego.core.model.UserProfile
import com.flla.wherego.core.sync.DueReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One `Budgets` meter card: category chrome plus this month's spend against the cap. */
data class PlanBudgetRow(
    val id: String,
    val categoryId: String?,
    val emoji: String,
    val name: String,
    val softHex: String,
    val strongHex: String,
    val spentMinor: Long,
    val capMinor: Long,
    val over: Boolean,
    val fraction: Float,
)

/**
 * One `Set aside` meter card. Goals carry no target of their own, so the target is the pot they
 * share — the summed allocation — and the meter shows this goal's slice of it.
 */
data class PlanGoalRow(
    val id: String,
    val emoji: String,
    val name: String,
    val allocatedMinor: Long,
    val targetMinor: Long,
    val percentLabel: String,
    val fraction: Float,
)

data class PlanMonthChoice(
    val id: String,
    val yearMonth: YearMonth,
)

data class PlanUiState(
    val monthId: String = YearMonth.now().toString(),
    val month: YearMonth = YearMonth.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val monthChoices: List<PlanMonthChoice> = emptyList(),
    val monthSpentMinor: Long = 0L,
    val capTotalMinor: Long = 0L,
    val capRemainingMinor: Long = 0L,
    val capFraction: Float = 0f,
    val daysLeft: Int = 0,
    val budgets: List<PlanBudgetRow> = emptyList(),
    val rules: List<RecurringRule> = emptyList(),
    val goals: List<PlanGoalRow> = emptyList(),
    val goalsTotalLabel: String = "",
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
    private val month = MutableStateFlow<YearMonth?>(null)

    val state: StateFlow<PlanUiState> = combine(profiles.profile, month) { profile, picked ->
        zone = zoneOf(profile)
        currency = profile?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY
        val today = LocalDate.now(zone)
        val current = YearMonth.from(today)
        Triple(profile, picked ?: current, current)
    }.flatMapLatest { (_, ym, current) ->
        val today = LocalDate.now(zone)
        val cur = currency
        combine(
            plan.observeBudgets(ym.toString()),
            plan.observeRules(),
            plan.observeGoals(),
            ledger.categories,
            ledger.observeActive(),
        ) { budgets, rules, goals, cats, txs ->
            val catMap = cats.associateBy { it.id }
            val spentByCategory = MonthSpend.byCategory(txs, ym)
            val monthSpent = spentByCategory.values.sum()
            val capTotal = budgets.sumOf { it.amountMinor }
            val goalsTotal = goals.sumOf { it.allocatedMinor }
            PlanUiState(
                monthId = ym.toString(),
                month = ym,
                currentMonth = current,
                today = today,
                monthChoices = monthChoices(current),
                monthSpentMinor = monthSpent,
                capTotalMinor = capTotal,
                capRemainingMinor = capTotal - monthSpent,
                capFraction = fraction(monthSpent, capTotal),
                daysLeft = if (ym == current) ym.lengthOfMonth() - today.dayOfMonth else -1,
                budgets = budgets.map { budget ->
                    val cat = budget.categoryId?.let { catMap[it] }
                    val spent = if (budget.categoryId == null) {
                        monthSpent
                    } else {
                        spentByCategory[budget.categoryId] ?: 0L
                    }
                    val cap = budget.amountMinor
                    val over = spent > cap
                    PlanBudgetRow(
                        id = budget.id,
                        categoryId = budget.categoryId,
                        emoji = cat?.emoji ?: "📦",
                        name = cat?.name.orEmpty(),
                        softHex = cat?.softColorHex
                            ?: budget.categoryId?.let { PresetCategories.softHex(it) }
                            ?: PresetCategories.ACCENT_SOFT_HEX,
                        strongHex = cat?.colorHex
                            ?: budget.categoryId?.let { PresetCategories.strongHex(it) }
                            ?: PresetCategories.ACCENT_HEX,
                        spentMinor = spent,
                        capMinor = cap,
                        over = over,
                        fraction = fraction(spent, cap),
                    )
                },
                rules = rules,
                goals = goals.map { goal ->
                    PlanGoalRow(
                        id = goal.id,
                        emoji = goalEmoji(goal.name),
                        name = goal.name,
                        allocatedMinor = goal.allocatedMinor,
                        targetMinor = goal.targetMinor,
                        percentLabel = if (goal.targetMinor > 0L) "${goal.percent}%" else "—",
                        fraction = goal.fraction,
                    )
                },
                goalsTotalLabel = MoneyFormatter.format(goalsTotal, cur),
                categories = cats.filter { it.kind != CategoryKind.INCOME },
                currency = cur,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun selectMonth(id: String) {
        month.value = YearMonth.parse(id)
    }

    fun addBudget(categoryId: String?, amountMinor: Long) {
        viewModelScope.launch {
            val ym = (month.value ?: YearMonth.from(LocalDate.now(zone))).toString()
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
        firstOn: LocalDate,
    ) {
        viewModelScope.launch {
            val rule = plan.newRule(
                kind = TransactionKind.EXPENSE,
                amountMinor = amountMinor,
                currency = currency,
                categoryId = categoryId,
                note = note,
                freq = freq,
                dayOfMonth = if (freq == Recurrence.MONTHLY) firstOn.dayOfMonth else null,
                weekday = if (freq == Recurrence.WEEKLY) firstOn.dayOfWeek.value else null,
                firstOn = firstOn,
            )
            reminder.schedule(rule, zone)
        }
    }

    fun deleteRule(id: String) {
        viewModelScope.launch { plan.deleteRule(id) }
    }

    fun addGoal(name: String, allocatedMinor: Long, targetMinor: Long) {
        viewModelScope.launch { plan.addGoal(name, allocatedMinor, currency, targetMinor) }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch { plan.deleteGoal(id) }
    }
}

private fun monthChoices(current: YearMonth): List<PlanMonthChoice> =
    (0..11).map { offset ->
        val ym = current.minusMonths(offset.toLong())
        PlanMonthChoice(id = ym.toString(), yearMonth = ym)
    }

private fun fraction(part: Long, whole: Long): Float =
    if (whole <= 0L) 0f else (part.toFloat() / whole.toFloat()).coerceIn(0f, 1f)

/** Goals carry no emoji column; pick one from the name so the badges stay legible. */
private fun goalEmoji(name: String): String {
    val key = name.lowercase(Locale.ROOT)
    return when {
        key.contains("trip") || key.contains("travel") || key.contains("liburan") -> "✈️"
        key.contains("home") || key.contains("house") || key.contains("rumah") -> "🏠"
        key.contains("car") || key.contains("motor") || key.contains("mobil") -> "🚗"
        key.contains("emergency") || key.contains("darurat") -> "🛟"
        key.contains("wedding") || key.contains("nikah") -> "💍"
        key.contains("phone") || key.contains("laptop") || key.contains("gadget") -> "📱"
        key.contains("school") || key.contains("course") || key.contains("kuliah") -> "🎓"
        key.contains("gift") || key.contains("hadiah") -> "🎁"
        else -> "🎯"
    }
}
