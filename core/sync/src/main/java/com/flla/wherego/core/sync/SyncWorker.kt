package com.flla.wherego.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val engine = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncEntryPoint::class.java,
        ).syncEngine()
        engine.sync()
        return Result.success()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncEntryPoint {
    fun syncEngine(): SyncEngine
}

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun enqueueNow() {
        try {
            val request = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(ONLINE).build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_NOW, ExistingWorkPolicy.REPLACE, request)
        } catch (_: Exception) {
        }
    }

    fun enqueuePeriodic() {
        try {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(ONLINE)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        } catch (_: Exception) {
        }
    }

    /** Cancels every queued Wherego worker — sync, FX, receipt upload, due reminders. */
    fun cancelAllWork() {
        try {
            WorkManager.getInstance(context).cancelAllWork()
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val UNIQUE_NOW = "wherego-sync-now"
        const val UNIQUE_PERIODIC = "wherego-sync-periodic"

        /** Pulls read from the server so `syncedAt` is resolved; offline runs are deferred. */
        val ONLINE: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
