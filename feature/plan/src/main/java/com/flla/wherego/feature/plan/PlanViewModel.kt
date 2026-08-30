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
import com.flla.wherego.core.model.PresetCategories
import com.flla.wherego.core.model.Recurrence
import com.flla.wherego.core.model.RecurringRule
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.TransactionKind
import com.flla.wherego.core.model.UserProfile
import com.flla.wherego.core.sync.DueReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    val detail: String,
    val note: String,
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
    val detail: String,
    val percentLabel: String,
    val fraction: Float,
)

private val MonthName = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

data class PlanUiState(
    val monthLabel: String = YearMonth.now().format(MonthName),
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

    val state: StateFlow<PlanUiState> = profiles.profile.flatMapLatest { profile ->
        zone = zoneOf(profile)
        currency = profile?.baseCurrency ?: UserProfile.DEFAULT_CURRENCY
        val today = LocalDate.now(zone)
        val month = YearMonth.from(today)
        val cur = currency
        combine(
            plan.observeBudgets(month.toString()),
            plan.observeRules(),
            plan.observeGoals(),
            ledger.categories,
            ledger.observeActive(),
        ) { budgets, rules, goals, cats, txs ->
            val catMap = cats.associateBy { it.id }
            val spentByCategory = monthSpend(txs, month)
            val monthSpent = spentByCategory.values.sum()
            val capTotal = budgets.sumOf { it.amountMinor }
            val goalsTotal = goals.sumOf { it.allocatedMinor }
            PlanUiState(
                monthLabel = month.format(MonthName),
                monthSpentMinor = monthSpent,
                capTotalMinor = capTotal,
                capRemainingMinor = capTotal - monthSpent,
                capFraction = fraction(monthSpent, capTotal),
                daysLeft = month.lengthOfMonth() - today.dayOfMonth,
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
                        name = cat?.name ?: "Overall",
                        softHex = cat?.softColorHex
                            ?: budget.categoryId?.let { PresetCategories.softHex(it) }
                            ?: "#D5F4EE",
                        strongHex = cat?.colorHex
                            ?: budget.categoryId?.let { PresetCategories.strongHex(it) }
                            ?: "#10B5A0",
                        spentMinor = spent,
                        capMinor = cap,
                        over = over,
                        detail = "${MoneyFormatter.format(spent, cur)} of ${MoneyFormatter.number(cap, cur)}",
                        note = if (over) {
                            "${MoneyFormatter.compact(spent - cap, cur)} over"
                        } else {
                            "${MoneyFormatter.compact(cap - spent, cur)} left"
                        },
                        fraction = fraction(spent, cap),
                    )
                },
                rules = rules,
                goals = goals.map { goal ->
                    val hasTarget = goal.targetMinor > 0L
                    PlanGoalRow(
                        id = goal.id,
                        emoji = goalEmoji(goal.name),
                        name = goal.name,
                        allocatedMinor = goal.allocatedMinor,
                        targetMinor = goal.targetMinor,
                        detail = if (hasTarget) {
                            "${MoneyFormatter.format(goal.allocatedMinor, cur)} of " +
                                MoneyFormatter.format(goal.targetMinor, cur)
                        } else {
                            "${MoneyFormatter.format(goal.allocatedMinor, cur)} set aside"
                        },
                        percentLabel = if (hasTarget) "${goal.percent}%" else "—",
                        fraction = goal.fraction,
                    )
                },
                goalsTotalLabel = MoneyFormatter.format(goalsTotal, cur),
                categories = cats.filter { it.kind != CategoryKind.INCOME },
                currency = cur,
            )
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

    fun addGoal(name: String, allocatedMinor: Long, targetMinor: Long) {
        viewModelScope.launch { plan.addGoal(name, allocatedMinor, currency, targetMinor) }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch { plan.deleteGoal(id) }
    }
}

private fun monthSpend(txs: List<Transaction>, month: YearMonth): Map<String, Long> {
    val start = month.atDay(1).toString()
    val end = month.atEndOfMonth().toString()
    return txs
        .filter { it.kind == TransactionKind.EXPENSE && it.occurredOn >= start && it.occurredOn <= end }
        .groupBy { it.categoryId }
        .mapValues { (_, rows) -> rows.sumOf { it.amountBaseMinor } }
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
