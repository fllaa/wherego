package com.flla.wherego.core.database

import com.flla.wherego.core.common.UlidGenerator
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.CsvRow
import com.flla.wherego.core.model.FxConvert
import com.flla.wherego.core.model.LogStreak
import com.flla.wherego.core.model.MonthSpend
import com.flla.wherego.core.model.MonthStory
import com.flla.wherego.core.model.PresetCategories
import com.flla.wherego.core.model.SpendRow
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.TransactionKind
import com.flla.wherego.core.model.UserProfile
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class HomeLedger(
    val monthSpentMinor: Long,
    val monthIncomeMinor: Long,
    val weekLoggedCount: Int,
    val todayExpenseMinor: Long,
    val today: List<HomeTx>,
    val earlierThisWeek: List<HomeTx>,
    val streakDays: Int,
    val hasTxToday: Boolean,
)

data class HomeTx(
    val tx: Transaction,
    val category: Category?,
)

data class CaptureDraft(
    val kind: String,
    val amountMinor: Long,
    val currency: String,
    val categoryId: String,
    val note: String,
    val occurredOn: String,
    val occurredAt: Long?,
    val recurringId: String? = null,
    val receiptId: String? = null,
    val fxRateToBase: String = "1",
    val baseCurrency: String = UserProfile.DEFAULT_CURRENCY,
)

