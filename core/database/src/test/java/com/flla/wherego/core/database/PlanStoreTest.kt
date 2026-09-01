package com.flla.wherego.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.flla.wherego.core.common.UlidGenerator
import com.flla.wherego.core.model.Recurrence
import com.flla.wherego.core.model.TransactionKind
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlanStoreTest {
    private lateinit var db: WheregoDatabase
    private lateinit var store: PlanStore
    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WheregoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = PlanStore(
            budgetDao = db.budgetDao(),
            recurringDao = db.recurringDao(),
            transactionDao = db.transactionDao(),
            categoryDao = db.categoryDao(),
            goalDao = db.goalDao(),
            ulid = UlidGenerator(),
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * A second cap for a category must land on the same row. Minting a new id instead left two
     * cards for one category and counted both in the month's cap total.
     */
    @Test
    fun oneCapPerCategoryPerMonth() = runBlocking {
        store.setBudget("cat_food_out", 500_000L, "IDR", "2026-09")
        store.setBudget("cat_food_out", 800_000L, "IDR", "2026-09")

        val september = store.observeBudgets("2026-09").first()
        assertEquals(1, september.size)
        assertEquals(800_000L, september.first().amountMinor)
        assertEquals(800_000L, september.sumOf { it.amountMinor })

        // A different category is a different cap, and so is the same category in another month.
        store.setBudget("cat_transport", 300_000L, "IDR", "2026-09")
        store.setBudget("cat_food_out", 100_000L, "IDR", "2026-10")
        assertEquals(2, store.observeBudgets("2026-09").first().size)
        assertEquals(1, store.observeBudgets("2026-10").first().size)
    }

    /** Overall (`categoryId == null`) is a cap like any other: one per month, not one per tap. */
    @Test
    fun overallCapIsAlsoSingle() = runBlocking {
        store.setBudget(null, 3_000_000L, "IDR", "2026-09")
        store.setBudget(null, 3_400_000L, "IDR", "2026-09")

        val rows = store.observeBudgets("2026-09").first()
        assertEquals(1, rows.size)
        assertNull(rows.first().categoryId)
        assertEquals(3_400_000L, rows.first().amountMinor)
    }

    @Test
    fun movingACapToAnotherCategoryLeavesOneRow() = runBlocking {
        store.setBudget("cat_food_out", 500_000L, "IDR", "2026-09")
        val original = store.observeBudgets("2026-09").first().single()

        store.setBudget("cat_transport", 250_000L, "IDR", "2026-09", replacedId = original.id)

        val rows = store.observeBudgets("2026-09").first()
        assertEquals(1, rows.size)
        assertEquals("cat_transport", rows.first().categoryId)
        assertEquals(250_000L, rows.first().amountMinor)
    }

    /** Editing onto a category that already has a cap merges into it rather than duplicating. */
    @Test
    fun movingACapOntoAnExistingCapMerges() = runBlocking {
        store.setBudget("cat_food_out", 500_000L, "IDR", "2026-09")
        store.setBudget("cat_transport", 250_000L, "IDR", "2026-09")
        val food = store.observeBudgets("2026-09").first().single { it.categoryId == "cat_food_out" }

        store.setBudget("cat_transport", 400_000L, "IDR", "2026-09", replacedId = food.id)

        val rows = store.observeBudgets("2026-09").first()
        assertEquals(1, rows.size)
        assertEquals("cat_transport", rows.first().categoryId)
        assertEquals(400_000L, rows.first().amountMinor)
    }

    /**
     * An edited bill moves its next hit and the day of month later hits land on; `startOn` is
     * history and must not move.
     */
    @Test
    fun updateRuleMovesNextDueAndKeepsStartOn() = runBlocking {
        val created = store.newRule(
            kind = TransactionKind.EXPENSE,
            amountMinor = 25_000L,
            currency = "IDR",
            categoryId = "cat_bills",
            note = "Wifi",
            freq = Recurrence.MONTHLY,
            dayOfMonth = 26,
            weekday = null,
            firstOn = LocalDate.parse("2026-09-26"),
        )

        val updated = store.updateRule(
            id = created.id,
            amountMinor = 30_000L,
            categoryId = "cat_other",
            note = "Wifi + TV",
            nextOn = LocalDate.parse("2026-10-05"),
        )

        assertEquals(30_000L, updated?.amountMinor)
        assertEquals("cat_other", updated?.categoryId)
        assertEquals("Wifi + TV", updated?.note)
        assertEquals("2026-10-05", updated?.nextOn)
        assertEquals(5, updated?.dayOfMonth)
        assertEquals("2026-09-26", updated?.startOn)
        assertEquals(created.id, store.observeRules().first().single().id)
        assertNull(store.updateRule("nope", 1L, "cat_other", "", LocalDate.parse("2026-10-05")))
    }

    /** The goal keeps the currency it was created in, and a blank name never wipes the old one. */
    @Test
    fun updateGoalKeepsCurrencyAndName() = runBlocking {
        val goal = store.addGoal("Umrah", 1_000_000L, "IDR", 5_000_000L)

        store.updateGoal(goal.id, "  ", 2_000_000L, 6_000_000L)

        val stored = store.observeGoals().first().single()
        assertEquals("Umrah", stored.name)
        assertEquals("IDR", stored.currency)
        assertEquals(2_000_000L, stored.allocatedMinor)
        assertEquals(6_000_000L, stored.targetMinor)

        store.updateGoal(goal.id, "Umrah 2027", 2_000_000L, 6_000_000L)
        assertEquals("Umrah 2027", store.observeGoals().first().single().name)
    }
}
