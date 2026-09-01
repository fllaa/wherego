package com.flla.wherego.core.sync

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.flla.wherego.core.database.CategoryDao
import com.flla.wherego.core.database.CategoryEntity
import com.flla.wherego.core.database.SyncStateDao
import com.flla.wherego.core.database.SyncStateEntity
import com.flla.wherego.core.database.TransactionDao
import com.flla.wherego.core.database.TransactionEntity
import com.flla.wherego.core.database.UserProfileDao
import com.flla.wherego.core.database.UserProfileEntity
import com.flla.wherego.core.datastore.ThemePreferences
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncEngineTest {
    private lateinit var preferences: ThemePreferences

    @Before
    fun setUp() = runBlocking {
        preferences = ThemePreferences(ApplicationProvider.getApplicationContext<Context>())
        preferences.clear()
    }

    @Test
    fun reinstallAdoptsOnboardedCloudAndDoesNotClobberIt() = runBlocking {
        val local = profile(id = "guest-now", onboarded = false, updatedAt = 9_000L)
        val remote = profile(id = "cloud-old", onboarded = true, updatedAt = 1_000L)
        val seed = category(id = "cat_food_out", archived = false, updatedAt = 9_000L)
        val cloudCat = category(id = "cat_food_out", archived = true, updatedAt = 1_000L)

        val profiles = MemProfiles(UserProfileEntity.from(local))
        val categories = MemCategories(CategoryEntity.from(seed))
        val cloud = MemCloud(remote, listOf(cloudCat))
        val engine = engine(profiles, categories, cloud)

        assertTrue(engine.sync())
        assertTrue(profiles.row!!.onboardingDone)
        assertEquals(1_000L, profiles.row!!.startingBalanceMinor)
        assertTrue(cloud.profile!!.onboardingDone)
        assertEquals(1_000L, cloud.profile!!.startingBalanceMinor)
        assertTrue(categories.row("cat_food_out")!!.archived)
    }

    @Test
    fun newGoogleUserStaysOnTour() = runBlocking {
        val local = profile(id = "guest-now", onboarded = false, updatedAt = 9_000L)
        val profiles = MemProfiles(UserProfileEntity.from(local))
        val cloud = MemCloud(profile = null)
        val engine = engine(profiles, MemCategories(), cloud)

        assertFalse(engine.sync())
        assertFalse(profiles.row!!.onboardingDone)
        assertFalse(cloud.profile!!.onboardingDone)
    }

    /**
     * Two phones, each holding transactions parked while signed out. Whichever signs in first
     * pulls before the other has pushed, so the peer's backlog only ever arrives on a later pass
     * — and it is stamped `updatedAt` from before that first pull. A watermark read off the local
     * clock discards it forever; the server cursor does not.
     */
    @Test
    fun aPeerBacklogArrivesEvenThoughItPredatesOurFirstPull() = runBlocking {
        val ours = transaction(id = "tx-a", updatedAt = 5_000L, dirty = true)
        val theirs = transaction(id = "tx-b", updatedAt = 1_000L, dirty = false)
        val txs = MemTx(TransactionEntity.from(ours))
        val cloud = MemCloud(profile = null)
        val engine = engine(MemProfiles.onboarded(), MemCategories(), cloud, txs)

        engine.sync()
        assertNull("peer has not signed in yet", txs.rows["tx-b"])
        assertFalse("our own row was accepted by the cloud", txs.rows.getValue("tx-a").dirty)

        cloud.pushTransactions("uid-1", listOf(theirs))
        engine.sync()

        assertNotNull("peer backlog must survive the cursor", txs.rows["tx-b"])
        assertEquals(1_000L, txs.rows.getValue("tx-b").updatedAt)
        assertFalse(txs.rows.getValue("tx-b").dirty)
    }

    /** A row the cursor already covered is not pulled a second time. */
    @Test
    fun aSettledRowIsNotPulledTwice() = runBlocking {
        val theirs = transaction(id = "tx-b", updatedAt = 1_000L, dirty = false)
        val txs = MemTx()
        val cloud = MemCloud(profile = null)
        cloud.pushTransactions("uid-1", listOf(theirs))
        val engine = engine(MemProfiles.onboarded(), MemCategories(), cloud, txs)

        engine.sync()
        assertEquals(1, cloud.transactionReads)

        engine.sync()
        assertEquals(0, cloud.transactionReads)
    }

    /**
     * `dirty` may only drop for the copy that actually reached the cloud. An edit landing while
     * the batch is in flight leaves a newer row behind that still owes a push.
     */
    @Test
    fun anEditDuringThePushKeepsTheRowDirty() = runBlocking {
        val row = transaction(id = "tx-1", updatedAt = 100L, dirty = true)
        val txs = MemTx(TransactionEntity.from(row))
        val cloud = MemCloud(profile = null)
        cloud.onPush = {
            txs.rows["tx-1"] = TransactionEntity.from(
                row.copy(amountMinor = 5_000L, updatedAt = 200L, dirty = true),
            )
        }

        engine(MemProfiles.onboarded(), MemCategories(), cloud, txs).sync()

        val stored = txs.rows.getValue("tx-1")
        assertTrue("mid-flight edit still owes the cloud a copy", stored.dirty)
        assertEquals(200L, stored.updatedAt)
        assertEquals(5_000L, stored.amountMinor)
    }

    /**
     * Another device asserted a total and it took over the balance. The arithmetic is right, but
     * the figure the user was shown changed, so Home gets one question to overrule it.
     */
    @Test
    fun aPeerAssertionThatMovesTheBalanceRaisesAQuestion() = runBlocking {
        val mine = reconcile(id = "mine", on = "2026-08-01", total = 5_000_000L)
        val theirs = reconcile(id = "theirs", on = "2026-08-20", total = 4_800_000L)
        val txs = MemTx(TransactionEntity.from(mine))
        val cloud = MemCloud(profile = null)
        cloud.pushTransactions("uid-1", listOf(theirs))

        engine(MemProfiles.onboarded(), MemCategories(), cloud, txs).sync()

        assertEquals("mine" to "theirs", preferences.balanceConflict.first())
    }

    /** Two devices that assert the same total agree. There is nothing to ask about. */
    @Test
    fun aPeerAssertionThatAgreesRaisesNothing() = runBlocking {
        val mine = reconcile(id = "mine", on = "2026-08-01", total = 5_000_000L)
        val theirs = reconcile(id = "theirs", on = "2026-08-20", total = 5_000_000L)
        val txs = MemTx(TransactionEntity.from(mine))
        val cloud = MemCloud(profile = null)
        cloud.pushTransactions("uid-1", listOf(theirs))

        engine(MemProfiles.onboarded(), MemCategories(), cloud, txs).sync()

        assertNull(preferences.balanceConflict.first())
    }

    private fun engine(
        profiles: MemProfiles,
        categories: MemCategories,
        cloud: MemCloud,
        transactions: MemTx = MemTx(),
    ) = SyncEngine(
        auth = FixedAuth("uid-1"),
        cloud = cloud,
        transactions = transactions,
        categories = categories,
        profiles = profiles,
        syncState = MemSyncState(),
        preferences = preferences,
    )
}

