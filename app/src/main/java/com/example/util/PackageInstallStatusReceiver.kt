package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log

class PackageInstallStatusReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_INSTALL_STATUS = "com.example.chompclock.ACTION_INSTALL_STATUS"
        private const val TAG = "PackageInstallStatus"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "No detail provided"
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)

        Log.d(TAG, "Install status update: status=$status, message=$message, sessionId=$sessionId")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // The Android PackageInstaller requires user confirmation to proceed with installation
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }

                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirmIntent)
                        Log.d(TAG, "Successfully launched system package install confirmation prompt")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start install confirmation activity", e)
                    }
                } else {
                    Log.e(TAG, "STATUS_PENDING_USER_ACTION received but EXTRA_INTENT was null")
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "Package installation succeeded!")
            }
            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_ABORTED,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                Log.e(TAG, "Package installation failed: status=$status ($message)")
            }
            else -> {
                Log.d(TAG, "Other install status: $status ($message)")
            }
        }
    }
}
