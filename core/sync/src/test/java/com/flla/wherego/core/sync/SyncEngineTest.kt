package com.flla.wherego.core.sync

import android.app.Activity
import com.flla.wherego.core.database.CategoryDao
import com.flla.wherego.core.database.CategoryEntity
import com.flla.wherego.core.database.SyncStateDao
import com.flla.wherego.core.database.SyncStateEntity
import com.flla.wherego.core.database.TransactionDao
import com.flla.wherego.core.database.TransactionEntity
import com.flla.wherego.core.database.UserProfileDao
import com.flla.wherego.core.database.UserProfileEntity
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.UserProfile
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncEngineTest {
    private val clock: Clock = Clock.fixed(Instant.parse("2026-08-30T10:00:00Z"), ZoneOffset.UTC)

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

    private fun engine(
        profiles: MemProfiles,
        categories: MemCategories,
        cloud: MemCloud,
    ) = SyncEngine(
        auth = FixedAuth("uid-1"),
        cloud = cloud,
        transactions = MemTx(),
        categories = categories,
        profiles = profiles,
        syncState = MemSyncState(),
        clock = clock,
    )
}

private fun profile(
    id: String,
    onboarded: Boolean,
    updatedAt: Long,
): UserProfile = UserProfile(
    id = id,
    googleSub = "sub",
    email = "a@b.c",
    displayName = "Aria",
    photoUrl = null,
    baseCurrency = "IDR",
    localeTag = "id-ID",
    timeZoneId = "Asia/Jakarta",
    onboardingDone = onboarded,
    startingBalanceMinor = if (onboarded) 1_000L else 0L,
    startingBalanceOn = if (onboarded) "2026-01-01" else null,
    createdAt = 1L,
    updatedAt = updatedAt,
    firebaseUid = "uid-1",
)

private fun category(
    id: String,
    archived: Boolean,
    updatedAt: Long,
): Category = Category(
    id = id,
    name = "Food out",
    emoji = "🍜",
    colorHex = "#FF6B4A",
    softColorHex = "#FFE1D6",
    kind = "expense",
    isPreset = true,
    archived = archived,
    sortOrder = 0,
    updatedAt = updatedAt,
    deletedAt = null,
)

private class FixedAuth(uid: String) : AuthRepository {
    private val snap = AuthState(firebaseUid = uid, signedIn = true)
    override val state: Flow<AuthState> = MutableStateFlow(snap)
    override suspend fun current(): AuthState = snap
    override suspend fun signIn(activity: Activity): Result<AuthState> = Result.success(snap)
    override suspend fun signOut() = Unit
    override suspend fun deleteAccount(activity: Activity): Result<Unit> = Result.success(Unit)
}

private class MemCloud(
    var profile: UserProfile?,
    private val categories: List<Category> = emptyList(),
) : CloudDataSource {
    override val available: Boolean = true
    override suspend fun pushTransactions(uid: String, rows: List<Transaction>) = Unit
    override suspend fun pullTransactions(uid: String, sinceEpoch: Long): List<Transaction> = emptyList()
    override suspend fun pushCategories(uid: String, rows: List<Category>) = Unit
    override suspend fun pullCategories(uid: String, sinceEpoch: Long): List<Category> =
        categories.filter { it.updatedAt > sinceEpoch }
    override suspend fun pushProfile(uid: String, profile: UserProfile) {
        this.profile = profile
    }
    override suspend fun pullProfile(uid: String, sinceEpoch: Long): UserProfile? =
        profile?.takeIf { it.updatedAt > sinceEpoch }
    override suspend fun deleteAll(uid: String) { profile = null }
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

private class MemTx : TransactionDao {
    override suspend fun insert(row: TransactionEntity) = Unit
    override suspend fun update(row: TransactionEntity) = Unit
    override suspend fun upsert(row: TransactionEntity) = Unit
    override suspend fun listDirty(): List<TransactionEntity> = emptyList()
    override fun observeDirtyCount(): Flow<Int> = MutableStateFlow(0)
    override suspend fun get(id: String): TransactionEntity? = null
    override fun observeActive(): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
    override suspend fun listActive(): List<TransactionEntity> = emptyList()
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