private class FixedAuth(uid: String) : AuthRepository {
    private val snap = AuthState(firebaseUid = uid, signedIn = true)
    override val state: Flow<AuthState> = MutableStateFlow(snap)
    override suspend fun current(): AuthState = snap
    override suspend fun signIn(activity: Activity): Result<AuthState> = Result.success(snap)
    override suspend fun signOut() = Unit
    override suspend fun deleteAccount(activity: Activity): Result<Unit> = Result.success(Unit)
}

private class Stamped<T>(val row: T, val syncedAt: Long)

/**
 * Stands in for Firestore. Its `syncedAt` clock is its own — strictly increasing per write and
 * unrelated to any row's `updatedAt`, exactly like a server timestamp — so a row authored long
 * ago still arrives at the back of the queue when it is finally pushed.
 */
private class MemCloud(
    var profile: UserProfile?,
    seedCategories: List<Category> = emptyList(),
) : CloudDataSource {
    private var serverClock = 0L
    private val transactions = LinkedHashMap<String, Stamped<Transaction>>()
    private val categories = LinkedHashMap<String, Stamped<Category>>()

    /** Rows the last transaction pull delivered — a pull that re-reads settled rows is a bug. */
    var transactionReads = 0
        private set

    /** Runs while a transaction batch is "in flight", to model a concurrent local edit. */
    var onPush: (() -> Unit)? = null

    init {
        seedCategories.forEach { categories[it.id] = Stamped(it, ++serverClock) }
    }

    override val available: Boolean = true

    override suspend fun pushTransactions(uid: String, rows: List<Transaction>) {
        rows.forEach { transactions[it.id] = Stamped(it, ++serverClock) }
        onPush?.invoke()
    }

    override suspend fun pullTransactions(uid: String, sinceCursor: Long): CloudPage<Transaction> =
        page(transactions, sinceCursor).also { transactionReads = it.rows.size }

    override suspend fun pushCategories(uid: String, rows: List<Category>) {
        rows.forEach { categories[it.id] = Stamped(it, ++serverClock) }
    }

    override suspend fun pullCategories(uid: String, sinceCursor: Long): CloudPage<Category> =
        page(categories, sinceCursor)

    override suspend fun pushProfile(uid: String, profile: UserProfile) {
        this.profile = profile
    }

    override suspend fun pullProfile(uid: String): UserProfile? = profile

    override suspend fun deleteAll(uid: String) {
        profile = null
        transactions.clear()
        categories.clear()
    }

    private fun <T> page(store: Map<String, Stamped<T>>, since: Long): CloudPage<T> {
        val fresh = store.values.filter { it.syncedAt > since }.sortedBy { it.syncedAt }
        return CloudPage(fresh.map { it.row }, fresh.lastOrNull()?.syncedAt ?: since)
    }
}

