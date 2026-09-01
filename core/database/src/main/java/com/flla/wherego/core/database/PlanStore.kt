package com.flla.wherego.core.database

import com.flla.wherego.core.common.UlidGenerator
import com.flla.wherego.core.model.Budget
import com.flla.wherego.core.model.BudgetBar
import com.flla.wherego.core.model.CsvExport
import com.flla.wherego.core.model.CsvRow
import com.flla.wherego.core.model.Goal
import com.flla.wherego.core.model.MonthSpend
import com.flla.wherego.core.model.PresetCategories
import com.flla.wherego.core.model.Recurrence
import com.flla.wherego.core.model.RecurringRule
import com.flla.wherego.core.model.UserProfile
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

data class DueItem(
    val rule: RecurringRule,
    val categoryName: String,
)

@Singleton
class PlanStore @Inject constructor(
    private val budgetDao: BudgetDao,
    private val recurringDao: RecurringDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val goalDao: GoalDao,
    private val ulid: UlidGenerator,
    private val clock: Clock,
) {
    fun observeBudgets(yearMonth: String): Flow<List<Budget>> =
        budgetDao.observeMonth(yearMonth).map { rows -> rows.map { it.toModel() } }

    fun observeRules(): Flow<List<RecurringRule>> =
        recurringDao.observeAll().map { rows -> rows.map { it.toModel() } }

    fun observeGoals(): Flow<List<Goal>> =
        goalDao.observeAll().map { rows -> rows.map { it.toModel() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDue(today: String): Flow<List<DueItem>> =
        combine(
            recurringDao.observeDue(today),
            categoryDao.observeAll(),
        ) { rules, cats ->
            val names = cats.associate { it.id to it.name }
            rules.map { it.toModel() }
                .filter { Recurrence.isDue(it.nextOn, today, it.endOn) }
                .map { DueItem(it, names[it.categoryId] ?: "Other") }
        }

    fun observeBars(yearMonth: String, zoneId: ZoneId): Flow<List<BudgetBar>> {
        val ym = YearMonth.parse(yearMonth)
        return combine(
            budgetDao.observeMonth(yearMonth),
            transactionDao.observeActive(),
            categoryDao.observeAll(),
        ) { budgets, txs, cats ->
            if (budgets.isEmpty()) return@combine emptyList()
            val catMap = cats.associate { it.id to it.toModel() }
            val spentByCat = MonthSpend.byCategory(txs.map { it.toModel() }, ym)
            val overallSpent = spentByCat.values.sum()
            budgets.map { it.toModel() }.map { budget ->
                val cat = budget.categoryId?.let { catMap[it] }
                BudgetBar(
                    categoryId = budget.categoryId,
                    name = cat?.name ?: "Overall",
                    emoji = cat?.emoji ?: "📦",
                    colorHex = cat?.colorHex ?: PresetCategories.ACCENT_HEX,
                    spentMinor = if (budget.categoryId == null) overallSpent else spentByCat[budget.categoryId] ?: 0L,
                    capMinor = budget.amountMinor,
                )
            }.take(3)
        }
    }

    /**
     * One cap per category per month. [replacedId] is the row an editor started from: when the
     * user moves that cap to another category the old row stops matching and has to go, or the
     * month would carry two caps for one category and the hero would count both.
     */
    suspend fun setBudget(
        categoryId: String?,
        amountMinor: Long,
        currency: String,
        yearMonth: String,
        replacedId: String? = null,
    ) {
        val existing = budgetDao.listMonth(yearMonth).firstOrNull { it.categoryId == categoryId }
        if (replacedId != null && replacedId != existing?.id) budgetDao.delete(replacedId)
        val row = Budget(
            id = existing?.id ?: ulid.next(),
            categoryId = categoryId,
            amountMinor = amountMinor,
            currency = currency,
            yearMonth = yearMonth,
            rollover = existing?.rollover ?: false,
            updatedAt = clock.millis(),
        )
        budgetDao.upsert(BudgetEntity.from(row))
    }

    suspend fun deleteBudget(id: String) = budgetDao.delete(id)

    suspend fun deleteRule(id: String) = recurringDao.delete(id)

    suspend fun confirmDue(ruleId: String, zoneId: ZoneId): RecurringRule? {
        val existing = recurringDao.get(ruleId)?.toModel() ?: return null
        val today = LocalDate.now(clock.withZone(zoneId))
        val occurredOn = existing.nextOn
        // caller creates the transaction; we only advance nextOn
        val next = Recurrence.advance(
            LocalDate.parse(occurredOn),
            existing.freq,
            existing.interval,
            existing.dayOfMonth,
        )
        val updated = existing.copy(nextOn = next.toString(), updatedAt = clock.millis())
        recurringDao.update(RecurringEntity.from(updated))
        return existing.copy(updatedAt = clock.millis())
    }

    /** [firstOn] is the day the bill first falls due; it seeds both `startOn` and `nextOn`. */
    suspend fun newRule(
        kind: String,
        amountMinor: Long,
        currency: String,
        categoryId: String,
        note: String,
        freq: String,
        dayOfMonth: Int?,
        weekday: Int?,
        firstOn: LocalDate,
    ): RecurringRule {
        val startOn = firstOn.toString()
        val rule = RecurringRule(
            id = ulid.next(),
            kind = kind,
            amountMinor = amountMinor,
            currency = currency,
            categoryId = categoryId,
            note = note,
            freq = freq,
            interval = 1,
            dayOfMonth = dayOfMonth,
            weekday = weekday,
            startOn = startOn,
            endOn = null,
            nextOn = startOn,
            remindDaysBefore = 0,
            autoPost = false,
            updatedAt = clock.millis(),
        )
        recurringDao.upsert(RecurringEntity.from(rule))
        return rule
    }

    /**
     * Edits a bill in place. [nextOn] moves the next hit and, for a monthly rule, the day of
     * month every later hit lands on; `startOn` stays as first written so the row keeps its
     * history. Returns the saved rule so the caller can reschedule the reminder.
     */
    suspend fun updateRule(
        id: String,
        amountMinor: Long,
        categoryId: String,
        note: String,
        nextOn: LocalDate,
    ): RecurringRule? {
        val existing = recurringDao.get(id)?.toModel() ?: return null
        val updated = existing.copy(
            amountMinor = amountMinor,
            categoryId = categoryId,
            note = note,
            nextOn = nextOn.toString(),
            dayOfMonth = if (existing.freq == Recurrence.MONTHLY) nextOn.dayOfMonth else existing.dayOfMonth,
            weekday = if (existing.freq == Recurrence.WEEKLY) nextOn.dayOfWeek.value else existing.weekday,
            updatedAt = clock.millis(),
        )
        recurringDao.update(RecurringEntity.from(updated))
        return updated
    }

    suspend fun exportCsv(): String {
        val cats = categoryDao.listAll().associate { it.id to it.name }
        val rows = transactionDao.listActive().map { it.toModel() }.map { tx ->
            CsvRow(
                date = tx.occurredOn,
                kind = tx.kind,
                amount = tx.amountMinor.toString(),
                currency = tx.currency,
                category = cats[tx.categoryId] ?: "",
                note = tx.note,
            )
        }
        return CsvExport.table(rows)
    }

    suspend fun addGoal(
        name: String,
        allocatedMinor: Long,
        currency: String,
        targetMinor: Long = 0L,
    ): Goal {
        val goal = Goal(
            id = ulid.next(),
            name = name.trim().ifBlank { "Goal" },
            allocatedMinor = allocatedMinor,
            currency = currency,
            updatedAt = clock.millis(),
            targetMinor = targetMinor,
        )
        goalDao.upsert(GoalEntity.from(goal))
        return goal
    }

    /** The name is never blanked: an unnamed earmark is unreadable, so a blank one keeps the old. */
    suspend fun updateGoal(id: String, name: String, allocatedMinor: Long, targetMinor: Long) {
        val existing = goalDao.get(id) ?: return
        goalDao.upsert(
            existing.copy(
                name = name.trim().ifBlank { existing.name },
                allocatedMinor = allocatedMinor,
                targetMinor = targetMinor,
                updatedAt = clock.millis(),
            ),
        )
    }

    suspend fun deleteGoal(id: String) {
        goalDao.delete(id)
    }
}

fun YearMonth.iso(): String = toString()
