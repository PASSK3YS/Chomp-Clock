package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class FastingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var isFasting = false
    private var targetTimeMillis: Long = 0
    private var startTimeMillis: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FAST -> {
                targetTimeMillis = intent.getLongExtra(EXTRA_TARGET_TIME, 16L * 3600 * 1000)
                startTimeMillis = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())
                startFasting()
            }
            ACTION_STOP_FAST -> {
                stopFasting()
            }
        }
        return START_STICKY
    }

    private fun startFasting() {
        if (isFasting) return
        isFasting = true
        startForeground(NOTIFICATION_ID, buildNotification())

        serviceScope.launch {
            while (isFasting) {
                delay(1000)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, buildNotification())
            }
        }
    }
    
    private fun getMetabolicStage(hours: Float): String {
        return when {
            hours < 4.0f -> "Digestion Stage"
            hours < 8.0f -> "Blood Sugar Normalizing"
            hours < 12.0f -> "Fat Burning Mode"
            hours < 18.0f -> "Ketosis State"
            hours < 24.0f -> "Autophagy Active"
            else -> "Deep Cellular Cleanse"
        }
    }

    private fun stopFasting() {
        isFasting = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val now = System.currentTimeMillis()
        val elapsedMillis = maxOf(0L, now - startTimeMillis)
        val elapsedHours = elapsedMillis / 3600000
        val elapsedMinutes = (elapsedMillis % 3600000) / 60000
        val elapsedSeconds = (elapsedMillis % 60000) / 1000
        val elapsedFormatted = String.format("%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds)

        val targetHours = targetTimeMillis / 3600000f
        val isOvertime = elapsedMillis >= targetTimeMillis
        val hoursFloat = elapsedMillis / 3600000f
        val stage = getMetabolicStage(hoursFloat)

        val contentTitle = "Active Fast • $stage"
        val contentText = if (isOvertime) {
            val overtimeMillis = elapsedMillis - targetTimeMillis
            val overH = overtimeMillis / 3600000
            val overM = (overtimeMillis % 3600000) / 60000
            "🎯 Goal completed! $elapsedFormatted elapsed (+${overH}h ${overM}m overtime)"
        } else {
            val remainingMillis = targetTimeMillis - elapsedMillis
            val remH = remainingMillis / 3600000
            val remM = (remainingMillis % 3600000) / 60000
            "⏳ $elapsedFormatted elapsed • ${remH}h ${remM}m left (Goal: ${targetHours.toInt()}h)"
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openAppPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Fasting Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent progress while an intermittent fast is active"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        const val ACTION_START_FAST = "ACTION_START_FAST"
        const val ACTION_STOP_FAST = "ACTION_STOP_FAST"
        const val EXTRA_TARGET_TIME = "EXTRA_TARGET_TIME"
        const val EXTRA_START_TIME = "EXTRA_START_TIME"
        private const val CHANNEL_ID = "fasting_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