private class MemProfiles(var row: UserProfileEntity?) : UserProfileDao {
    override suspend fun get(): UserProfileEntity? = row
    override fun observe(): Flow<UserProfileEntity?> = MutableStateFlow(row)
    override suspend fun insert(row: UserProfileEntity) {
        this.row = row
    }
    override suspend fun update(row: UserProfileEntity) {
        this.row = row
    }

    companion object {
        fun onboarded() = MemProfiles(
            UserProfileEntity.from(profile(id = "me", onboarded = true, updatedAt = 9_000L)),
        )
    }
}

private class MemCategories(
    vararg initial: CategoryEntity,
) : CategoryDao {
    private val rows = initial.associateBy { it.id }.toMutableMap()
    fun row(id: String): CategoryEntity? = rows[id]
    override suspend fun count(): Int = rows.size
    override suspend fun insertAll(rows: List<CategoryEntity>) {
        rows.forEach { this.rows[it.id] = it }
    }
    override fun observeActive(): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
    override suspend fun listActive(): List<CategoryEntity> = rows.values.filter { !it.archived }
    override suspend fun get(id: String): CategoryEntity? = rows[id]
    override suspend fun update(row: CategoryEntity) {
        rows[row.id] = row
    }
    override suspend fun upsert(row: CategoryEntity) {
        rows[row.id] = row
    }
    override fun observeAll(): Flow<List<CategoryEntity>> = MutableStateFlow(rows.values.toList())
    override suspend fun listAll(): List<CategoryEntity> = rows.values.toList()
}

private class MemTx(vararg initial: TransactionEntity) : TransactionDao {
    val rows = initial.associateBy { it.id }.toMutableMap()
    override suspend fun insert(row: TransactionEntity) {
        rows[row.id] = row
    }
    override suspend fun update(row: TransactionEntity) {
        rows[row.id] = row
    }
    override suspend fun upsert(row: TransactionEntity) {
        rows[row.id] = row
    }
    override suspend fun listDirty(): List<TransactionEntity> = rows.values.filter { it.dirty }
    override fun observeDirtyCount(): Flow<Int> = MutableStateFlow(rows.values.count { it.dirty })
    override suspend fun get(id: String): TransactionEntity? = rows[id]
    override fun observeActive(): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
    override suspend fun listActive(): List<TransactionEntity> = rows.values.toList()
    override suspend fun recentCategoryIds(kind: String): List<String> = emptyList()
    override suspend fun sumExpenses(startOn: String, endOn: String): Long = 0
    override suspend fun countForCategory(categoryId: String): Int = 0
    override suspend fun distinctCurrencies(): List<String> = emptyList()
}

private class MemSyncState : SyncStateDao {
    private val rows = mutableMapOf<String, SyncStateEntity>()
    override suspend fun get(collection: String): SyncStateEntity? = rows[collection]
    override suspend fun upsert(row: SyncStateEntity) {
        rows[row.collection] = row
    }
}
