package app.wherego.core.database

import app.wherego.core.common.UlidGenerator
import app.wherego.core.model.Category
import app.wherego.core.model.PresetCategories
import app.wherego.core.model.Transaction
import app.wherego.core.model.TransactionKind
import app.wherego.core.model.UserProfile
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
    val todayExpenseMinor: Long,
    val today: List<HomeTx>,
    val earlierThisWeek: List<HomeTx>,
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

    suspend fun save(draft: CaptureDraft, editingId: String?): Transaction {
        val now = clock.millis()
        val row = if (editingId == null) {
            Transaction(
                id = ulid.next(),
                kind = draft.kind,
                amountMinor = draft.amountMinor,
                currency = draft.currency,
                fxRateToBase = "1",
                amountBaseMinor = draft.amountMinor,
                categoryId = draft.categoryId,
                note = draft.note,
                occurredOn = draft.occurredOn,
                occurredAt = draft.occurredAt,
                recurringId = null,
                receiptId = null,
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
                amountBaseMinor = draft.amountMinor,
                categoryId = draft.categoryId,
                note = draft.note,
                occurredOn = draft.occurredOn,
                occurredAt = draft.occurredAt,
                updatedAt = now,
                dirty = true,
            )
        }
        val entity = TransactionEntity.from(row)
        if (editingId == null) transactionDao.insert(entity) else transactionDao.update(entity)
        return row
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
        val date = LocalDate.parse(occurredOn)
        return if (date == today) {
            clock.millis()
        } else {
            date.atTime(LocalTime.NOON).atZone(zoneId).toInstant().toEpochMilli()
        }
    }



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

        val monthSpent = txs
            .filter { it.kind == TransactionKind.EXPENSE }
            .filter { it.occurredOn >= monthStart && it.occurredOn <= monthEnd }
            .sumOf { it.amountBaseMinor }

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
            todayExpenseMinor = todayExpense,
            today = todayTxs.map(::wrap),
            earlierThisWeek = earlier.map(::wrap),
        )
    }
}

fun zoneOf(profile: UserProfile?): ZoneId =
    ZoneId.of(profile?.timeZoneId ?: UserProfile.DEFAULT_ZONE)
