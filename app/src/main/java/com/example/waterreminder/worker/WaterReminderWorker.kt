package com.example.waterreminder.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.waterreminder.R
import com.example.waterreminder.data.WaterReminderPreferences

class WaterReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("WorkManager", "виконується задача нагадування про воду")

            // збільш лічильник відправлених нагадувань на +1 у DataStore
            val prefs = WaterReminderPreferences(applicationContext)
            prefs.incrementReminderCount()

            // перевір дозвіл POST_NOTIFICATIONS для Android 13+ та відправляємо сповіщення
            showWaterNotification()

            Log.d("WorkManager", "нагадування успішно відправлено та збережено")
            Result.success()
        } catch (e: Exception) {
            Log.e("WorkManager", "помилка під час виконання WaterReminderWorker", e)
            Result.retry()
        }
    }

    private fun showWaterNotification() {
        // TIRAMISU і вище
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.w("WorkManager", "немає дозволу POST_NOTIFICATIONS для показу сповіщення")
                return
            }
        }

        val channelId = "water_channel"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // канал сповіщень (Android 8.0 / API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "нагадування про воду",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "канал для регулярних нагадувань випити склянку води"
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("трекер води")
            .setContentText("час випити склянку води! 💧")
            .setSmallIcon(R.drawable.ic_water_drop)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}