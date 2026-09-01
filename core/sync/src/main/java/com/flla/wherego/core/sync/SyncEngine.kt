package com.flla.wherego.core.sync

import com.flla.wherego.core.database.CategoryDao
import com.flla.wherego.core.database.CategoryEntity
import com.flla.wherego.core.database.SyncStateDao
import com.flla.wherego.core.database.SyncStateEntity
import com.flla.wherego.core.database.TransactionDao
import com.flla.wherego.core.database.TransactionEntity
import com.flla.wherego.core.database.UserProfileDao
import com.flla.wherego.core.database.UserProfileEntity
import com.flla.wherego.core.datastore.ThemePreferences
import com.flla.wherego.core.model.BalanceSeries
import com.flla.wherego.core.sync.CloudDataSource.Companion.NO_CURSOR
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
    private val preferences: ThemePreferences,
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

    /**
     * Clearing `dirty` is only safe for the row that was actually pushed. Re-reading catches an
     * edit landed while the batch was in flight: its `updatedAt` has moved, so the flag stays up
     * and the next pass carries the newer copy.
     */
    private suspend fun pushTransactions(uid: String) {
        val dirty = transactions.listDirty()
        if (dirty.isEmpty()) return
        cloud.pushTransactions(uid, dirty.map { it.toModel() })
        dirty.forEach { pushed ->
            val current = transactions.get(pushed.id) ?: return@forEach
            if (SyncMerge.shouldClearDirty(pushed.updatedAt, current.updatedAt)) {
                transactions.update(current.copy(dirty = false))
            }
        }
    }

    private suspend fun pullTransactions(uid: String) {
        val fallback = profiles.get()?.startingBalanceMinor ?: 0L
        val before = balance(fallback)
        val page = cloud.pullTransactions(uid, cursor(TRANSACTIONS))
        page.rows.forEach { incoming ->
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
        advance(TRANSACTIONS, page.cursor)
        flagConflict(before, balance(fallback))
    }

    private suspend fun balance(fallbackMinor: Long): Pair<String?, Long> {
        val rows = transactions.listActive().map { it.toModel() }
        return BalanceSeries.anchor(rows)?.id to BalanceSeries.total(rows, fallbackMinor)
    }

    /**
     * Another device's assertion took over the balance and the number moved. The arithmetic is
     * right — the later claim anchors — but the user was shown a different figure and the peer's
     * claim could be the typo, so Home asks once. Equal totals settle nothing worth asking about.
     */
    private suspend fun flagConflict(before: Pair<String?, Long>, after: Pair<String?, Long>) {
        val mine = before.first ?: return
        val theirs = after.first ?: return
        if (mine == theirs || before.second == after.second) return
        preferences.setBalanceConflict(mineId = mine, theirsId = theirs)
    }

    private suspend fun pushCategories(uid: String) {
        val rows = categories.listAll()
        if (rows.isEmpty()) return
        cloud.pushCategories(uid, rows.map { it.toModel() })
    }

    private suspend fun pullCategories(uid: String, preferRemote: Boolean = false) {
        val page = cloud.pullCategories(uid, cursor(CATEGORIES))
        page.rows.forEach { incoming ->
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
        advance(CATEGORIES, page.cursor)
    }

    private suspend fun pushProfile(uid: String) {
        val profile = profiles.get()?.toModel() ?: return
        cloud.pushProfile(uid, profile)
    }

    private suspend fun pullProfile(uid: String) {
        val remote = cloud.pullProfile(uid) ?: return
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
    }

    private suspend fun cursor(collection: String): Long =
        syncState.get(collection)?.lastPullCursor ?: NO_CURSOR

    private suspend fun advance(collection: String, cursor: Long) {
        syncState.upsert(SyncStateEntity(collection = collection, lastPullCursor = cursor))
    }

    private companion object {
        const val TRANSACTIONS = "transactions"
        const val CATEGORIES = "categories"
    }
}
