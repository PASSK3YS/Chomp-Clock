package com.example.ui.fasting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.UserPreferences
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.UserAvatarView
import com.example.ui.settings.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FastingScreen(
    userPrefs: UserPreferences?,
    viewModel: FastingViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val isFasting by viewModel.isFasting.collectAsState()
    val elapsed by viewModel.elapsedMillis.collectAsState()
    val targetDuration by viewModel.targetDurationMillis.collectAsState()
    val pastSessions by viewModel.pastSessions.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()
    val achievements by viewModel.achievements.collectAsState()

    var showLogPastDialog by remember { mutableStateOf(false) }
    var showAchievementsDialog by remember { mutableStateOf(false) }
    var showCustomFastDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }

    val username = userPrefs?.username ?: "User"
    val avatarId = userPrefs?.avatarId

    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
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

    if (showAchievementsDialog) {
        AlertDialog(
            onDismissRequest = { showAchievementsDialog = false },
            title = { Text("Achievements", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    achievements.forEach { ach ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (ach.isUnlocked) "🏆" else "🔒", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(ach.title, fontWeight = FontWeight.Bold, color = if (ach.isUnlocked) Color.White else Color(0xFF71717A))
                                Text(ach.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFFA1A1AA))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAchievementsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Close")
                }
            },
            containerColor = Color(0xFF18181B),
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
                        color = Color(0xFFA1A1AA),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$greeting, $username",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF18181B),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                        Text(
                            text = "$streak",
                            color = Color(0xFFFFEDD5),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Surface(
                    onClick = { showAchievementsDialog = true },
                    color = Color(0xFF18181B),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color(0xFF27272A)),
                    modifier = Modifier.size(38.dp)
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
                        color = Color(0xFF1F1F23),
                        strokeWidth = 10.dp
                    )
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = if (isOvertime) Color(0xFF34D399) else Color(0xFF3B82F6),
                        strokeWidth = 10.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isOvertime) "COMPLETED (+OVERTIME)" else "REMAINING",
                            color = if (isOvertime) Color(0xFF34D399) else Color(0xFFA1A1AA),
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
                            color = Color.White
                        )
                        Text(
                            text = "Goal: ${targetHoursFormatted.removeSuffix(".0")} Hours",
                            color = Color(0xFF71717A),
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
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF18181B),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "ELAPSED TIME",
                            color = Color(0xFF71717A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val elapsedH = totalSeconds / 3600
                        val elapsedM = (totalSeconds % 3600) / 60
                        Text(
                            "${elapsedH}h ${elapsedM}m",
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.endFast() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF18181B),
                    contentColor = Color(0xFFF87171)
                ),
                border = BorderStroke(1.dp, Color(0xFF7F1D1D))
            ) {
                Text("End Fast", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        } else {
            Text(
                "START A FAST",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF71717A),
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
                        color = Color(0xFF18181B),
                        border = BorderStroke(1.dp, Color(0xFF27272A))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Text(sub, color = Color(0xFF71717A), fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Custom Fast Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomFastDialog = true },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF18181B),
                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f))
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
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Custom Fast Duration", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Set custom hours and minutes (e.g. 12h - 72h)", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                        }
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = "Open", tint = Color(0xFF60A5FA))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "RECENT FASTS",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF71717A),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                if (pastSessions.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF18181B),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF27272A))
                        ) {
                            Text(
                                "No fasts completed yet. Tap a preset above to begin!",
                                color = Color(0xFF71717A),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(pastSessions.take(5)) { session ->
                        val durationHrs = (session.endTime - session.startTime) / (1000 * 3600f)
                        val targetHrs = session.durationTargetMillis / (1000 * 3600f)
                        val hitGoal = durationHrs >= targetHrs
                        val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                            border = BorderStroke(1.dp, Color(0xFF27272A)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        dateFormat.format(Date(session.startTime)),
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "Target: ${String.format(Locale.getDefault(), "%.0fh", targetHrs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF71717A)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (hitGoal) Color(0xFF064E3B) else Color(0xFF27272A),
                                    border = BorderStroke(1.dp, if (hitGoal) Color(0xFF059669) else Color(0xFF3F3F46))
                                ) {
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%.1f", durationHrs)} hrs ${if (hitGoal) "✓" else ""}",
                                        color = if (hitGoal) Color(0xFF34D399) else Color(0xFFA1A1AA),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { showLogPastDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF4F4F5),
                contentColor = Color.Black
            )
        ) {
            Text("Log Past Fast Manually", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun MetabolicStageBox(hoursElapsed: Float, modifier: Modifier = Modifier) {
    val (stage, subtitle) = when {
        hoursElapsed < 2 -> Pair("Blood Sugar Rising", "Insulin response active")
        hoursElapsed < 4 -> "Blood Sugar Dropping" to "Digestion completing"
        hoursElapsed < 8 -> "Fat Burning" to "Liver glycogen depleted"
        hoursElapsed < 14 -> "Ketosis" to "Ketone bodies rising"
        hoursElapsed < 24 -> "Deep Ketosis" to "Fat oxidation peak"
        else -> "Autophagy" to "Cellular cleanup & renewal"
    }

    Surface(
        modifier = modifier,
        color = Color(0xFF18181B),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF27272A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "METABOLIC STATE",
                color = Color(0xFF71717A),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stage,
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                subtitle,
                color = Color(0xFF71717A),
                fontSize = 10.sp,
                maxLines = 1
            )
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
            color = Color(0xFF18181B),
            border = BorderStroke(1.dp, Color(0xFF27272A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "LOG COMPLETED FAST",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA1A1AA),
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Fast Duration (Hours)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF27272A),
                        focusedContainerColor = Color(0xFF27272A),
                        unfocusedContainerColor = Color(0xFF27272A)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = daysAgo,
                    onValueChange = { daysAgo = it },
                    label = { Text("How many days ago?") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF27272A),
                        focusedContainerColor = Color(0xFF27272A),
                        unfocusedContainerColor = Color(0xFF27272A)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFFA1A1AA))
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("Save Record")
                    }
                }
            }
        }
    }
}
