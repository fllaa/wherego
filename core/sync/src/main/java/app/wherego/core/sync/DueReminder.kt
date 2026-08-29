package app.wherego.core.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.wherego.core.model.RecurringRule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

class DueReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val label = inputData.getString(KEY_LABEL) ?: "Bill"
        ensureChannel(applicationContext)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Wherego")
            .setContentText("$label usually hits today. Log it?")
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(label.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_LABEL = "label"
        const val CHANNEL = "wherego-due"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Due reminders", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }
}

@Singleton
class DueReminder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedule(rule: RecurringRule, zoneId: ZoneId) {
        val next = LocalDate.parse(rule.nextOn)
        val fireAt = ZonedDateTime.of(next, LocalTime.of(8, 0), zoneId)
        val delayMs = fireAt.toInstant().toEpochMilli() - System.currentTimeMillis()
        val label = rule.note.ifBlank { "Bill" }
        val data = Data.Builder().putString(DueReminderWorker.KEY_LABEL, label).build()
        val request = OneTimeWorkRequestBuilder<DueReminderWorker>()
            .setInputData(data)
            .setInitialDelay(delayMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "due-${rule.id}",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
