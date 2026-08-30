package com.flla.wherego.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.flla.wherego.core.database.ReceiptStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class ReceiptUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val receiptId = inputData.getString(KEY_ID) ?: return Result.success()
        val deps = EntryPointAccessors.fromApplication(
            applicationContext,
            ReceiptUploadEntryPoint::class.java,
        )
        val uid = deps.auth().current().firebaseUid ?: return Result.success()
        val row = deps.receipts().get(receiptId) ?: return Result.success()
        val file = File(row.localPath)
        if (!file.exists()) return Result.success()
        return try {
            val remote = deps.uploader().upload(uid, receiptId, file)
            if (remote != null) deps.receipts().markUploaded(receiptId, remote)
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    companion object {
        const val KEY_ID = "receiptId"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiptUploadEntryPoint {
    fun auth(): AuthRepository
    fun receipts(): ReceiptStore
    fun uploader(): ReceiptUploader
}

@Singleton
class ReceiptUploadScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun enqueue(receiptId: String) {
        try {
            val request = OneTimeWorkRequestBuilder<ReceiptUploadWorker>()
                .setInputData(workDataOf(ReceiptUploadWorker.KEY_ID to receiptId))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "receipt-$receiptId",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        } catch (_: Exception) {
        }
    }
}
