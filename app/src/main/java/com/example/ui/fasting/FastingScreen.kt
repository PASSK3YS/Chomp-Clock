package com.example.ui.fasting

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.*

@Composable
fun FastingScreen(
    viewModel: FastingViewModel = viewModel(),
    username: String
) {
    val isFasting by viewModel.isFasting.collectAsState()
    val elapsed by viewModel.elapsedMillis.collectAsState()
    val pastSessions by viewModel.pastSessions.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    
    var showLogPastDialog by remember { mutableStateOf(false) }
    var showAchievementsDialog by remember { mutableStateOf(false) }

    val currentHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHourOfDay) {
        in 0..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    if (showAchievementsDialog) {
        com.example.ui.components.AchievementsDialog(
            achievements = achievements,
            onDismiss = { showAchievementsDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                val dateFormat = java.text.SimpleDateFormat("dd-MM-yyyy", Locale.UK)
                Text(dateFormat.format(Date()), color = androidx.compose.ui.graphics.Color(0xFFA1A1AA), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Text(text = "$greeting, $username", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = androidx.compose.ui.graphics.Color(0xFF18181B),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF27272A))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                        Text("$streak", color = androidx.compose.ui.graphics.Color(0xFFFFEDD5), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Surface(
                    onClick = { showAchievementsDialog = true },
                    color = androidx.compose.ui.graphics.Color(0xFF18181B),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF27272A)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(androidx.compose.material.icons.Icons.Default.Star, contentDescription = "Achievements", tint = androidx.compose.ui.graphics.Color(0xFFEAB308), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        if (isFasting) {
            val totalSeconds = elapsed / 1000
            val targetSeconds = 16L * 3600 // placeholder for goal, using 16h default for now
            val remainingSeconds = maxOf(0, targetSeconds - totalSeconds)
            
            val hours = remainingSeconds / 3600
            val minutes = (remainingSeconds % 3600) / 60
            val seconds = remainingSeconds % 60
            
            val progress = (totalSeconds.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(256.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
                        strokeWidth = 8.dp
                    )
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.ui.graphics.Color(0xFF3B82F6),
                        strokeWidth = 8.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("REMAINING", color = androidx.compose.ui.graphics.Color(0xFFA1A1AA), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                        Text("Goal: ${targetSeconds / 3600} Hours", color = androidx.compose.ui.graphics.Color(0xFF71717A), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetabolicStageBox(hoursElapsed = (totalSeconds / 3600f), modifier = Modifier.weight(1f))
                Surface(
                    modifier = Modifier.weight(1f),
                    color = androidx.compose.ui.graphics.Color(0xFF18181B),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF27272A))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("DAILY TARGET", color = androidx.compose.ui.graphics.Color(0xFF71717A), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("2,150 kcal", color = androidx.compose.ui.graphics.Color(0xFF34D399), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { viewModel.endFast() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18181B), contentColor = androidx.compose.ui.graphics.Color.White)
            ) {
                Text("End Fast", fontWeight = FontWeight.Bold)
            }
        } else {
            // ... (rest unchanged for now, maybe simple buttons)
            Text("Start a Fast", style = MaterialTheme.typography.titleMedium, color = androidx.compose.ui.graphics.Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { viewModel.startFast(16L * 3600 * 1000) }, colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18181B), contentColor = androidx.compose.ui.graphics.Color.White)) {
                    Text("16:8")
                }
                Button(onClick = { viewModel.startFast(18L * 3600 * 1000) }, colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18181B), contentColor = androidx.compose.ui.graphics.Color.White)) {
                    Text("18:6")
                }
                Button(onClick = { viewModel.startFast(20L * 3600 * 1000) }, colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18181B), contentColor = androidx.compose.ui.graphics.Color.White)) {
                    Text("20:4")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.startFast(24L * 3600 * 1000) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18181B), contentColor = androidx.compose.ui.graphics.Color.White)) {
                Text("Custom (24h)")
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showLogPastDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFF4F4F5), contentColor = androidx.compose.ui.graphics.Color.Black)
        ) {
            Text("Log past fast", fontWeight = FontWeight.Bold)
        }
    }

    if (showLogPastDialog) {
        // Simple dialog to log past fast. In a real app, use date/time pickers.
        AlertDialog(
            onDismissRequest = { showLogPastDialog = false },
            title = { Text("Log Past Fast") },
            text = { Text("Logging a past 16h fast starting yesterday for simplicity.") },
            confirmButton = {
                TextButton(onClick = {
                    val now = System.currentTimeMillis()
                    viewModel.logPastFast(now - (24 * 3600 * 1000), now - (8 * 3600 * 1000), 16L * 3600 * 1000)
                    showLogPastDialog = false
                }) { Text("Log") }
            },
            dismissButton = {
                TextButton(onClick = { showLogPastDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MetabolicStageBox(hoursElapsed: Float, modifier: Modifier = Modifier) {
    val stage = when {
        hoursElapsed < 2 -> "Blood sugar rises"
        hoursElapsed < 5 -> "Blood sugar drops"
        hoursElapsed < 8 -> "Glycogen drops"
        hoursElapsed < 10 -> "Gluconeogenesis"
        hoursElapsed < 12 -> "Fat burning"
        hoursElapsed < 14 -> "Ketosis"
        else -> "Autophagy"
    }

    Surface(
        modifier = modifier,
        color = androidx.compose.ui.graphics.Color(0xFF18181B),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF27272A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("CURRENT STAGE", color = androidx.compose.ui.graphics.Color(0xFF71717A), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(stage, color = androidx.compose.ui.graphics.Color(0xFF60A5FA), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
