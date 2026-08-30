package com.flla.wherego.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flla.wherego.core.database.FxRateStore
import com.flla.wherego.core.database.LedgerStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

class FxCacheWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(
            applicationContext,
            FxCacheEntryPoint::class.java,
        )
        val used = deps.ledger().usedCurrencies().filter { it != "IDR" }
        if (used.isEmpty()) return Result.success()
        used.forEach { code ->
            val rate = fetchRate(code, "IDR") ?: return@forEach
            deps.rates().put(code, rate)
        }
        return Result.success()
    }

    private fun fetchRate(from: String, to: String): String? {
        return try {
            val url = URL("https://api.frankfurter.app/latest?from=$from&to=$to")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val match = Regex(""""$to"\s*:\s*([0-9]+(?:\.[0-9]+)?)""").find(body)
            match?.groupValues?.getOrNull(1)
        } catch (_: Exception) {
            null
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FxCacheEntryPoint {
    fun ledger(): LedgerStore
    fun rates(): FxRateStore
}

@Singleton
class FxCacheScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun enqueueWeekly() {
        val request = PeriodicWorkRequestBuilder<FxCacheWorker>(7, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val UNIQUE = "wherego-fx-weekly"
    }
}
