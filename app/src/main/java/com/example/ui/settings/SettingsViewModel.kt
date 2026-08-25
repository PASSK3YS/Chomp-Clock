package com.example.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.remote.GitHubReleaseResponse
import com.example.data.remote.GitHubService
import com.example.data.repository.BackupImportResult
import com.example.data.repository.DataBackupManager
import com.example.data.repository.UserPreferences
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    object Checking : UpdateCheckState()
    data class UpdateAvailable(
        val latestVersion: String,
        val currentVersion: String,
        val releaseName: String,
        val releaseNotes: String,
        val downloadUrl: String?,
        val htmlUrl: String,
        val publishedAt: String?
    ) : UpdateCheckState()
    data class UpToDate(
        val currentVersion: String,
        val releaseName: String,
        val releaseNotes: String?,
        val htmlUrl: String
    ) : UpdateCheckState()
    data class NoReleasesFound(
        val currentVersion: String,
        val repoUrl: String
    ) : UpdateCheckState()
    data class Error(
        val errorMessage: String,
        val repoUrl: String
    ) : UpdateCheckState()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserPreferencesRepository(application)
    private val db = AppDatabase.getDatabase(application)
    private val gitHubService = GitHubService.create()
    val backupManager = DataBackupManager(application)
    
    val userPrefs: StateFlow<UserPreferences?> = repository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    fun updateUsername(name: String) = viewModelScope.launch { repository.updateUsername(name) }
    fun updateHeight(cm: Float) = viewModelScope.launch { repository.updateHeight(cm) }
    fun updateGender(gender: String) = viewModelScope.launch { repository.updateGender(gender) }
    fun updateWeightUnit(unit: WeightUnit) = viewModelScope.launch { repository.updateWeightUnit(unit) }
    fun updateUseImperial(useImperial: Boolean) = viewModelScope.launch { repository.updateUseImperial(useImperial) }
    fun updateUseDarkTheme(darkTheme: Boolean) = viewModelScope.launch { repository.updateUseDarkTheme(darkTheme) }
    fun updateSoundsEnabled(sounds: Boolean) = viewModelScope.launch { repository.updateSoundsEnabled(sounds) }
    fun updateProfilePicUri(uri: String?) = viewModelScope.launch { repository.updateProfilePicUri(uri) }
    fun updateAvatarId(avatarId: String) = viewModelScope.launch { repository.updateAvatarId(avatarId) }
    fun updateUseCustomCalories(enabled: Boolean) = viewModelScope.launch { repository.updateUseCustomCalories(enabled) }
    fun updateCustomDailyCalories(calories: Int) = viewModelScope.launch { repository.updateCustomDailyCalories(calories) }
    
    fun deleteDeviceData(onComplete: () -> Unit = {}) = viewModelScope.launch {
        db.fastSessionDao().deleteAll()
        db.weightEntryDao().deleteAll()
        db.foodEntryDao().deleteAll()
        repository.resetAllPreferences()
        onComplete()
    }

    suspend fun getJsonExportData(): String {
        return backupManager.generateJsonBackup()
    }

    suspend fun getCsvExportData(): String {
        return backupManager.generateCsvBackup()
    }

    fun exportToFileUri(uri: Uri, content: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isBackingUp.value = true
            val success = backupManager.writeContentToUri(uri, content)
            _isBackingUp.value = false
            onResult(success)
        }
    }

    fun importFromJsonUri(uri: Uri, clearExisting: Boolean, onResult: (BackupImportResult) -> Unit) {
        viewModelScope.launch {
            _isBackingUp.value = true
            val result = backupManager.importFromJsonUri(uri, clearExisting)
            _isBackingUp.value = false
            onResult(result)
        }
    }

    fun checkForUpdates() {
        _updateCheckState.value = UpdateCheckState.Checking
        viewModelScope.launch {
            val currentVersion = BuildConfig.VERSION_NAME.ifEmpty { "1.0" }
            val defaultRepoUrl = "https://github.com/PASSK3YS/Chomp-Clock/releases"
            try {
                val releases = gitHubService.getAllReleases("PASSK3YS", "Chomp-Clock")
                if (releases.isEmpty()) {
                    _updateCheckState.value = UpdateCheckState.NoReleasesFound(
                        currentVersion = currentVersion,
                        repoUrl = defaultRepoUrl
                    )
                    return@launch
                }

                val latest = releases.first()
                val latestTag = (latest.tagName ?: "v1.0").removePrefix("v").trim()
                val currentClean = currentVersion.removePrefix("v").trim()

                // Find APK asset if present
                val apkAsset = latest.assets?.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }
                val downloadUrl = apkAsset?.browserDownloadUrl ?: latest.htmlUrl ?: defaultRepoUrl

                val isNewer = isVersionNewer(latestTag, currentClean)

                if (isNewer) {
                    _updateCheckState.value = UpdateCheckState.UpdateAvailable(
                        latestVersion = latest.tagName ?: "v$latestTag",
                        currentVersion = "v$currentClean",
                        releaseName = latest.name ?: "Version ${latest.tagName}",
                        releaseNotes = latest.body?.ifBlank { "New features, performance enhancements, and bug fixes." }
                            ?: "New features and bug fixes.",
                        downloadUrl = downloadUrl,
                        htmlUrl = latest.htmlUrl ?: defaultRepoUrl,
                        publishedAt = latest.publishedAt
                    )
                } else {
                    _updateCheckState.value = UpdateCheckState.UpToDate(
                        currentVersion = "v$currentClean",
                        releaseName = latest.name ?: "Version ${latest.tagName}",
                        releaseNotes = latest.body,
                        htmlUrl = latest.htmlUrl ?: defaultRepoUrl
                    )
                }
            } catch (e: Exception) {
                _updateCheckState.value = UpdateCheckState.Error(
                    errorMessage = e.localizedMessage ?: "Unable to connect to GitHub releases API",
                    repoUrl = defaultRepoUrl
                )
            }
        }
    }

    fun resetUpdateState() {
        _updateCheckState.value = UpdateCheckState.Idle
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        try {
            val latestParts = latest.split(".").mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }
            val currentParts = current.split(".").mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            return false
        } catch (e: Exception) {
            return latest != current
        }
    }
}
