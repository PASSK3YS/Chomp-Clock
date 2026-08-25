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
                targetTimeMillis = intent.getLongExtra(EXTRA_TARGET_TIME, 0)
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
        startForeground(NOTIFICATION_ID, buildNotification(getTimerText()))

        serviceScope.launch {
            while (isFasting) {
                delay(1000)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, buildNotification(getTimerText()))
            }
        }
    }
    
    private fun getTimerText(): String {
        val now = System.currentTimeMillis()
        val elapsed = now - startTimeMillis
        val remaining = maxOf(0, targetTimeMillis - elapsed)
        
        val hours = remaining / 3600000
        val minutes = (remaining % 3600000) / 60000
        val seconds = (remaining % 60000) / 1000
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun stopFasting() {
        isFasting = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(timerText: String): Notification {
        val stopIntent = Intent(this, FastingService::class.java).apply {
            action = ACTION_STOP_FAST
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fasting Timer")
            .setContentText(timerText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "End Fast", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fasting Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the active fasting timer"
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
        private const val NOTIFICATION_ID = 1
    }
}
