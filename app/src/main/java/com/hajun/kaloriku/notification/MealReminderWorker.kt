package com.hajun.kaloriku.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hajun.kaloriku.MainActivity
import com.hajun.kaloriku.R
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import com.hajun.kaloriku.data.ReminderTimes
import com.hajun.kaloriku.util.toLocalDate
import com.hajun.kaloriku.util.nextReminderDelayMillis
import java.util.concurrent.TimeUnit

/**
 * Pengingat makan pagi, siang, dan malam. Pekerjaan ini hanya mengirim notifikasi
 * kalau pada hari itu belum ada catatan untuk waktu makan tersebut.
 */
class MealReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? com.hajun.kaloriku.KaloriKuApp ?: return Result.success()
        val repository = app.container.repository
        if (!repository.remindersEnabled.value) return Result.success()

        val mealType = inputData.getString(KEY_MEAL_TYPE) ?: return Result.success()
        val today = LocalDate.now()

        val entries = repository.entries.first()
        val alreadyRecorded = entries.any {
            it.timestamp.toLocalDate() == today && it.mealType.name == mealType
        }
        if (alreadyRecorded) return Result.success()

        sendNotification(mealType)
        return Result.success()
    }

    private fun sendNotification(mealType: String) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager)

        val label = when (mealType) {
            "SARAPAN" -> "sarapan"
            "MAKAN_SIANG" -> "makan siang"
            else -> "makan malam"
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            mealType.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Waktunya mencatat $label")
            .setContentText("Kamu belum mencatat $label hari ini. Yuk catat sekarang.")
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(mealType.hashCode(), notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pengingat makan",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Mengingatkan mencatat makan pagi, siang, dan malam"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "meal_reminder"
        private const val KEY_MEAL_TYPE = "meal_type"

        /**
         * Mendaftarkan pengingat harian untuk tiga waktu makan.
         * Memakai PeriodicWorkRequest 24 jam dengan waktu mulai yang berbeda,
         * karena Android membatasi jumlah pekerjaan berkala per aplikasi.
         */
        fun schedule(context: Context, hour: Int, minute: Int, mealType: String, tag: String) {
            val delay = nextReminderDelayMillis(java.time.ZonedDateTime.now(), hour, minute)
            val request = PeriodicWorkRequestBuilder<MealReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(androidx.work.workDataOf(KEY_MEAL_TYPE to mealType))
                .setConstraints(Constraints.Builder().build())
                .addTag(tag)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                tag,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun scheduleAll(context: Context, times: ReminderTimes = ReminderTimes()) {
            schedule(context, times.breakfastHour, times.breakfastMinute, "SARAPAN", "reminder_sarapan")
            schedule(context, times.lunchHour, times.lunchMinute, "MAKAN_SIANG", "reminder_makan_siang")
            schedule(context, times.dinnerHour, times.dinnerMinute, "MAKAN_MALAM", "reminder_makan_malam")
        }

        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).apply {
                cancelUniqueWork("reminder_sarapan")
                cancelUniqueWork("reminder_makan_siang")
                cancelUniqueWork("reminder_makan_malam")
            }
        }
    }
}