@Singleton
class LedgerStore @Inject constructor(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val ulid: UlidGenerator,
    private val clock: Clock,
) {
    val categories: Flow<List<Category>> =
        categoryDao.observeActive().map { rows -> rows.map { it.toModel() } }

    fun observeHome(zoneId: ZoneId): Flow<HomeLedger> = combine(
        transactionDao.observeActive(),
        categoryDao.observeActive(),
    ) { txs, cats ->
        val catMap = cats.associate { it.id to it.toModel() }
        assembleHome(txs.map { it.toModel() }, catMap, zoneId)
    }

    suspend fun seedCategoriesIfEmpty() {
        if (categoryDao.count() > 0) return
        val now = clock.millis()
        categoryDao.insertAll(
            PresetCategories.all.map { preset ->
                CategoryEntity(
                    id = preset.id,
                    name = preset.name,
                    emoji = preset.emoji,
                    colorHex = preset.colorHex,
                    kind = preset.kind,
                    isPreset = true,
                    archived = false,
                    sortOrder = preset.sortOrder,
                    updatedAt = now,
                    deletedAt = null,
                )
            },
        )
    }

    suspend fun recentCategoryIds(kind: String, limit: Int = 6): List<String> {
        val seen = LinkedHashSet<String>()
        for (id in transactionDao.recentCategoryIds(kind)) {
            if (seen.add(id) && seen.size == limit) break
        }
        return seen.toList()
    }

    suspend fun getTransaction(id: String): Transaction? = transactionDao.get(id)?.toModel()

    suspend fun save(draft: CaptureDraft, editingId: String?, draftId: String? = null): Transaction {
        val now = clock.millis()
        val rate = draft.fxRateToBase.ifBlank { "1" }
        val baseMinor = FxConvert.toBase(draft.amountMinor, draft.currency, rate, draft.baseCurrency)
        val row = if (editingId == null) {
            Transaction(
                id = draftId ?: ulid.next(),
                kind = draft.kind,
                amountMinor = draft.amountMinor,
                currency = draft.currency,
                fxRateToBase = rate,
                amountBaseMinor = baseMinor,
                categoryId = draft.categoryId,
                note = draft.note,
                occurredOn = draft.occurredOn,
                occurredAt = draft.occurredAt,
                recurringId = draft.recurringId,
                receiptId = draft.receiptId,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                dirty = true,
            )
        } else {
            val existing = transactionDao.get(editingId)?.toModel()
                ?: error("missing transaction $editingId")
            existing.copy(
                kind = draft.kind,
                amountMinor = draft.amountMinor,
                currency = draft.currency,
                fxRateToBase = rate,
                amountBaseMinor = baseMinor,
                categoryId = draft.categoryId,
                note = draft.note,
                occurredOn = draft.occurredOn,
                occurredAt = draft.occurredAt,
                receiptId = draft.receiptId ?: existing.receiptId,
                updatedAt = now,
                dirty = true,
            )
        }
        val entity = TransactionEntity.from(row)
        if (editingId == null) transactionDao.insert(entity) else transactionDao.update(entity)
        return row
    }

    fun observeActive(): Flow<List<Transaction>> =
        transactionDao.observeActive().map { rows -> rows.map { it.toModel() } }

    suspend fun usedCurrencies(): List<String> = transactionDao.distinctCurrencies()

    suspend fun importRows(rows: List<CsvRow>, baseCurrency: String, zoneId: ZoneId): Int {
        val cats = categoryDao.listAll()
        val byName = cats.associate { it.name.trim().lowercase() to it }
        var n = 0
        for (row in rows) {
            val amount = row.amount.toLongOrNull() ?: continue
            if (amount == 0L) continue
            val date = row.date
            if (!date.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) continue
            val kind = when (row.kind.trim().lowercase()) {
                TransactionKind.INCOME -> TransactionKind.INCOME
                TransactionKind.ADJUSTMENT -> TransactionKind.ADJUSTMENT
                else -> TransactionKind.EXPENSE
            }
            val fallbackId = if (kind == TransactionKind.INCOME) "cat_other_in" else "cat_other"
            val cat = byName[row.category.trim().lowercase()] ?: cats.firstOrNull { it.id == fallbackId }
            val categoryId = cat?.id ?: fallbackId
            val currency = row.currency.ifBlank { baseCurrency }
            save(
                CaptureDraft(
                    kind = kind,
                    amountMinor = amount,
                    currency = currency,
                    categoryId = categoryId,
                    note = row.note.take(80),
                    occurredOn = date,
                    occurredAt = occurredAtForDate(date, zoneId),
                    fxRateToBase = "1",
                    baseCurrency = baseCurrency,
                ),
                editingId = null,
            )
            n++
        }
        return n
    }

    suspend fun setReceiptId(transactionId: String, receiptId: String) {
        val existing = transactionDao.get(transactionId) ?: return
        val now = clock.millis()
        transactionDao.update(existing.copy(receiptId = receiptId, updatedAt = now, dirty = true))
    }

    suspend fun applyOcrAmount(transactionId: String, amountMinor: Long) {
        val existing = transactionDao.get(transactionId) ?: return
        val now = clock.millis()
        transactionDao.update(
            existing.copy(
                amountMinor = amountMinor,
                amountBaseMinor = amountMinor,
                updatedAt = now,
                dirty = true,
            ),
        )
    }

    suspend fun softDelete(id: String): Transaction? {
        val existing = transactionDao.get(id) ?: return null
        val now = clock.millis()
        val updated = existing.copy(deletedAt = now, updatedAt = now, dirty = true)
        transactionDao.update(updated)
        return updated.toModel()
    }

    suspend fun restore(id: String): Transaction? {
        val existing = transactionDao.get(id) ?: return null
        val now = clock.millis()
        val updated = existing.copy(deletedAt = null, updatedAt = now, dirty = true)
        transactionDao.update(updated)
        return updated.toModel()
    }

    suspend fun duplicateNow(id: String, zoneId: ZoneId): Transaction? {
        val src = transactionDao.get(id)?.toModel() ?: return null
        val now = clock.millis()
        val today = LocalDate.now(clock.withZone(zoneId))
        val copy = src.copy(
            id = ulid.next(),
            occurredOn = today.toString(),
            occurredAt = now,
            recurringId = null,
            receiptId = null,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            dirty = true,
        )
        transactionDao.insert(TransactionEntity.from(copy))
        return copy
    }

    suspend fun monthSpent(yearMonth: YearMonth): Long {
        val start = yearMonth.atDay(1).toString()
        val end = yearMonth.atEndOfMonth().toString()
        return transactionDao.sumExpenses(start, end)
    }

    fun todayOn(zoneId: ZoneId): String =
        LocalDate.now(clock.withZone(zoneId)).toString()

    fun yesterdayOn(zoneId: ZoneId): String =
        LocalDate.now(clock.withZone(zoneId)).minusDays(1).toString()

    fun occurredAtForDate(occurredOn: String, zoneId: ZoneId): Long {
        val today = LocalDate.now(clock.withZone(zoneId))
        val date = runCatching { LocalDate.parse(occurredOn) }.getOrNull() ?: today
        return if (date == today) {
            clock.millis()
        } else {
            date.atTime(LocalTime.NOON).atZone(zoneId).toInstant().toEpochMilli()
        }
    }

    val allCategories: Flow<List<Category>> =
        categoryDao.observeAll().map { rows -> rows.map { it.toModel() } }

    fun observeMonth(yearMonth: YearMonth): Flow<List<com.flla.wherego.core.model.CategorySpend>> = combine(
        transactionDao.observeActive(),
        categoryDao.observeAll(),
    ) { txs, cats ->
        val catMap = cats.associate { it.id to it.toModel() }
        val rows = MonthSpend.byCategory(txs.map { it.toModel() }, yearMonth)
            .map { (categoryId, amountMinor) ->
                val cat = catMap[categoryId]
                SpendRow(
                    categoryId = categoryId,
                    name = cat?.name ?: "Other",
                    emoji = cat?.emoji ?: "📦",
                    colorHex = cat?.colorHex ?: PresetCategories.ACCENT_HEX,
                    amountMinor = amountMinor,
                )
            }
        MonthStory.aggregate(rows)
    }

    suspend fun currentBalance(startingBalanceMinor: Long): Long {
        val txs = transactionDao.listActive()
        val income = txs.filter { it.kind == TransactionKind.INCOME }.sumOf { it.amountBaseMinor }
        val expense = txs.filter { it.kind == TransactionKind.EXPENSE }.sumOf { it.amountBaseMinor }
        val adj = txs.filter { it.kind == TransactionKind.ADJUSTMENT }.sumOf { it.amountBaseMinor }
        return startingBalanceMinor + income - expense + adj
    }

    suspend fun setBalanceTo(
        targetMinor: Long,
        startingBalanceMinor: Long,
        currency: String,
        zoneId: ZoneId,
    ) {
        val current = currentBalance(startingBalanceMinor)
        val delta = targetMinor - current
        if (delta == 0L) return
        val today = todayOn(zoneId)
        save(
            CaptureDraft(
                kind = TransactionKind.ADJUSTMENT,
                amountMinor = delta,
                currency = currency,
                categoryId = "cat_other",
                note = "Set balance",
                occurredOn = today,
                occurredAt = clock.millis(),
            ),
            editingId = null,
        )
    }

    suspend fun createCategory(
        name: String,
        emoji: String,
        colorHex: String,
        kind: String = TransactionKind.EXPENSE,
    ): String {
        val id = "cat_custom_${ulid.next()}"
        val now = clock.millis()
        val all = categoryDao.listAll()
        val nextOrder = (all.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val entity = CategoryEntity(
            id = id,
            name = name.trim().ifBlank { "Custom" },
            emoji = emoji.trim().ifBlank { "✨" },
            colorHex = colorHex.ifBlank { PresetCategories.ACCENT_HEX },
            kind = kind,
            isPreset = false,
            archived = false,
            sortOrder = nextOrder,
            updatedAt = now,
            deletedAt = null,
        )
        categoryDao.upsert(entity)
        return id
    }

    suspend fun pinCategoryToTop(id: String) {
        val target = categoryDao.get(id) ?: return
        val allActive = categoryDao.listActive().filter { it.kind == target.kind || it.kind == "both" }
        val minOrder = allActive.minOfOrNull { it.sortOrder } ?: 0
        categoryDao.update(
            target.copy(
                sortOrder = minOrder - 1,
                updatedAt = clock.millis(),
            ),
        )
    }

    suspend fun updateCategory(
        id: String,
        name: String,
        emoji: String,
        colorHex: String,
        kind: String? = null,
    ) {
        val existing = categoryDao.get(id) ?: return
        categoryDao.update(
            existing.copy(
                name = name.trim().ifBlank { existing.name },
                emoji = emoji.trim().ifBlank { existing.emoji },
                colorHex = colorHex.ifBlank { existing.colorHex },
                kind = kind ?: existing.kind,
                updatedAt = clock.millis(),
            ),
        )
    }

    suspend fun archiveCategory(id: String, archived: Boolean) {
        val existing = categoryDao.get(id) ?: return
        categoryDao.update(
            existing.copy(archived = archived, updatedAt = clock.millis()),
        )
    }

    suspend fun transactionCountForCategory(id: String): Int = transactionDao.countForCategory(id)




    private fun assembleHome(
        txs: List<Transaction>,
        catMap: Map<String, Category>,
        zoneId: ZoneId,
    ): HomeLedger {
        val today = LocalDate.now(clock.withZone(zoneId))
        val month = YearMonth.from(today)
        val weekStart = today.with(DayOfWeek.MONDAY)
        val monthStart = month.atDay(1).toString()
        val monthEnd = month.atEndOfMonth().toString()
        val todayOn = today.toString()
        val weekStartOn = weekStart.toString()

        fun wrap(tx: Transaction) = HomeTx(tx, catMap[tx.categoryId])

        val monthSpent = MonthSpend.total(txs, month)
        val monthIncome = txs
            .filter { it.kind == TransactionKind.INCOME }
            .filter { it.occurredOn >= monthStart && it.occurredOn <= monthEnd }
            .sumOf { it.amountBaseMinor }
        val weekLogged = txs.count { it.occurredOn >= weekStartOn && it.occurredOn <= todayOn }

        val todayTxs = txs.filter { it.occurredOn == todayOn }
            .sortedWith(compareByDescending<Transaction> { it.occurredAt ?: 0L }.thenByDescending { it.createdAt })
        val todayExpense = todayTxs
            .filter { it.kind == TransactionKind.EXPENSE }
            .sumOf { it.amountBaseMinor }

        val earlier = txs.filter { on ->
            on.occurredOn >= weekStartOn && on.occurredOn < todayOn
        }.sortedWith(
            compareByDescending<Transaction> { it.occurredOn }
                .thenByDescending { it.occurredAt ?: 0L },
        )

        return HomeLedger(
            monthSpentMinor = monthSpent,
            monthIncomeMinor = monthIncome,
            weekLoggedCount = weekLogged,
            todayExpenseMinor = todayExpense,
            today = todayTxs.map(::wrap),
            earlierThisWeek = earlier.map(::wrap),
            streakDays = com.flla.wherego.core.model.LogStreak.distinctDays(txs.map { it.occurredOn }),
            hasTxToday = todayTxs.isNotEmpty(),
        )
    }
}

fun zoneOf(profile: UserProfile?): ZoneId =
    ZoneId.of(profile?.timeZoneId ?: UserProfile.DEFAULT_ZONE)
