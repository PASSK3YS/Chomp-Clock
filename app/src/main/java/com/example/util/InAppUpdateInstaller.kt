package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object InAppUpdateInstaller {

    data class InstallState(
        val isDownloading: Boolean = false,
        val progress: Float = 0f,
        val downloadedMb: Float = 0f,
        val totalMb: Float = 0f,
        val statusMessage: String = "",
        val isReadyToInstall: Boolean = false,
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
                val fallback = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
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

            // Handle HTTP 301/302 redirects (GitHub release downloads redirect to AWS S3)
            var responseCode = connection.responseCode
            var redirectCount = 0
            while ((responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == 307 || responseCode == 308) && redirectCount < 5) {
                val newUrl = connection.getHeaderField("Location")
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

            val apkFile = File(context.cacheDir, "chompclock-update.apk")
            if (apkFile.exists()) apkFile.delete()

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
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

            onProgress(
                InstallState(
                    isDownloading = false,
                    progress = 1.0f,
                    isReadyToInstall = true,
                    statusMessage = "Download complete. Starting Android package installer..."
                )
            )

            apkFile
        } catch (e: Exception) {
            onProgress(
                InstallState(
                    isDownloading = false,
                    error = e.localizedMessage ?: "Failed to download update APK"
                )
            )
            null
        }
    }

    fun triggerPackageInstall(context: Context, apkFile: File): Boolean {
        return try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            try {
                val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    val authority = "${context.packageName}.fileprovider"
                    val contentUri = FileProvider.getUriForFile(context, authority, apkFile)
                    data = contentUri
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                }
                context.startActivity(installIntent)
                true
            } catch (fallbackEx: Exception) {
                false
            }
        }
    }
}
