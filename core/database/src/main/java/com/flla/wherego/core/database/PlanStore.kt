package com.flla.wherego.core.database

import com.flla.wherego.core.common.UlidGenerator
import com.flla.wherego.core.model.Budget
import com.flla.wherego.core.model.BudgetBar
import com.flla.wherego.core.model.CsvExport
import com.flla.wherego.core.model.CsvRow
import com.flla.wherego.core.model.Goal
import com.flla.wherego.core.model.Recurrence
import com.flla.wherego.core.model.RecurringRule
import com.flla.wherego.core.model.TransactionKind
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
        val start = ym.atDay(1).toString()
        val end = ym.atEndOfMonth().toString()
        return combine(
            budgetDao.observeMonth(yearMonth),
            transactionDao.observeActive(),
            categoryDao.observeAll(),
        ) { budgets, txs, cats ->
            if (budgets.isEmpty()) return@combine emptyList()
            val catMap = cats.associate { it.id to it.toModel() }
            val spentByCat = txs
                .map { it.toModel() }
                .filter { it.kind == TransactionKind.EXPENSE }
                .filter { it.occurredOn >= start && it.occurredOn <= end }
                .groupBy { it.categoryId }
                .mapValues { (_, rows) -> rows.sumOf { it.amountBaseMinor } }
            val overallSpent = spentByCat.values.sum()
            budgets.map { it.toModel() }.map { budget ->
                val cat = budget.categoryId?.let { catMap[it] }
                BudgetBar(
                    categoryId = budget.categoryId,
                    name = cat?.name ?: "Overall",
                    emoji = cat?.emoji ?: "📦",
                    colorHex = cat?.colorHex ?: "#10B5A0",
                    spentMinor = if (budget.categoryId == null) overallSpent else spentByCat[budget.categoryId] ?: 0L,
                    capMinor = budget.amountMinor,
                )
            }.take(3)
        }
    }

    suspend fun upsertBudget(
        categoryId: String?,
        amountMinor: Long,
        currency: String,
        yearMonth: String,
    ) {
        val row = Budget(
            id = ulid.next(),
            categoryId = categoryId,
            amountMinor = amountMinor,
            currency = currency,
            yearMonth = yearMonth,
            rollover = false,
            updatedAt = clock.millis(),
        )
        budgetDao.upsert(BudgetEntity.from(row))
    }

    suspend fun deleteBudget(id: String) = budgetDao.delete(id)

    suspend fun upsertRule(rule: RecurringRule) {
        recurringDao.upsert(RecurringEntity.from(rule.copy(updatedAt = clock.millis(), autoPost = false)))
    }

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

    suspend fun newRule(
        kind: String,
        amountMinor: Long,
        currency: String,
        categoryId: String,
        note: String,
        freq: String,
        dayOfMonth: Int?,
        weekday: Int?,
        zoneId: ZoneId,
    ): RecurringRule {
        val today = LocalDate.now(clock.withZone(zoneId))
        val nextOn = today.toString()
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
            startOn = nextOn,
            endOn = null,
            nextOn = nextOn,
            remindDaysBefore = 0,
            autoPost = false,
            updatedAt = clock.millis(),
        )
        recurringDao.upsert(RecurringEntity.from(rule))
        return rule
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

    suspend fun deleteGoal(id: String) {
        goalDao.delete(id)
    }
}

fun YearMonth.iso(): String = toString()
