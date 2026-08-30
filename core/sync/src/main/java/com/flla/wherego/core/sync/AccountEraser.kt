package com.flla.wherego.core.sync

import android.app.Activity
import com.flla.wherego.core.database.LocalDataEraser
import com.flla.wherego.core.datastore.ThemePreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountEraser @Inject constructor(
    private val auth: AuthRepository,
    private val cloud: CloudDataSource,
    private val receipts: ReceiptUploader,
    private val local: LocalDataEraser,
    private val preferences: ThemePreferences,
    private val syncScheduler: SyncScheduler,
    private val fxCache: FxCacheScheduler,
) {
    /**
     * Cloud first, device second. A cloud or auth failure aborts with the device
     * untouched, so a retry still knows which account to purge — the reverse order
     * could strand a live cloud copy that the next sign-in would pull straight back.
     */
    suspend fun erase(activity: Activity): Result<Unit> {
        syncScheduler.cancelAllWork()
        val uid = auth.current().firebaseUid
        if (uid != null) {
            if (cloud.available) {
                runCatching { cloud.deleteAll(uid) }.onFailure { return Result.failure(it) }
            }
            receipts.deleteAll(uid)
            auth.deleteAccount(activity).onFailure { return Result.failure(it) }
        }
        auth.signOut()
        local.resetToGuest()
        preferences.clear()
        syncScheduler.enqueuePeriodic()
        fxCache.enqueueWeekly()
        return Result.success(Unit)
    }
}
