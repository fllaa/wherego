package com.flla.wherego.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.flla.wherego.core.common.UlidGenerator
import com.flla.wherego.core.model.TransactionKind
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalDataEraserTest {
    private lateinit var context: Context
    private lateinit var db: WheregoDatabase
    private lateinit var profiles: UserProfileStore
    private lateinit var ledger: LedgerStore
    private lateinit var eraser: RoomLocalDataEraser
    private val clock: Clock = Clock.fixed(Instant.parse("2026-08-12T10:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WheregoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        profiles = UserProfileStore(
            dao = db.userProfileDao(),
            ulid = UlidGenerator(),
            clock = clock,
        )
        ledger = LedgerStore(
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            ulid = UlidGenerator(),
            clock = clock,
        )
        eraser = RoomLocalDataEraser(
            context = context,
            db = db,
            profiles = profiles,
            ledger = ledger,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun resetToGuestWipesDataAndReseedsGuest() = runBlocking {
        ledger.seedCategoriesIfEmpty()
        ledger.save(
            draft = CaptureDraft(
                kind = TransactionKind.EXPENSE,
                amountMinor = 18_000L,
                currency = "IDR",
                categoryId = "cat_food_out",
                note = "Lunch",
                occurredOn = "2026-08-12",
                occurredAt = 1723456800000L,
            ),
            editingId = null,
        )
        profiles.ensureGuest()
        profiles.completeOnboarding("IDR", null)

        val stubReceipt = File(ReceiptFiles.dir(context), "stub.jpg").apply {
            writeText("dummy receipt")
        }
        assertTrue(stubReceipt.exists())

        eraser.resetToGuest()

        assertEquals(emptyList<TransactionEntity>(), db.transactionDao().listActive())
        val profile = db.userProfileDao().get()
        assertNotNull(profile)
        assertFalse(profile!!.onboardingDone)
        assertEquals(0L, profile.startingBalanceMinor)
        assertEquals(14, db.categoryDao().count())
        assertFalse(stubReceipt.exists())
    }
}
