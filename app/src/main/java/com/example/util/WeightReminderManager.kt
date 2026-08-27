package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.WeighInFrequency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object WeightReminderManager {

    const val CHANNEL_ID = "weigh_in_reminders"
    const val CHANNEL_NAME = "Weigh-in Reminders"
    const val NOTIFICATION_ID = 7001
    const val REQUEST_CODE_ALARM = 7002

    const val ACTION_WEIGH_IN_ALARM = "com.example.ACTION_WEIGH_IN_REMINDER"
    const val EXTRA_NAV_TARGET = "extra_nav_target"
    const val NAV_TARGET_WEIGHT = "weight"

    private val motivationalMessages = listOf(
        "Time for your weigh-in! Step on the scale and log your progress.",
        "Consistency is key! Take a moment to record your weight today.",
        "Check in with your fitness goals. Log your weigh-in in ChompClock!",
        "Keep up the great work! Record your weight to see your trend.",
        "Morning check-in: Log your weight and celebrate your consistency!"
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Timely reminders to step on the scale and record your weight"
                    enableVibration(true)
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Calculates the next trigger timestamp in milliseconds for the given schedule.
     */
    fun calculateNextTriggerMillis(
        frequency: WeighInFrequency,
        dayOfWeek: Int,
        hour: Int,
        minute: Int,
        fromMillis: Long = System.currentTimeMillis()
    ): Long {
        val now = Calendar.getInstance().apply { timeInMillis = fromMillis }
        val target = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (frequency) {
            WeighInFrequency.DAILY -> {
                if (!target.after(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            WeighInFrequency.WEEKLY -> {
                target.set(Calendar.DAY_OF_WEEK, dayOfWeek)
                if (!target.after(now)) {
                    target.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            WeighInFrequency.BI_WEEKLY -> {
                target.set(Calendar.DAY_OF_WEEK, dayOfWeek)
                if (!target.after(now)) {
                    target.add(Calendar.WEEK_OF_YEAR, 2)
                }
            }
            WeighInFrequency.MONTHLY -> {
                target.set(Calendar.DAY_OF_WEEK, dayOfWeek)
                if (!target.after(now)) {
                    target.add(Calendar.WEEK_OF_YEAR, 4)
                }
            }
        }
        return target.timeInMillis
    }

    /**
     * Formats next reminder timestamp into user-friendly text like:
     * "Monday, 31 Aug at 08:00 AM (in 3 days)"
     */
    fun formatNextReminderPreview(
        frequency: WeighInFrequency,
        dayOfWeek: Int,
        hour: Int,
        minute: Int
    ): String {
        val nextMillis = calculateNextTriggerMillis(frequency, dayOfWeek, hour, minute)
        val now = System.currentTimeMillis()
        val diffMillis = nextMillis - now
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        val diffHours = ((diffMillis % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)).toInt()

        val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        val dateStr = dateFormatter.format(Date(nextMillis))
        val timeStr = timeFormatter.format(Date(nextMillis))

        val relativeStr = when {
            diffDays == 0 && diffHours <= 1 -> "in less than an hour"
            diffDays == 0 -> "today in ${diffHours}h"
            diffDays == 1 -> "tomorrow at $timeStr"
            else -> "in $diffDays days"
        }

        return "$dateStr at $timeStr ($relativeStr)"
    }

    /**
     * Converts day-of-week integer (Calendar.SUNDAY..Calendar.SATURDAY) to display name.
     */
    fun getDayOfWeekDisplayName(dayOfWeek: Int, short: Boolean = false): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> if (short) "Sun" else "Sunday"
            Calendar.MONDAY -> if (short) "Mon" else "Monday"
            Calendar.TUESDAY -> if (short) "Tue" else "Tuesday"
            Calendar.WEDNESDAY -> if (short) "Wed" else "Wednesday"
            Calendar.THURSDAY -> if (short) "Thu" else "Thursday"
            Calendar.FRIDAY -> if (short) "Fri" else "Friday"
            Calendar.SATURDAY -> if (short) "Sat" else "Saturday"
            else -> if (short) "Mon" else "Monday"
        }
    }

    /**
     * Schedules the next weigh-in alarm with AlarmManager.
     */
    fun scheduleAlarm(
        context: Context,
        enabled: Boolean,
        frequency: WeighInFrequency,
        dayOfWeek: Int,
        hour: Int,
        minute: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WeightReminderReceiver::class.java).apply {
            action = ACTION_WEIGH_IN_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            Log.d("WeightReminder", "Weigh-in reminder alarm canceled")
            return
        }

        val triggerAtMillis = calculateNextTriggerMillis(frequency, dayOfWeek, hour, minute)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            val formatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(triggerAtMillis))
            Log.d("WeightReminder", "Weigh-in reminder scheduled for $formatted")
        } catch (e: SecurityException) {
            // Fallback for Android 12+ if exact alarm permission is restricted
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (err: Exception) {
                Log.e("WeightReminder", "Failed to schedule reminder alarm", err)
            }
        }
    }

    /**
     * Cancels the active weigh-in alarm.
     */
    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WeightReminderReceiver::class.java).apply {
            action = ACTION_WEIGH_IN_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Displays the notification immediately and reschedules the subsequent occurrence.
     */
    fun showWeighInNotification(context: Context, isTest: Boolean = false) {
        createNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_TARGET, NAV_TARGET_WEIGHT)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val randomMessage = motivationalMessages.random()
        val titleText = if (isTest) "⚖️ Weigh-In Reminder (Test)" else "⚖️ Time for your Weigh-In"
        val bodyText = if (isTest) "Notifications are working! You'll receive reminders based on your schedule." else randomMessage

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.mipmap.ic_launcher,
                "Log Weight",
                contentPendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        // Reschedule next occurrence if this was an actual alarm trigger
        if (!isTest) {
            CoroutineScope(Dispatchers.IO).launch {
                val repository = UserPreferencesRepository(context)
                val prefs = repository.userPreferencesFlow.first()
                if (prefs.weighInReminderEnabled) {
                    repository.updateWeighInLastNotifiedMillis(System.currentTimeMillis())
                    scheduleAlarm(
                        context = context,
                        enabled = true,
                        frequency = prefs.weighInFrequency,
                        dayOfWeek = prefs.weighInDayOfWeek,
                        hour = prefs.weighInHour,
                        minute = prefs.weighInMinute
                    )
                }
            }
        }
    }

    /**
     * Reschedules alarm after reboot or preference update.
     */
    fun rescheduleFromPreferences(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = UserPreferencesRepository(context)
            val prefs = repository.userPreferencesFlow.first()
            if (prefs.weighInReminderEnabled) {
                scheduleAlarm(
                    context = context,
                    enabled = true,
                    frequency = prefs.weighInFrequency,
                    dayOfWeek = prefs.weighInDayOfWeek,
                    hour = prefs.weighInHour,
                    minute = prefs.weighInMinute
                )
            } else {
                cancelAlarm(context)
            }
        }
    }
}
