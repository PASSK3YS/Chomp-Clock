package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class WeightReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        Log.d("WeightReminderReceiver", "Alarm received: ${intent?.action}")
        if (intent?.action == WeightReminderManager.ACTION_WEIGH_IN_ALARM) {
            WeightReminderManager.showWeighInNotification(context, isTest = false)
        }
    }
}
