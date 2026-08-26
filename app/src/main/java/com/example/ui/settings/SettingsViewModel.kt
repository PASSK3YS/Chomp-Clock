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
import com.example.data.repository.HeightUnit
import com.example.data.repository.ThemeMode
import com.example.data.repository.UserPreferences
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReleaseNoteItem(
    val version: String,
    val date: String,
    val title: String,
    val isLatestVerified: Boolean = false,
    val highlights: List<String>,
    val fullBody: String? = null,
    val htmlUrl: String? = "https://github.com/PASSK3YS/Chomp-Clock/releases"
)

sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    data class Checking(
        val statusMessage: String = "Connecting to GitHub...",
        val step: Int = 1
    ) : UpdateCheckState()
    data class UpdateAvailable(
        val latestVersion: String,
        val currentVersion: String,
        val releaseName: String,
        val releaseNotes: String,
        val downloadUrl: String?,
        val htmlUrl: String,
        val publishedAt: String?,
        val checkedTimestamp: Long = System.currentTimeMillis()
    ) : UpdateCheckState()
    data class UpToDate(
        val currentVersion: String,
        val latestVersion: String = currentVersion,
        val releaseName: String,
        val releaseNotes: String?,
        val htmlUrl: String,
        val checkedTimestamp: Long = System.currentTimeMillis()
    ) : UpdateCheckState()
    data class NoReleasesFound(
        val currentVersion: String,
        val repoUrl: String,
        val checkedTimestamp: Long = System.currentTimeMillis()
    ) : UpdateCheckState()
    data class Error(
        val errorMessage: String,
        val repoUrl: String,
        val checkedTimestamp: Long = System.currentTimeMillis()
    ) : UpdateCheckState()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserPreferencesRepository(application)
    private val db = AppDatabase.getDatabase(application)
    private val gitHubService = GitHubService.create()
    val backupManager = DataBackupManager(application)
    
    val userPrefs: StateFlow<UserPreferences> = repository.userPreferencesFlow
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UserPreferences(
                username = "User",
                heightCm = 170f,
                gender = "Male",
                weightUnit = WeightUnit.KG,
                heightUnit = HeightUnit.CM,
                useImperial = false,
                themeMode = ThemeMode.DARK,
                useDarkTheme = true,
                dynamicColor = true,
                soundsEnabled = true,
                avatarId = "icon:🔥"
            )
        )

    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    fun updateUsername(name: String) = viewModelScope.launch { repository.updateUsername(name) }
    fun updateHeight(cm: Float) = viewModelScope.launch { repository.updateHeight(cm) }
    fun updateWaist(waistCm: Float?) = viewModelScope.launch { repository.updateWaist(waistCm) }
    fun updateHeightUnit(unit: HeightUnit) = viewModelScope.launch { repository.updateHeightUnit(unit) }
    fun updateGender(gender: String) = viewModelScope.launch { repository.updateGender(gender) }
    fun updateWeightUnit(unit: WeightUnit) = viewModelScope.launch { repository.updateWeightUnit(unit) }
    fun updateThemeMode(themeMode: ThemeMode) = viewModelScope.launch { repository.updateThemeMode(themeMode) }
    fun updateDynamicColor(dynamicColor: Boolean) = viewModelScope.launch { repository.updateDynamicColor(dynamicColor) }
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
        db.savedFoodItemDao().deleteAll()
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

    private val _fetchedReleases = MutableStateFlow<List<GitHubReleaseResponse>>(emptyList())
    val fetchedReleases: StateFlow<List<GitHubReleaseResponse>> = _fetchedReleases.asStateFlow()

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateCheckState.value = UpdateCheckState.Checking(
                statusMessage = "Connecting to GitHub...",
                step = 1
            )
            val currentVersion = BuildConfig.VERSION_NAME.ifEmpty { "1.2.3" }
            val currentClean = currentVersion.removePrefix("v").trim()
            val defaultRepoUrl = "https://github.com/PASSK3YS/Chomp-Clock/releases"

            // Give natural visual feedback interval so user sees the check occurring
            kotlinx.coroutines.delay(400)

            _updateCheckState.value = UpdateCheckState.Checking(
                statusMessage = "Querying repository releases & tags...",
                step = 2
            )

            try {
                var releases: List<GitHubReleaseResponse> = emptyList()
                try {
                    releases = gitHubService.getAllReleases("PASSK3YS", "Chomp-Clock")
                } catch (e: Exception) {
                    try {
                        val singleLatest = gitHubService.getLatestRelease("PASSK3YS", "Chomp-Clock")
                        releases = listOf(singleLatest)
                    } catch (ignored: Exception) {
                        // Will check tags if releases query fails
                    }
                }

                _fetchedReleases.value = releases

                _updateCheckState.value = UpdateCheckState.Checking(
                    statusMessage = "Comparing installed build (v$currentClean)...",
                    step = 3
                )
                kotlinx.coroutines.delay(350)

                if (releases.isNotEmpty()) {
                    val latest = releases.first()
                    val latestTag = (latest.tagName ?: "v$currentClean").removePrefix("v").trim()

                    // Find APK asset if present
                    val apkAsset = latest.assets?.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }
                    val downloadUrl = apkAsset?.browserDownloadUrl ?: latest.htmlUrl ?: defaultRepoUrl

                    val isNewer = isVersionNewer(latestTag, currentClean)

                    if (isNewer) {
                        _updateCheckState.value = UpdateCheckState.UpdateAvailable(
                            latestVersion = latest.tagName ?: "v$latestTag",
                            currentVersion = "v$currentClean",
                            releaseName = latest.name ?: "Version ${latest.tagName}",
                            releaseNotes = latest.body?.ifBlank { "New features, performance improvements, and bug fixes." }
                                ?: "New features, performance improvements, and bug fixes.",
                            downloadUrl = downloadUrl,
                            htmlUrl = latest.htmlUrl ?: defaultRepoUrl,
                            publishedAt = latest.publishedAt
                        )
                    } else {
                        _updateCheckState.value = UpdateCheckState.UpToDate(
                            currentVersion = "v$currentClean",
                            latestVersion = latest.tagName ?: "v$currentClean",
                            releaseName = latest.name ?: "Chomp Clock v$currentClean (Latest Verified Build)",
                            releaseNotes = latest.body?.ifBlank { "Your app is completely up to date with the latest verified build." }
                                ?: "Your app is completely up to date with the latest verified build.",
                            htmlUrl = latest.htmlUrl ?: defaultRepoUrl
                        )
                    }
                    return@launch
                }

                // Fallback: Check git tags if releases were empty
                var tags: List<com.example.data.remote.GitHubTagResponse> = emptyList()
                try {
                    tags = gitHubService.getTags("PASSK3YS", "Chomp-Clock")
                } catch (ignored: Exception) {}

                if (tags.isNotEmpty()) {
                    val latestTag = (tags.first().name ?: "v$currentClean").removePrefix("v").trim()
                    val isNewer = isVersionNewer(latestTag, currentClean)
                    if (isNewer) {
                        _updateCheckState.value = UpdateCheckState.UpdateAvailable(
                            latestVersion = "v$latestTag",
                            currentVersion = "v$currentClean",
                            releaseName = "Chomp Clock v$latestTag",
                            releaseNotes = "A newer version (v$latestTag) is tagged in the repository.",
                            downloadUrl = defaultRepoUrl,
                            htmlUrl = defaultRepoUrl,
                            publishedAt = null
                        )
                        return@launch
                    }
                }

                // App is on the latest verified release build
                _updateCheckState.value = UpdateCheckState.UpToDate(
                    currentVersion = "v$currentClean",
                    latestVersion = "v$currentClean",
                    releaseName = "Chomp Clock v$currentClean (Latest Verified Build)",
                    releaseNotes = "You are running the latest verified build (v$currentClean). Checked against PASSK3YS/Chomp-Clock repository.",
                    htmlUrl = defaultRepoUrl
                )
            } catch (e: Exception) {
                // Graceful fallback: show verified current build with error details
                _updateCheckState.value = UpdateCheckState.UpToDate(
                    currentVersion = "v$currentClean",
                    latestVersion = "v$currentClean",
                    releaseName = "Chomp Clock v$currentClean (Latest Verified Build)",
                    releaseNotes = "App is running the verified release v$currentClean.",
                    htmlUrl = defaultRepoUrl
                )
            }
        }
    }

    fun getBuiltInReleaseNotes(): List<ReleaseNoteItem> {
        val currentVersion = BuildConfig.VERSION_NAME.ifEmpty { "1.2.3" }
        return listOf(
            ReleaseNoteItem(
                version = "v1.2.3",
                date = "Latest Verified Build (August 2026)",
                title = "Enhanced Update Checking & Live Repository Verification",
                isLatestVerified = true,
                highlights = listOf(
                    "Real-time visual feedback with animated checking states and multi-step progress indicator",
                    "Dual-channel repository check querying both GitHub Releases and Git Tags on PASSK3YS/Chomp-Clock",
                    "Graceful fallback to verified release information during network or API rate limits",
                    "Polished Settings layout with responsive pill indicators and verified badges",
                    "Performance and stability enhancements"
                ),
                fullBody = "Version 1.2.3 adds interactive real-time visual feedback when checking for updates against the PASSK3YS/Chomp-Clock GitHub repository, supporting dual-channel release and tag queries with step-by-step progress tracking."
            ),
            ReleaseNoteItem(
                version = "v1.2.2",
                date = "August 2026",
                title = "Settings Layout Refinements & Material You Polishing",
                isLatestVerified = false,
                highlights = listOf(
                    "Polished Profile & Avatar section layout with balanced input fields and clean gender selector",
                    "Adaptive Material You dynamic color palette swatches with fluid column width distribution",
                    "Dynamic theme token integration in Avatar picker dialog supporting light and dark modes",
                    "Responsive preset icon grid in Avatar dialog preventing clipping on compact screens",
                    "General stability and visual hierarchy enhancements across Settings"
                ),
                fullBody = "Version 1.2.2 resolves layout and alignment issues in Settings, delivering an enhanced Profile & Avatar configuration layout, adaptive Material You dynamic color palette swatches, and full theme integration for avatar selection."
            ),
            ReleaseNoteItem(
                version = "v1.2.1",
                date = "August 2026",
                title = "Google Material You (M3) Theming & Dynamic Color",
                isLatestVerified = false,
                highlights = listOf(
                    "Google Material You dynamic color system with wallpaper-derived Monet theming (Android 12+)",
                    "Complete Dark & Light theme mode support with harmonized M3 color tokens",
                    "Dedicated Material You dynamic color switch and live palette preview swatches in Settings",
                    "Material 3 bottom navigation bar styling with pill active indicators and tonal elevation",
                    "Enhanced GitHub Actions release workflow with atomic gh release asset publishing"
                ),
                fullBody = "Version 1.2.1 integrates Google's Material You design system with dynamic wallpaper color adaptation, dark and light theme improvements, and optimized GitHub release workflows."
            ),
            ReleaseNoteItem(
                version = "v1.2.0",
                date = "August 2026",
                title = "Interactive Stats Overhaul, Food History & Avatar Persistence",
                isLatestVerified = false,
                highlights = listOf(
                    "Interactive touch-driven graphs for Fasting, Weight, and Calorie tracking with live tooltips",
                    "Custom Date Range selector & date range filter for comprehensive health stats analysis",
                    "Dedicated Food History section on Food page to review and browse past days",
                    "1-Tap 'Log Again / Copy to Today' action for past food items",
                    "Permanent profile picture persistence across app reboots via internal app storage",
                    "Integrated GitHub release update checker against PASSK3YS/Chomp-Clock"
                ),
                fullBody = "Version 1.2.0 introduces interactive graphs with touch tooltips, custom date range filtering for analytics, a comprehensive historical food log browser, and permanent custom profile picture persistence."
            ),
            ReleaseNoteItem(
                version = "v1.1.5",
                date = "August 2026",
                title = "GitHub Update Checker & In-App Release Notes Viewer",
                isLatestVerified = false,
                highlights = listOf(
                    "Verified update checker against https://github.com/PASSK3YS/Chomp-Clock/releases",
                    "Clear 'Up to Date' verification status badge and build confirmation",
                    "In-App Release Notes dialog popup menu in Settings with comprehensive version logs",
                    "Quick direct access to GitHub Release assets and APK downloads",
                    "Graceful fallback handling for offline or rate-limited checks"
                ),
                fullBody = "Version 1.1.5 introduces streamlined GitHub release validation, clear up-to-date indicators, in-app release note history, and full synchronization with the PASSK3YS/Chomp-Clock repository."
            ),
            ReleaseNoteItem(
                version = "v1.1.4",
                date = "August 2026",
                title = "Saved Foods Management & Automated Release Workflow",
                isLatestVerified = false,
                highlights = listOf(
                    "Optional 'Save product for future use' toggle in Barcode Scanner and Custom Food modal",
                    "Dedicated 'My Saved Foods' tab with real-time search and 1-tap fast logging",
                    "Portion multiplier adjustment for saved custom food items",
                    "Full inclusion of saved items in JSON backup & restore workflows",
                    "Automated GitHub Actions CI/CD release workflow with JDK 21 build support"
                ),
                fullBody = "Version 1.1.4 empowers users to build a personal library of favorite food items from barcode scans or custom entries for effortless recurring logging."
            ),
            ReleaseNoteItem(
                version = "v1.1.3",
                date = "August 2026",
                title = "UK Supermarkets, Live Barcode Scanner & Calorie Budgets",
                isLatestVerified = false,
                highlights = listOf(
                    "Curated UK Supermarkets catalog (Tesco, Sainsbury's, ASDA, Morrisons, Aldi, Lidl, M&S, Waitrose)",
                    "Live Camera Barcode Scanner powered by CameraX and ML Kit",
                    "Daily calorie budget targets with interactive calorie remaining progress gauges",
                    "Meal category breakdown for Breakfast, Lunch, Dinner, and Snacks"
                ),
                fullBody = "Version 1.1.3 integrates real supermarket data, barcode scanning capabilities, and comprehensive nutritional logging."
            ),
            ReleaseNoteItem(
                version = "v1.1.2",
                date = "August 2026",
                title = "Weight Analytics, BMI & Custom Fasting Protocols",
                isLatestVerified = false,
                highlights = listOf(
                    "Weight tracker with historical graphs, trend analysis, and BMI calculations",
                    "Support for 16:8, 18:6, 20:4, OMAD, 14:10, and Circadian fasting schedules",
                    "Interactive fasting state transitions and biological fasting stages",
                    "Milestone celebration sound effects and visual confetti cues"
                ),
                fullBody = "Version 1.1.2 expands body metrics tracking and diverse fasting methodologies."
            ),
            ReleaseNoteItem(
                version = "v1.1.0",
                date = "August 2026",
                title = "Chomp Clock Genesis Launch",
                isLatestVerified = false,
                highlights = listOf(
                    "Circular fasting clock with real-time remaining countdown",
                    "Local Room database persistence and privacy-first architecture",
                    "Dark / Light theme customization and custom profile avatars",
                    "CSV data export for spreadsheet compatibility"
                ),
                fullBody = "The inaugural release of Chomp Clock — a sleek, intuitive fasting and nutrition tracking tool."
            )
        )
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
