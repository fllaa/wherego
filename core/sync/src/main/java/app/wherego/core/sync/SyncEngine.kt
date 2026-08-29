package app.wherego.core.sync

import app.wherego.core.database.CategoryDao
import app.wherego.core.database.CategoryEntity
import app.wherego.core.database.SyncStateDao
import app.wherego.core.database.SyncStateEntity
import app.wherego.core.database.TransactionDao
import app.wherego.core.database.TransactionEntity
import app.wherego.core.database.UserProfileDao
import app.wherego.core.database.UserProfileEntity
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncEngine @Inject constructor(
    private val auth: AuthRepository,
    private val cloud: CloudDataSource,
    private val transactions: TransactionDao,
    private val categories: CategoryDao,
    private val profiles: UserProfileDao,
    private val syncState: SyncStateDao,
    private val clock: Clock,
) {
    suspend fun sync() {
        val uid = auth.current().firebaseUid ?: return
        if (!cloud.available) return
        pushTransactions(uid)
        pushCategories(uid)
        pushProfile(uid)
        pullTransactions(uid)
        pullCategories(uid)
        pullProfile(uid)
    }

    private suspend fun pushTransactions(uid: String) {
        val dirty = transactions.listDirty()
        if (dirty.isNotEmpty()) {
            cloud.pushTransactions(uid, dirty.map { it.toModel() })
            dirty.forEach { row ->
                if (SyncMerge.shouldClearDirty(row.updatedAt, row.updatedAt)) {
                    transactions.update(row.copy(dirty = false))
                }
            }
        }
        markPushed("transactions")
    }

    private suspend fun pullTransactions(uid: String) {
        val since = syncState.get("transactions")?.lastPullEpoch ?: 0L
        val remote = cloud.pullTransactions(uid, since)
        remote.forEach { incoming ->
            val local = transactions.get(incoming.id)
            when (
                SyncMerge.decide(
                    localUpdatedAt = local?.updatedAt,
                    localDirty = local?.dirty == true,
                    remoteUpdatedAt = incoming.updatedAt,
                )
            ) {
                MergeDecision.ApplyRemote ->
                    transactions.upsert(TransactionEntity.from(incoming.copy(dirty = false)))
                MergeDecision.PushLocal, MergeDecision.KeepLocal -> Unit
            }
        }
        markPulled("transactions")
    }

    private suspend fun pushCategories(uid: String) {
        val rows = categories.listAll()
        if (rows.isNotEmpty()) {
            cloud.pushCategories(uid, rows.map { it.toModel() })
        }
        markPushed("categories")
    }

    private suspend fun pullCategories(uid: String) {
        val since = syncState.get("categories")?.lastPullEpoch ?: 0L
        cloud.pullCategories(uid, since).forEach { incoming ->
            val local = categories.get(incoming.id)
            when (
                SyncMerge.decide(
                    localUpdatedAt = local?.updatedAt,
                    localDirty = false,
                    remoteUpdatedAt = incoming.updatedAt,
                )
            ) {
                MergeDecision.ApplyRemote -> categories.upsert(CategoryEntity.from(incoming))
                MergeDecision.PushLocal, MergeDecision.KeepLocal -> Unit
            }
        }
        markPulled("categories")
    }

    private suspend fun pushProfile(uid: String) {
        val profile = profiles.get()?.toModel() ?: return
        cloud.pushProfile(uid, profile)
        markPushed("profile")
    }

    private suspend fun pullProfile(uid: String) {
        val since = syncState.get("profile")?.lastPullEpoch ?: 0L
        val remote = cloud.pullProfile(uid, since) ?: return
        val local = profiles.get()
        when (
            SyncMerge.decide(
                localUpdatedAt = local?.updatedAt,
                localDirty = false,
                remoteUpdatedAt = remote.updatedAt,
            )
        ) {
            MergeDecision.ApplyRemote -> {
                val merged = remote.copy(id = local?.id ?: remote.id)
                if (local != null) {
                    profiles.update(UserProfileEntity.from(merged))
                }
            }
            MergeDecision.PushLocal, MergeDecision.KeepLocal -> Unit
        }
        markPulled("profile")
    }

    private suspend fun markPushed(collection: String) {
        val now = clock.millis()
        val prev = syncState.get(collection)
        syncState.upsert(
            SyncStateEntity(
                collection = collection,
                lastPullEpoch = prev?.lastPullEpoch ?: 0L,
                lastPushEpoch = now,
            ),
        )
    }

    private suspend fun markPulled(collection: String) {
        val now = clock.millis()
        val prev = syncState.get(collection)
        syncState.upsert(
            SyncStateEntity(
                collection = collection,
                lastPullEpoch = now,
                lastPushEpoch = prev?.lastPushEpoch ?: 0L,
            ),
        )
    }
}
