package com.flla.wherego.core.sync

import com.flla.wherego.core.database.CategoryDao
import com.flla.wherego.core.database.CategoryEntity
import com.flla.wherego.core.database.SyncStateDao
import com.flla.wherego.core.database.SyncStateEntity
import com.flla.wherego.core.database.TransactionDao
import com.flla.wherego.core.database.TransactionEntity
import com.flla.wherego.core.database.UserProfileDao
import com.flla.wherego.core.database.UserProfileEntity
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
    /**
     * @return whether the local profile is onboarded after this pass — true if we
     * adopted an already-onboarded cloud profile on reinstall.
     */
    suspend fun sync(): Boolean {
        val uid = auth.current().firebaseUid ?: return profiles.get()?.onboardingDone == true
        if (!cloud.available) return profiles.get()?.onboardingDone == true
        val restoring = profiles.get()?.onboardingDone != true
        if (restoring) pullProfile(uid)
        val restored = restoring && profiles.get()?.onboardingDone == true
        pushTransactions(uid)
        if (!restored) pushCategories(uid)
        pushProfile(uid)
        pullTransactions(uid)
        pullCategories(uid, preferRemote = restored)
        if (!restoring) pullProfile(uid)
        return profiles.get()?.onboardingDone == true
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

    private suspend fun pullCategories(uid: String, preferRemote: Boolean = false) {
        val since = syncState.get("categories")?.lastPullEpoch ?: 0L
        cloud.pullCategories(uid, since).forEach { incoming ->
            val local = categories.get(incoming.id)
            val decision = if (preferRemote) {
                MergeDecision.ApplyRemote
            } else {
                SyncMerge.decide(
                    localUpdatedAt = local?.updatedAt,
                    localDirty = false,
                    remoteUpdatedAt = incoming.updatedAt,
                )
            }
            when (decision) {
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
            SyncMerge.decideProfile(
                localOnboardingDone = local?.onboardingDone,
                localUpdatedAt = local?.updatedAt,
                remoteOnboardingDone = remote.onboardingDone,
                remoteUpdatedAt = remote.updatedAt,
            )
        ) {
            MergeDecision.ApplyRemote -> {
                val merged = remote.copy(
                    id = local?.id ?: remote.id,
                    firebaseUid = local?.firebaseUid ?: remote.firebaseUid,
                    googleSub = local?.googleSub ?: remote.googleSub,
                    email = local?.email ?: remote.email,
                    displayName = local?.displayName ?: remote.displayName,
                    photoUrl = local?.photoUrl ?: remote.photoUrl,
                )
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
