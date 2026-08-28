package com.example.ui.fasting

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.FastSession
import com.example.data.model.AchievementCategory
import com.example.data.repository.UserPreferences
import com.example.ui.components.AchievementsDialog
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.DailyStreakDialog
import com.example.ui.components.UserAvatarView
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FastingScreen(
    userPrefs: UserPreferences?,
    viewModel: FastingViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val isFasting by viewModel.isFasting.collectAsState()
    val elapsed by viewModel.elapsedMillis.collectAsState()
    val targetDuration by viewModel.targetDurationMillis.collectAsState()
    val pastSessions by viewModel.pastSessions.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()
    val streakDetails by viewModel.streakDetails.collectAsState()
    val detailedAchievements by viewModel.detailedAchievements.collectAsState()

    var showLogPastDialog by remember { mutableStateOf(false) }
    var showRecentFastsDialog by remember { mutableStateOf(false) }
    var showStreakDialog by remember { mutableStateOf(false) }
    var showAchievementsDialog by remember { mutableStateOf(false) }
    var achievementsInitialCategory by remember { mutableStateOf(AchievementCategory.ALL) }
    var showCustomFastDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showMetabolicStatesDialog by remember { mutableStateOf(false) }
    var showEndFastConfirmDialog by remember { mutableStateOf(false) }
    var fastToDelete by remember { mutableStateOf<FastSession?>(null) }

    // Request notification permission for persistent silent fasting notification
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val username = userPrefs?.username ?: "User"
    val avatarId = userPrefs?.avatarId

    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    if (showEndFastConfirmDialog) {
        val totalSeconds = elapsed / 1000
        val targetSeconds = maxOf(1L, targetDuration / 1000)
        val isOvertime = totalSeconds >= targetSeconds
        val elapsedH = totalSeconds / 3600
        val elapsedM = (totalSeconds % 3600) / 60
        val targetH = String.format(Locale.getDefault(), "%.1f", targetDuration / (3600f * 1000f)).removeSuffix(".0")

        AlertDialog(
            onDismissRequest = { showEndFastConfirmDialog = false },
            title = {
                Text(
                    text = if (isOvertime) "Complete Fast?" else "End Fast Early?",
                    color = AppTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (isOvertime) {
                        Text(
                            text = "🎉 Awesome effort! You reached your $targetH-hour goal and completed ${elapsedH}h ${elapsedM}m total fasting time.",
                            color = AppTheme.colors.success,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        val remainingSec = targetSeconds - totalSeconds
                        val remH = remainingSec / 3600
                        val remM = (remainingSec % 3600) / 60
                        Text(
                            text = "You have fasted for ${elapsedH}h ${elapsedM}m out of your $targetH-hour goal (${remH}h ${remM}m remaining).",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to end this fasting session and record it in your history?",
                        color = AppTheme.colors.textPrimary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndFastConfirmDialog = false
                        viewModel.endFast()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOvertime) AppTheme.colors.success else AppTheme.colors.danger
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isOvertime) "End & Save Fast" else "End Fast Now",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndFastConfirmDialog = false }) {
                    Text("Keep Fasting", color = AppTheme.colors.primary, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = AppTheme.colors.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (showMetabolicStatesDialog) {
        val currentHours = if (isFasting) (elapsed / (1000f * 3600f)) else null
        MetabolicStateGuideDialog(
            hoursElapsed = currentHours,
            onDismiss = { showMetabolicStatesDialog = false }
        )
    }

    if (showCustomFastDialog) {
        CustomFastDialog(
            onDismiss = { showCustomFastDialog = false },
            onStartFast = { durationMillis ->
                viewModel.startFast(durationMillis)
            }
        )
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentAvatarId = avatarId,
            onDismiss = { showAvatarPicker = false },
            onAvatarSelected = { newAvatar ->
                settingsViewModel.updateAvatarId(newAvatar)
            }
        )
    }

    if (showStreakDialog) {
        DailyStreakDialog(
            streakDetails = streakDetails,
            isFastingNow = isFasting,
            onDismiss = { showStreakDialog = false },
            onViewAchievements = {
                showStreakDialog = false
                achievementsInitialCategory = AchievementCategory.STREAKS
                showAchievementsDialog = true
            },
            onStartFast = {
                showStreakDialog = false
                showCustomFastDialog = true
            }
        )
    }

    if (showAchievementsDialog) {
        AchievementsDialog(
            achievements = detailedAchievements,
            initialCategory = achievementsInitialCategory,
            onDismiss = {
                showAchievementsDialog = false
                achievementsInitialCategory = AchievementCategory.ALL
            }
        )
    }

    if (fastToDelete != null) {
        val session = fastToDelete!!
        val durationHrs = (session.endTime - session.startTime) / (1000 * 3600f)
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(session.startTime))
        AlertDialog(
            onDismissRequest = { fastToDelete = null },
            title = { Text("Delete Fast Record", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete this completed fast of ${String.format(Locale.getDefault(), "%.1f", durationHrs)} hrs recorded on $dateStr? This action cannot be undone.",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(session)
                        fastToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.danger)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fastToDelete = null }) {
                    Text("Cancel", color = AppTheme.colors.textMuted)
                }
            },
            containerColor = AppTheme.colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showLogPastDialog) {
        LogPastFastDialog(
            onDismiss = { showLogPastDialog = false },
            onSave = { start, end, target ->
                viewModel.logPastFast(start, end, target)
                showLogPastDialog = false
            }
        )
    }

    if (showRecentFastsDialog) {
        RecentFastsDialog(
            pastSessions = pastSessions,
            onDismiss = { showRecentFastsDialog = false },
            onDeleteSession = { viewModel.deleteSession(it) },
            onLogManualFast = { showLogPastDialog = true }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatarView(
                    avatarId = avatarId,
                    size = 44.dp,
                    onClick = { showAvatarPicker = true }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.UK)
                    Text(
                        text = dateFormat.format(Date()),
                        color = AppTheme.colors.textMuted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$greeting, $username",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.textPrimary
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showStreakDialog = true },
                    color = AppTheme.colors.surfaceElevated,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .testTag("daily_streak_button")
                        .animateContentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 13.sp, modifier = Modifier.padding(end = 4.dp))
                        Text(
                            text = "$streak",
                            color = AppTheme.colors.warning,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (streakDetails.isTodayCompleted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("✓", fontSize = 10.sp, color = AppTheme.colors.success, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Surface(
                    onClick = {
                        achievementsInitialCategory = AchievementCategory.ALL
                        showAchievementsDialog = true
                    },
                    color = AppTheme.colors.surfaceElevated,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("achievements_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Achievements",
                            tint = Color(0xFFEAB308),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isFasting) {
            val totalSeconds = elapsed / 1000
            val targetSeconds = maxOf(1L, targetDuration / 1000)
            val isOvertime = totalSeconds >= targetSeconds
            val remainingSeconds = if (isOvertime) totalSeconds - targetSeconds else targetSeconds - totalSeconds

            val hours = remainingSeconds / 3600
            val minutes = (remainingSeconds % 3600) / 60
            val seconds = remainingSeconds % 60

            val progress = (totalSeconds.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)
            val targetHoursFormatted = String.format(Locale.getDefault(), "%.1f", targetDuration / (3600f * 1000f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = AppTheme.colors.surfaceElevated,
                        strokeWidth = 10.dp
                    )
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = if (isOvertime) AppTheme.colors.success else AppTheme.colors.primary,
                        strokeWidth = 10.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isOvertime) "COMPLETED (+OVERTIME)" else "REMAINING",
                            color = if (isOvertime) AppTheme.colors.success else AppTheme.colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = "Goal: ${targetHoursFormatted.removeSuffix(".0")} Hours",
                            color = AppTheme.colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetabolicStageBox(
                    hoursElapsed = (totalSeconds / 3600f),
                    modifier = Modifier.weight(1f),
                    onClick = { showMetabolicStatesDialog = true }
                )
                Surface(
                    modifier = Modifier.weight(1f),
                    color = AppTheme.colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.border)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "ELAPSED TIME",
                            color = AppTheme.colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val elapsedH = totalSeconds / 3600
                        val elapsedM = (totalSeconds % 3600) / 60
                        Text(
                            "${elapsedH}h ${elapsedM}m",
                            color = AppTheme.colors.success,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metabolic Progression Card
            MetabolicProgressionBar(
                hoursElapsed = (totalSeconds / 3600f),
                onClick = { showMetabolicStatesDialog = true }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showEndFastConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.surface,
                    contentColor = AppTheme.colors.danger
                ),
                border = BorderStroke(1.dp, AppTheme.colors.danger.copy(alpha = 0.5f))
            ) {
                Text("End Fast", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        } else {
            Text(
                "START A FAST",
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.colors.textMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Presets grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("16:8", 16L * 3600 * 1000, "Standard"),
                    Triple("18:6", 18L * 3600 * 1000, "Fat burn"),
                    Triple("20:4", 20L * 3600 * 1000, "Warrior")
                ).forEach { (label, duration, sub) ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.startFast(duration) },
                        shape = RoundedCornerShape(16.dp),
                        color = AppTheme.colors.surface,
                        border = BorderStroke(1.dp, AppTheme.colors.border)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(label, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary, fontSize = 16.sp)
                            Text(sub, color = AppTheme.colors.textMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom Fast Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomFastDialog = true },
                shape = RoundedCornerShape(16.dp),
                color = AppTheme.colors.surface,
                border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = AppTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Custom Fast Duration", fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary, fontSize = 14.sp)
                            Text("Set custom hours and minutes (e.g. 12h - 72h)", color = AppTheme.colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = "Open", tint = AppTheme.colors.primary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metabolic States & Science Section (Compact Information Card)
            MetabolicSciencePreviewCard(onClick = { showMetabolicStatesDialog = true })

            Spacer(modifier = Modifier.height(10.dp))

            // RECENT FASTS ENTRY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "RECENT FASTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                if (pastSessions.isNotEmpty()) {
                    Text(
                        text = "View All (${pastSessions.size})",
                        color = AppTheme.colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { showRecentFastsDialog = true }
                            .padding(4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Clickable Recent Fasts Summary Card (Opens Dedicated Popup Menu)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRecentFastsDialog = true },
                shape = RoundedCornerShape(16.dp),
                color = AppTheme.colors.surface,
                border = BorderStroke(1.dp, AppTheme.colors.border)
            ) {
                if (pastSessions.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AppTheme.colors.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = AppTheme.colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "No fasts completed yet",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    "Tap presets above or log manual fasts",
                                    color = AppTheme.colors.textMuted,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open History",
                            tint = AppTheme.colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    val latestSession = pastSessions.first()
                    val latestDurationHrs = (latestSession.endTime - latestSession.startTime) / (1000 * 3600f)
                    val latestTargetHrs = latestSession.durationTargetMillis / (1000 * 3600f)
                    val hitGoal = latestDurationHrs >= latestTargetHrs
                    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AppTheme.colors.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = AppTheme.colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Latest Fast",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = AppTheme.colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (hitGoal) AppTheme.colors.success.copy(alpha = 0.15f) else AppTheme.colors.surfaceElevated,
                                        border = BorderStroke(1.dp, if (hitGoal) AppTheme.colors.success.copy(alpha = 0.4f) else AppTheme.colors.border)
                                    ) {
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%.1f", latestDurationHrs)}h ${if (hitGoal) "✓" else ""}",
                                            color = if (hitGoal) AppTheme.colors.success else AppTheme.colors.textSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${dateFormat.format(Date(latestSession.startTime))} • Target: ${String.format(Locale.getDefault(), "%.0fh", latestTargetHrs)}",
                                    color = AppTheme.colors.textMuted,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Open",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open Recent Fasts",
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { showRecentFastsDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AppTheme.colors.border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.textPrimary)
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Recent Fasts", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { showLogPastDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.primary,
                    contentColor = Color.White
                )
            ) {
                Text("Log Past Fast", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun MetabolicStageBox(
    hoursElapsed: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val stage = FastingMetabolicStages.getCurrentStage(hoursElapsed)

    Surface(
        modifier = modifier.clickable { onClick() },
        color = AppTheme.colors.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, stage.accentColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "METABOLIC STATE",
                    color = AppTheme.colors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(stage.icon, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stage.title,
                color = stage.accentColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Text(
                "Fuel: ${stage.primaryFuelSource.take(24)}...",
                color = AppTheme.colors.textSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MetabolicProgressionBar(
    hoursElapsed: Float,
    onClick: () -> Unit
) {
    val currentStage = FastingMetabolicStages.getCurrentStage(hoursElapsed)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = AppTheme.colors.surface,
        border = BorderStroke(1.dp, AppTheme.colors.border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧬", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "METABOLIC MILESTONES",
                        color = AppTheme.colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Guide & Science",
                        color = AppTheme.colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AppTheme.colors.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stage pill timeline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FastingMetabolicStages.ALL_STAGES.forEach { stage ->
                    val isPast = stage.hourEnd != null && hoursElapsed >= stage.hourEnd
                    val isCurrent = currentStage.id == stage.id

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isCurrent -> stage.accentColor.copy(alpha = 0.25f)
                            isPast -> AppTheme.colors.success.copy(alpha = 0.2f)
                            else -> AppTheme.colors.surfaceElevated
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isCurrent -> stage.accentColor
                                isPast -> AppTheme.colors.success.copy(alpha = 0.5f)
                                else -> AppTheme.colors.border
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stage.icon,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${stage.hourStart.toInt()}h",
                                color = when {
                                    isCurrent -> stage.accentColor
                                    isPast -> AppTheme.colors.success
                                    else -> AppTheme.colors.textMuted
                                },
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetabolicSciencePreviewCard(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = AppTheme.colors.surface,
        border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AppTheme.colors.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🧬", fontSize = 17.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "METABOLIC STATES GUIDE",
                        color = AppTheme.colors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        "7 Stages • Biomarkers & Science",
                        color = AppTheme.colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AppTheme.colors.surfaceElevated,
                border = BorderStroke(1.dp, AppTheme.colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Guide",
                        color = AppTheme.colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Guide",
                        tint = AppTheme.colors.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LogPastFastDialog(onDismiss: () -> Unit, onSave: (Long, Long, Long) -> Unit) {
    var hours by remember { mutableStateOf("16") }
    var daysAgo by remember { mutableStateOf("0") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AppTheme.colors.surface,
            border = BorderStroke(1.dp, AppTheme.colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "LOG COMPLETED FAST",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textMuted,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Fast Duration (Hours)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primary,
                        unfocusedBorderColor = AppTheme.colors.border,
                        focusedContainerColor = AppTheme.colors.inputBackground,
                        unfocusedContainerColor = AppTheme.colors.inputBackground,
                        focusedTextColor = AppTheme.colors.textPrimary,
                        unfocusedTextColor = AppTheme.colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = daysAgo,
                    onValueChange = { daysAgo = it },
                    label = { Text("How many days ago?") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primary,
                        unfocusedBorderColor = AppTheme.colors.border,
                        focusedContainerColor = AppTheme.colors.inputBackground,
                        unfocusedContainerColor = AppTheme.colors.inputBackground,
                        focusedTextColor = AppTheme.colors.textPrimary,
                        unfocusedTextColor = AppTheme.colors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = AppTheme.colors.textMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val h = hours.toFloatOrNull() ?: 16f
                            val d = daysAgo.toIntOrNull() ?: 0
                            val end = System.currentTimeMillis() - (d * 86400000L)
                            val start = end - (h * 3600 * 1000).toLong()
                            onSave(start, end, (h * 3600 * 1000).toLong())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary)
                    ) {
                        Text("Save Record", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
