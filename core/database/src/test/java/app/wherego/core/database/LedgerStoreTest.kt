package app.wherego.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.wherego.core.common.UlidGenerator
import app.wherego.core.model.TransactionKind
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LedgerStoreTest {
    private lateinit var db: WheregoDatabase
    private lateinit var store: LedgerStore
    private val zone: ZoneId = ZoneId.of("Asia/Jakarta")
    private val clock: Clock = Clock.fixed(Instant.parse("2026-08-12T10:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WheregoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = LedgerStore(
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            ulid = UlidGenerator(),
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertMonthAggregateAndSoftDelete() = runBlocking {
        store.seedCategoriesIfEmpty()
        store.seedCategoriesIfEmpty()
        assertEquals(14, db.categoryDao().count())

        val saved = store.save(
            CaptureDraft(
                kind = TransactionKind.EXPENSE,
                amountMinor = 18_000L,
                currency = "IDR",
                categoryId = "cat_food_out",
                note = "Warteg",
                occurredOn = "2026-08-12",
                occurredAt = clock.millis(),
            ),
            editingId = null,
        )
        assertEquals(18_000L, store.monthSpent(YearMonth.of(2026, 8)))
        assertEquals(0L, store.monthSpent(YearMonth.of(2026, 7)))

        val deleted = store.softDelete(saved.id)
        assertNotNull(deleted?.deletedAt)
        assertEquals(0L, store.monthSpent(YearMonth.of(2026, 8)))

        val restored = store.restore(saved.id)
        assertNull(restored?.deletedAt)
        assertEquals(18_000L, store.monthSpent(YearMonth.of(2026, 8)))
    }

    @Test
    fun duplicateNowCreatesNewRowToday() = runBlocking {
        store.seedCategoriesIfEmpty()
        val original = store.save(
            CaptureDraft(
                kind = TransactionKind.EXPENSE,
                amountMinor = 22_000L,
                currency = "IDR",
                categoryId = "cat_transport",
                note = "Gojek",
                occurredOn = "2026-08-10",
                occurredAt = clock.millis(),
            ),
            editingId = null,
        )
        val copy = store.duplicateNow(original.id, zone)!!
        assertEquals(22_000L, copy.amountMinor)
        assertEquals("2026-08-12", copy.occurredOn)
        assertEquals(true, copy.dirty)
        assertEquals(44_000L, store.monthSpent(YearMonth.of(2026, 8)))
    }
}
