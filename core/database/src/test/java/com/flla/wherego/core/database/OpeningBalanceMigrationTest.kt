package com.flla.wherego.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.flla.wherego.core.model.TransactionKind
import com.flla.wherego.core.model.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Migration 10 -> 11 turns the profile's opening-balance scalar into an anchor row.
 *
 * `sync_state` aside, v10 and v11 share a schema — the migration only moves data — so a v11
 * database Room built itself is a faithful stand-in for a v10 one, and the SQL runs against real
 * tables instead of a hand-written copy that could drift.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OpeningBalanceMigrationTest {
    private lateinit var db: WheregoDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            WheregoDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun aStartingBalanceBecomesAnAnchorRowAndTheScalarIsCleared() = runBlocking {
        db.userProfileDao().insert(profile(startingBalanceMinor = 5_000_000L, on = "2026-08-20"))

        WheregoDatabase.MIGRATION_10_11.migrate(db.openHelper.writableDatabase)

        val row = db.transactionDao().listActive().single()
        assertEquals("reconcile-p1", row.id)
        assertEquals(TransactionKind.RECONCILE, row.kind)
        assertEquals(5_000_000L, row.amountMinor)
        assertEquals("the assertion is already in base currency", 5_000_000L, row.amountBaseMinor)
        assertEquals("IDR", row.currency)
        assertEquals("2026-08-20", row.occurredOn)
        assertNull(row.deletedAt)
        assertTrue("the anchor has to reach the other device", row.dirty)

        val migrated = db.userProfileDao().get()!!
        assertEquals(0L, migrated.startingBalanceMinor)
        assertNull(migrated.startingBalanceOn)
    }

    /** A profile that skipped the balance step has nothing to assert. */
    @Test
    fun aZeroBalanceWritesNoRow() = runBlocking {
        db.userProfileDao().insert(profile(startingBalanceMinor = 0L, on = null))

        WheregoDatabase.MIGRATION_10_11.migrate(db.openHelper.writableDatabase)

        assertEquals(emptyList<TransactionEntity>(), db.transactionDao().listActive())
    }

    /** Running twice must not mint a second anchor: the row id is derived from the profile. */
    @Test
    fun theMigrationIsIdempotent() = runBlocking {
        db.userProfileDao().insert(profile(startingBalanceMinor = 5_000_000L, on = "2026-08-20"))

        WheregoDatabase.MIGRATION_10_11.migrate(db.openHelper.writableDatabase)
        WheregoDatabase.MIGRATION_10_11.migrate(db.openHelper.writableDatabase)

        assertEquals(1, db.transactionDao().listActive().size)
    }

    /** No `startingBalanceOn` on disk, so the date falls back to when the profile was made. */
    @Test
    fun aMissingDateFallsBackToTheProfilesCreationDay() = runBlocking {
        // 2026-08-12T10:00:00Z
        val created = 1_786_528_800_000L
        db.userProfileDao().insert(
            profile(startingBalanceMinor = 1_000L, on = null, createdAt = created),
        )

        WheregoDatabase.MIGRATION_10_11.migrate(db.openHelper.writableDatabase)

        assertEquals("2026-08-12", db.transactionDao().listActive().single().occurredOn)
    }

    private fun profile(
        startingBalanceMinor: Long,
        on: String?,
        createdAt: Long = 1_786_528_800_000L,
    ) = UserProfileEntity(
        id = "p1",
        googleSub = null,
        email = null,
        displayName = null,
        photoUrl = null,
        baseCurrency = UserProfile.DEFAULT_CURRENCY,
        localeTag = "id-ID",
        timeZoneId = UserProfile.DEFAULT_ZONE,
        onboardingDone = true,
        startingBalanceMinor = startingBalanceMinor,
        startingBalanceOn = on,
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
