package com.example.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object InAppUpdateInstaller {

    private const val TAG = "InAppUpdateInstaller"

    data class ApkInfo(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val isCompatiblePackage: Boolean,
        val fileSizeBytes: Long
    )

    data class InstallState(
        val isDownloading: Boolean = false,
        val progress: Float = 0f,
        val downloadedMb: Float = 0f,
        val totalMb: Float = 0f,
        val statusMessage: String = "",
        val isReadyToInstall: Boolean = false,
        val apkInfo: ApkInfo? = null,
        val localApkFile: File? = null,
        val error: String? = null
    )

    fun canInstallApks(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to open ACTION_MANAGE_UNKNOWN_APP_SOURCES, falling back to security settings", e)
                try {
                    val fallback = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to open security settings", e2)
                }
            }
        }
    }

    /**
     * Inspects and validates an APK file using the Android PackageManager.
     * Returns null if the file is corrupted, not a valid APK, or cannot be parsed.
     */
    fun inspectApk(context: Context, apkFile: File): ApkInfo? {
        if (!apkFile.exists() || apkFile.length() <= 0) return null
        return try {
            val pm = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            } ?: return null

            val apkPackageName = packageInfo.packageName ?: return null
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            ApkInfo(
                packageName = apkPackageName,
                versionName = versionName,
                versionCode = versionCode,
                isCompatiblePackage = (apkPackageName == context.packageName),
                fileSizeBytes = apkFile.length()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting APK: ${apkFile.absolutePath}", e)
            null
        }
    }

    /**
     * Copies a user-selected APK from a Content URI (from SAF file picker) into local app storage.
     */
    suspend fun copyUriToApkFile(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "apk_updates")
            if (!updateDir.exists()) updateDir.mkdirs()

            val targetFile = File(updateDir, "ChompClock-local-update.apk")
            if (targetFile.exists()) targetFile.delete()

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.setReadable(true, false)
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy APK from URI: $uri", e)
            null
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (InstallState) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            onProgress(
                InstallState(
                    isDownloading = true,
                    progress = 0.05f,
                    statusMessage = "Connecting to download server..."
                )
            )

            val url = URL(downloadUrl)
            var connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "ChompClock-Android-App")
            connection.setRequestProperty("Accept", "application/octet-stream, application/vnd.android.package-archive, */*")
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 20000
            connection.readTimeout = 30000
            connection.connect()

            // Handle HTTP 301/302/307/308 redirects (GitHub release downloads redirect to AWS S3)
            var responseCode = connection.responseCode
            var redirectCount = 0
            while ((responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == 307 || responseCode == 308) && redirectCount < 5) {
                val newUrl = connection.getHeaderField("Location") ?: break
                connection.disconnect()
                val redirectUrl = URL(newUrl)
                connection = redirectUrl.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "ChompClock-Android-App")
                connection.connectTimeout = 20000
                connection.readTimeout = 30000
                connection.connect()
                responseCode = connection.responseCode
                redirectCount++
            }

            if (responseCode !in 200..299) {
                onProgress(
                    InstallState(
                        isDownloading = false,
                        error = "Download server returned HTTP $responseCode"
                    )
                )
                return@withContext null
            }

            val fileLength = connection.contentLength.toLong()
            val totalMb = if (fileLength > 0) fileLength / (1024f * 1024f) else 0f

            val updateDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "apk_updates")
            if (!updateDir.exists()) updateDir.mkdirs()

            val apkFile = File(updateDir, "ChompClock-update.apk")
            if (apkFile.exists()) apkFile.delete()

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(16384)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        val progress = if (fileLength > 0) {
                            (totalBytesRead.toFloat() / fileLength.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0.5f
                        }
                        val downloadedMb = totalBytesRead / (1024f * 1024f)

                        onProgress(
                            InstallState(
                                isDownloading = true,
                                progress = progress,
                                downloadedMb = downloadedMb,
                                totalMb = totalMb,
                                statusMessage = if (totalMb > 0) {
                                    "Downloading: ${"%.1f".format(downloadedMb)} MB / ${"%.1f".format(totalMb)} MB (${(progress * 100).toInt()}%)"
                                } else {
                                    "Downloading: ${"%.1f".format(downloadedMb)} MB..."
                                }
                            )
                        )
                    }
                    output.flush()
                }
            }
            apkFile.setReadable(true, false)

            // Validate the downloaded APK
            val apkInfo = inspectApk(context, apkFile)
            if (apkInfo == null) {
                onProgress(
                    InstallState(
                        isDownloading = false,
                        error = "The downloaded file is corrupted or not a valid Android APK."
                    )
                )
                return@withContext null
            }

            onProgress(
                InstallState(
                    isDownloading = false,
                    progress = 1.0f,
                    isReadyToInstall = true,
                    apkInfo = apkInfo,
                    localApkFile = apkFile,
                    statusMessage = "Ready to install: ${apkInfo.versionName} (${"%.1f".format(apkFile.length() / (1024f * 1024f))} MB)"
                )
            )

            apkFile
        } catch (e: Exception) {
            Log.e(TAG, "Exception during APK download", e)
            onProgress(
                InstallState(
                    isDownloading = false,
                    error = e.localizedMessage ?: "Failed to download update APK"
                )
            )
            null
        }
    }

    /**
     * Installs an APK using the official Android PackageInstaller Session API.
     * This is the recommended and most reliable method for modern Android (API 21+ / Android 10-15).
     */
    private fun installViaPackageInstaller(context: Context, apkFile: File): Boolean {
        return try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                setSize(apkFile.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
                }
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            apkFile.inputStream().use { input ->
                session.openWrite("package_update", 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            val intent = Intent(context, PackageInstallStatusReceiver::class.java).apply {
                action = PackageInstallStatusReceiver.ACTION_INSTALL_STATUS
                putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags)

            session.commit(pendingIntent.intentSender)
            session.close()
            Log.d(TAG, "PackageInstaller session $sessionId committed successfully")
            true
        } catch (e: Exception) {
            Log.w(TAG, "PackageInstaller session creation failed, will fall back to Intent.ACTION_VIEW", e)
            false
        }
    }

    /**
     * Fallback method using Intent.ACTION_VIEW with FileProvider and explicit URI permission grants.
     */
    private fun installViaActionView(context: Context, apkFile: File): Boolean {
        return try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Explicitly grant URI permission to any app capable of handling package installation
            val resInfoList = context.packageManager.queryIntentActivities(
                installIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val pkgName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(pkgName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(installIntent)
            Log.d(TAG, "Started package installation via Intent.ACTION_VIEW")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ACTION_VIEW intent for APK", e)
            false
        }
    }

    /**
     * Comprehensive entry point to trigger the installation of a local APK file.
     * Returns Pair(success, errorMessage).
     */
    fun triggerPackageInstall(context: Context, apkFile: File): Pair<Boolean, String?> {
        // 1. Check if the app has permission to install unknown apps
        if (!canInstallApks(context)) {
            return Pair(false, "PERMISSION_REQUIRED")
        }

        // 2. Inspect the APK
        val apkInfo = inspectApk(context, apkFile)
        if (apkInfo == null) {
            return Pair(false, "The APK file is invalid, corrupted, or cannot be parsed by Android.")
        }

        if (!apkInfo.isCompatiblePackage) {
            return Pair(false, "The APK package is '${apkInfo.packageName}', but this app is '${context.packageName}'.")
        }

        // 3. Try official PackageInstaller API first
        val sessionSuccess = installViaPackageInstaller(context, apkFile)
        if (sessionSuccess) {
            return Pair(true, null)
        }

        // 4. Fallback to FileProvider + Intent.ACTION_VIEW
        val actionViewSuccess = installViaActionView(context, apkFile)
        if (actionViewSuccess) {
            return Pair(true, null)
        }

        return Pair(false, "Could not launch Android package installer. Please verify unknown apps permission.")
    }
}
