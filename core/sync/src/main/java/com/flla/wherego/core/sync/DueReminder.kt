package com.flla.wherego.core.sync

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
import com.flla.wherego.core.database.UserProfileStore
import com.flla.wherego.core.i18n.AppLocale
import com.flla.wherego.core.i18n.R
import com.flla.wherego.core.model.AppLanguage
import com.flla.wherego.core.model.RecurringRule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

class DueReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val lang = inputData.getString(KEY_LANG) ?: AppLanguage.SYSTEM
        val ctx = AppLocale.context(applicationContext, lang)
        val label = inputData.getString(KEY_LABEL)
            ?: ctx.getString(R.string.recurring_fallback_label)
        ensureChannel(ctx)
        val notification = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(ctx.getString(R.string.notif_due_title))
            .setContentText(ctx.getString(R.string.notif_due_body, label))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(label.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_LABEL = "label"
        const val KEY_LANG = "lang"
        const val CHANNEL = "wherego-due"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    context.getString(R.string.notif_channel_due),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
    }
}

@Singleton
class DueReminder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userProfileStore: UserProfileStore,
) {
    suspend fun schedule(rule: RecurringRule, zoneId: ZoneId) {
        val next = LocalDate.parse(rule.nextOn)
        val fireAt = ZonedDateTime.of(next, LocalTime.of(8, 0), zoneId)
        val delayMs = fireAt.toInstant().toEpochMilli() - System.currentTimeMillis()
        val lang = AppLanguage.parse(userProfileStore.profile.first()?.localeTag)
        val ctx = AppLocale.context(context, lang)
        val label = rule.note.ifBlank { ctx.getString(R.string.recurring_fallback_label) }
        val data = Data.Builder()
            .putString(DueReminderWorker.KEY_LABEL, label)
            .putString(DueReminderWorker.KEY_LANG, lang)
            .build()
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
