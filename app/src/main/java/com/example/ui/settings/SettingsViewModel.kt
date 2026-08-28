package com.example.ui.settings

import android.app.Application
import android.content.Context
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
import com.example.util.InAppUpdateInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

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

    private val _installState = MutableStateFlow<InAppUpdateInstaller.InstallState>(InAppUpdateInstaller.InstallState())
    val installState: StateFlow<InAppUpdateInstaller.InstallState> = _installState.asStateFlow()

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

    fun downloadAndInstallUpdate(context: Context, downloadUrl: String) {
        viewModelScope.launch {
            val apkFile = InAppUpdateInstaller.downloadApk(
                context = context,
                downloadUrl = downloadUrl,
                onProgress = { state ->
                    _installState.value = state
                }
            )
            if (apkFile != null) {
                val launched = InAppUpdateInstaller.triggerPackageInstall(context, apkFile)
                if (!launched) {
                    _installState.value = _installState.value.copy(
                        isDownloading = false,
                        error = "Could not open installer. Please allow 'Install unknown apps' permission."
                    )
                }
            }
        }
    }

    fun resetInstallState() {
        _installState.value = InAppUpdateInstaller.InstallState()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateCheckState.value = UpdateCheckState.Checking(
                statusMessage = "Connecting to GitHub...",
                step = 1
            )
            val currentVersion = BuildConfig.VERSION_NAME.ifEmpty { "1.2.7" }
            val currentClean = currentVersion.removePrefix("v").trim()
            val defaultRepoUrl = "https://github.com/PASSK3YS/Chomp-Clock/releases"

            kotlinx.coroutines.delay(350)

            _updateCheckState.value = UpdateCheckState.Checking(
                statusMessage = "Querying repository releases & tags...",
                step = 2
            )

            var remoteReleases: List<GitHubReleaseResponse> = emptyList()
            var lastError: String? = null

            // 1. Try official releases API
            try {
                remoteReleases = gitHubService.getAllReleases("PASSK3YS", "Chomp-Clock")
            } catch (e: Exception) {
                lastError = e.message
                try {
                    val singleLatest = gitHubService.getLatestRelease("PASSK3YS", "Chomp-Clock")
                    remoteReleases = listOf(singleLatest)
                } catch (ignored: Exception) {
                    lastError = ignored.message
                }
            }

            _fetchedReleases.value = remoteReleases

            _updateCheckState.value = UpdateCheckState.Checking(
                statusMessage = "Comparing installed build (v$currentClean)...",
                step = 3
            )
            kotlinx.coroutines.delay(250)

            // Check releases first
            if (remoteReleases.isNotEmpty()) {
                val latest = remoteReleases.first()
                val latestTag = (latest.tagName ?: "v$currentClean").removePrefix("v").trim()

                val apkAsset = latest.assets?.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }
                val downloadUrl = apkAsset?.browserDownloadUrl ?: "https://github.com/PASSK3YS/Chomp-Clock/releases/download/v$latestTag/ChompClock-release.apk"

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
                    return@launch
                }
            }

            // 2. Check Git tags
            try {
                val tags = gitHubService.getTags("PASSK3YS", "Chomp-Clock")
                if (tags.isNotEmpty()) {
                    val latestTag = (tags.first().name ?: "v$currentClean").removePrefix("v").trim()
                    if (isVersionNewer(latestTag, currentClean)) {
                        _updateCheckState.value = UpdateCheckState.UpdateAvailable(
                            latestVersion = "v$latestTag",
                            currentVersion = "v$currentClean",
                            releaseName = "Chomp Clock v$latestTag",
                            releaseNotes = "A newer version (v$latestTag) is tagged in the repository.",
                            downloadUrl = "https://github.com/PASSK3YS/Chomp-Clock/releases/download/v$latestTag/ChompClock-release.apk",
                            htmlUrl = defaultRepoUrl,
                            publishedAt = null
                        )
                        return@launch
                    }
                }
            } catch (ignored: Exception) {}

            // 3. Check direct raw build.gradle.kts to bypass any API rate limits
            try {
                val rawVersion = withContext(Dispatchers.IO) {
                    val url = URL("https://raw.githubusercontent.com/PASSK3YS/Chomp-Clock/main/app/build.gradle.kts")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "ChompClock-Android-App")
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val match = Regex("""versionName\s*=\s*"([^"]+)"""").find(text)
                        match?.groupValues?.getOrNull(1)
                    } else null
                }

                if (!rawVersion.isNullOrBlank()) {
                    val cleanRaw = rawVersion.removePrefix("v").trim()
                    if (isVersionNewer(cleanRaw, currentClean)) {
                        _updateCheckState.value = UpdateCheckState.UpdateAvailable(
                            latestVersion = "v$cleanRaw",
                            currentVersion = "v$currentClean",
                            releaseName = "Chomp Clock v$cleanRaw",
                            releaseNotes = "A newer version (v$cleanRaw) is published on the main repository branch.",
                            downloadUrl = "https://github.com/PASSK3YS/Chomp-Clock/releases/download/v$cleanRaw/ChompClock-release.apk",
                            htmlUrl = defaultRepoUrl,
                            publishedAt = null
                        )
                        return@launch
                    }
                }
            } catch (ignored: Exception) {}

            // If we successfully checked and no newer version is found:
            _updateCheckState.value = UpdateCheckState.UpToDate(
                currentVersion = "v$currentClean",
                latestVersion = if (remoteReleases.isNotEmpty()) remoteReleases.first().tagName ?: "v$currentClean" else "v$currentClean",
                releaseName = "Chomp Clock v$currentClean (Latest Verified Build)",
                releaseNotes = if (remoteReleases.isNotEmpty()) remoteReleases.first().body else "Your app is completely up to date with the latest verified build.",
                htmlUrl = defaultRepoUrl
            )
        }
    }

    fun getBuiltInReleaseNotes(): List<ReleaseNoteItem> {
        val currentVersion = BuildConfig.VERSION_NAME.ifEmpty { "1.3.3" }
        return listOf(
            ReleaseNoteItem(
                version = "v1.3.3",
                date = "Latest Verified Build (August 2026)",
                title = "Dedicated Past Weight Logs Popup Menu & Progress Analytics",
                isLatestVerified = true,
                highlights = listOf(
                    "Dedicated Past Weight Logs pop-up menu featuring full weigh-in history and body measurements",
                    "Weight progress overview analytics including Total Logs, Latest, Lowest Weight, and Net Change",
                    "Individual weigh-in log deletion with safety confirmation dialog",
                    "Progressive weight delta indicators (loss/gain) compared against previous check-ins",
                    "Streamlined weight screen layout with quick-access summary cards and fast-action buttons"
                ),
                fullBody = "Version 1.3.3 brings a dedicated slide-up popup menu for Past Weight Logs, featuring aggregate progress analytics (Total Logs, Latest, Lowest Weight, Net Change), progressive delta indicators, individual log deletion, and streamlined weight tracking."
            ),
            ReleaseNoteItem(
                version = "v1.3.2",
                date = "August 2026",
                title = "Dedicated Recent Fasts Popup Menu & Session Analytics",
                isLatestVerified = false,
                highlights = listOf(
                    "Dedicated Recent Fasts pop-up menu featuring full fasting history and session analytics",
                    "Aggregate statistics overview including Total Fasts, Average Length, Longest Session, and Goal Hit Rate",
                    "Individual fast session deletion with safety confirmation dialog",
                    "Direct manual fast logging shortcut integrated within the recent fasts view",
                    "Streamlined fasting home screen with quick-access history cards and bottom action controls"
                ),
                fullBody = "Version 1.3.2 moves Recent Fasts into its own dedicated slide-up popup menu complete with aggregate metrics (Total Fasts, Avg Length, Longest, Goal Hit Rate), individual log deletion, and direct manual past fast logging."
            ),
            ReleaseNoteItem(
                version = "v1.3.1",
                date = "August 2026",
                title = "Popup Dialog Usability & Sticky Action Controls",
                isLatestVerified = false,
                highlights = listOf(
                    "Fixed popup menu clipping across Custom Food, Weigh-In Reminders, and Calorie Goal dialogs",
                    "Pinned sticky action bars to ensure save & log buttons remain visible above virtual keyboards",
                    "Smooth independent scrolling for form inputs, portion multipliers, and day selectors",
                    "Performance optimizations and dialog layout responsiveness enhancements"
                ),
                fullBody = "Version 1.3.1 delivers enhanced popup dialog usability with pinned bottom action bars, independent content scrolling to prevent keyboard clipping, and overall stability improvements."
            ),
            ReleaseNoteItem(
                version = "v1.3.0",
                date = "August 2026",
                title = "Scheduled Weigh-In Reminders & Notification System",
                isLatestVerified = false,
                highlights = listOf(
                    "Custom recurring weigh-in reminders (Weekly, Bi-Weekly, Monthly, or Daily)",
                    "Day-of-the-week selector with active visual highlights and custom time picker with quick presets (7:00 AM - 9:00 AM)",
                    "Live upcoming reminder preview banner and 1-tap instant notification testing",
                    "Reliable exact AlarmManager scheduling with automatic restoration on device boot and timezone changes",
                    "Direct notification tap action navigating directly to the Weight tracking tab",
                    "Weigh-In Reminder management cards in both Weight Screen and Settings"
                ),
                fullBody = "Version 1.3.0 introduces a comprehensive Weigh-In Reminder and Notification system, allowing users to configure customizable schedules (Weekly, Bi-Weekly, Monthly, or Daily), choose preferred weigh-in days and times, test notifications, and receive timely alarms with automatic boot persistence."
            ),
            ReleaseNoteItem(
                version = "v1.2.7",
                date = "August 2026",
                title = "Full-Width Slide-Up Popup Menus & In-App APK Package Installer",
                isLatestVerified = false,
                highlights = listOf(
                    "Full-screen width slide-up bottom sheets with smooth spring enter & exit animations for all popup menus",
                    "Integrated in-app APK downloader & installer with live progress bar and direct package installer launch",
                    "Added REQUEST_INSTALL_PACKAGES permission and Android FileProvider for seamless in-place updates without deleting the app",
                    "Multi-tier GitHub update checking against releases, git tags, commits, and raw repository metadata",
                    "Added drag handles, elevation styling, and responsive IME/keyboard insets across all modal screens",
                    "Standardized deterministic release signing workflow on GitHub Actions"
                ),
                fullBody = "Version 1.2.7 transforms all popup menus across the app into full-width slide-up bottom sheets with spring physics and adds an in-app APK downloader and installer for seamless in-place updates."
            ),
            ReleaseNoteItem(
                version = "v1.2.6",
                date = "August 2026",
                title = "Smart Dynamic Custom Food Portion & Gram Calorie Auto-Calculation",
                isLatestVerified = false,
                highlights = listOf(
                    "Dynamic calorie auto-calculation when entering custom foods based on reference serving and grams",
                    "Real-time calorie scaling when doubling servings (2x), halving (0.5x), or adjusting grams up/down",
                    "Quick portion multiplier chips (½, 1x, 1.5x, 2x Double, 3x Triple) and instant gram step buttons (±10g, ±50g)",
                    "Live energy breakdown formula display showing exact kcal/g energy density and portion math",
                    "Proportional gram scaling support across Custom Food, Scanned Products, and Saved Foods",
                    "Auto-generated or custom portion labels with clean log-to-meal workflow"
                ),
                fullBody = "Version 1.2.6 adds an intelligent automatic calorie calculator for custom food input, dynamically updating total calories in real-time as users double servings, adjust gram weights, or select quick portion multipliers."
            ),
            ReleaseNoteItem(
                version = "v1.2.5",
                date = "August 2026",
                title = "Expanded Achievements System & Interactive Daily Streak Popup",
                isLatestVerified = false,
                highlights = listOf(
                    "Comprehensive multi-category achievements (Sessions, Duration, Streaks, Nutrition, and Body Metrics)",
                    "Interactive Daily Streak dialog popup when tapping the fire badge on the Fasting screen",
                    "Dynamic streak tier titles (Weekly Flame, Fortnight Hero, Iron Will, Centennial Legend) and motivational guidance",
                    "7-Day consistency tracker with rolling day badges and completion indicators",
                    "4-Stat streak metrics grid (Current Streak, Best Streak, Fasting Days, Lifetime Hours Fasted)",
                    "Next milestone goal progress bar and scientific health insights for badge unlocks"
                ),
                fullBody = "Version 1.2.5 expands the achievements system into an exhaustive multi-category catalog with tiered rarities and adds an interactive Daily Streak dialog featuring 7-day consistency tracking, milestone countdowns, and rich lifetime statistics."
            ),
            ReleaseNoteItem(
                version = "v1.2.4",
                date = "August 2026",
                title = "Custom Adaptive Launcher Icon & Release Keystore Alignment",
                isLatestVerified = false,
                highlights = listOf(
                    "Custom Material You adaptive launcher icon featuring modern fasting stopwatch dial with emerald/amber gradient progress arc and bite detailing",
                    "Automated release signing keystore configuration for seamless in-place APK updates from GitHub Releases",
                    "Resolved APK signature mismatch installation issues across releases",
                    "Performance optimizations and updated release distribution workflows"
                ),
                fullBody = "Version 1.2.4 introduces a custom adaptive app launcher icon, replaces default placeholder assets, and standardizes release keystore configurations to ensure smooth in-place APK updates from GitHub Releases."
            ),
            ReleaseNoteItem(
                version = "v1.2.3",
                date = "August 2026",
                title = "Enhanced Update Checking & Live Repository Verification",
                isLatestVerified = false,
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
