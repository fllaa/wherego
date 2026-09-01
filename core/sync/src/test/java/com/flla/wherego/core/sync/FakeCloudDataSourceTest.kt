package com.flla.wherego.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.sync.CloudDataSource.Companion.NO_CURSOR
import com.flla.wherego.core.sync.CloudDataSource.Companion.SYNCED_AT
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Firestore needs a live project, so this fake is the only executable implementation of the
 * transport contract. It has to answer the cursor questions the same way, or debug builds and
 * release builds disagree about which rows a device has already seen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FakeCloudDataSourceTest {
    private lateinit var context: Context
    private lateinit var cloud: FakeCloudDataSource

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "wherego-cloud").deleteRecursively()
        cloud = FakeCloudDataSource(context)
    }

    /**
     * The defect this contract exists to prevent: a peer parked rows offline, so they carry an
     * `updatedAt` older than anything we have pulled. Pushed late, they must still arrive.
     */
    @Test
    fun aRowAuthoredLongAgoStillArrivesWhenItIsPushedLate() = runBlocking {
        cloud.pushTransactions(UID, listOf(transaction(id = "tx-a", updatedAt = 5_000L, dirty = true)))
        val first = cloud.pullTransactions(UID, NO_CURSOR)
        assertEquals(listOf("tx-a"), first.rows.map { it.id })
        assertTrue(first.cursor > NO_CURSOR)

        cloud.pushTransactions(UID, listOf(transaction(id = "tx-b", updatedAt = 1_000L, dirty = false)))
        val second = cloud.pullTransactions(UID, first.cursor)

        assertEquals(listOf("tx-b"), second.rows.map { it.id })
        assertTrue(second.cursor > first.cursor)
    }

    @Test
    fun aSettledRowIsNotDeliveredAgain() = runBlocking {
        cloud.pushTransactions(UID, listOf(transaction(id = "tx-a", updatedAt = 5_000L, dirty = true)))
        val first = cloud.pullTransactions(UID, NO_CURSOR)

        val second = cloud.pullTransactions(UID, first.cursor)

        assertTrue(second.rows.isEmpty())
        assertEquals("an empty page must not move the watermark", first.cursor, second.cursor)
    }

    /** The cursor tracks write order, not the order rows were authored in. */
    @Test
    fun cursorFollowsWriteOrderNotUpdatedAt() = runBlocking {
        cloud.pushTransactions(
            UID,
            listOf(
                transaction(id = "tx-new", updatedAt = 9_000L, dirty = true),
                transaction(id = "tx-old", updatedAt = 1_000L, dirty = true),
            ),
        )

        val page = cloud.pullTransactions(UID, NO_CURSOR)

        assertEquals(listOf("tx-new", "tx-old"), page.rows.map { it.id })
    }

    /**
     * Rows written before `syncedAt` existed match no range filter, so the first pull reads the
     * collection whole and stamps them. They come back once more on the pull after that — an
     * idempotent upsert — and then settle.
     */
    @Test
    fun preCursorRowsAreStampedThenSettle() = runBlocking {
        writeUnstamped(transaction(id = "tx-legacy", updatedAt = 1_000L, dirty = false))

        val first = cloud.pullTransactions(UID, NO_CURSOR)
        assertEquals(listOf("tx-legacy"), first.rows.map { it.id })
        assertEquals("nothing was stamped when the page was read", NO_CURSOR, first.cursor)

        val second = cloud.pullTransactions(UID, first.cursor)
        assertEquals(listOf("tx-legacy"), second.rows.map { it.id })
        assertTrue(second.cursor > NO_CURSOR)

        val third = cloud.pullTransactions(UID, second.cursor)
        assertTrue(third.rows.isEmpty())
    }

    @Test
    fun profileRoundTripsWithoutACursor() = runBlocking {
        assertEquals(null, cloud.pullProfile(UID))

        cloud.pushProfile(UID, profile(id = "me", onboarded = true, updatedAt = 4_000L))

        val stored = cloud.pullProfile(UID)
        assertEquals("me", stored?.id)
        assertTrue(stored!!.onboardingDone)
    }

    @Test
    fun deleteAllLeavesNothingToPull() = runBlocking {
        cloud.pushTransactions(UID, listOf(transaction(id = "tx-a", updatedAt = 5_000L, dirty = true)))
        cloud.pushCategories(UID, listOf(category(id = "cat_food_out", archived = false, updatedAt = 1L)))
        cloud.pushProfile(UID, profile(id = "me", onboarded = true, updatedAt = 4_000L))

        cloud.deleteAll(UID)

        assertTrue(cloud.pullTransactions(UID, NO_CURSOR).rows.isEmpty())
        assertTrue(cloud.pullCategories(UID, NO_CURSOR).rows.isEmpty())
        assertEquals(null, cloud.pullProfile(UID))
    }

    /** A document as it was stored before the transport carried a server stamp. */
    private fun writeUnstamped(row: Transaction) {
        val dir = File(context.filesDir, "wherego-cloud/$UID/transactions").also { it.mkdirs() }
        val json = CloudCodec.transaction(row)
        assertFalse(json.has(SYNCED_AT))
        File(dir, "${row.id}.json").writeText(json.toString())
    }

    private companion object {
        const val UID = "uid-1"
    }
}
