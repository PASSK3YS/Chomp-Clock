package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.StreakDetails
import com.example.ui.theme.AppTheme
import java.util.Locale

@Composable
fun DailyStreakDialog(
    streakDetails: StreakDetails,
    isFastingNow: Boolean,
    onDismiss: () -> Unit,
    onViewAchievements: () -> Unit,
    onStartFast: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .testTag("daily_streak_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = AppTheme.colors.surface,
            border = BorderStroke(1.dp, AppTheme.colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF97316).copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color(0xFFF97316)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔥", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Daily Fasting Streak",
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "${streakDetails.streakTierEmoji} ${streakDetails.streakTierTitle}",
                                color = Color(0xFFF97316),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_streak_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AppTheme.colors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Hero Streak Flame Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E140A),
                        border = BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Large Flame + Number
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFF97316).copy(alpha = 0.35f),
                                                Color(0xFFEA580C).copy(alpha = 0.08f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🔥",
                                    fontSize = 42.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${streakDetails.currentStreak} ${if (streakDetails.currentStreak == 1) "Day" else "Days"}",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp
                            )

                            Text(
                                text = "CURRENT STREAK",
                                color = Color(0xFFFDBA74),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Status Pill
                            val (statusText, statusBg, statusColor) = when {
                                streakDetails.isTodayCompleted -> Triple(
                                    "✓ Streak Locked In For Today",
                                    Color(0xFF065F46).copy(alpha = 0.4f),
                                    Color(0xFF34D399)
                                )
                                isFastingNow -> Triple(
                                    "⚡ Fast In Progress — Finish To Extend",
                                    Color(0xFF1E3A8A).copy(alpha = 0.4f),
                                    Color(0xFF60A5FA)
                                )
                                else -> Triple(
                                    "⏳ Complete A Fast Today To Keep Active",
                                    Color(0xFF78350F).copy(alpha = 0.4f),
                                    Color(0xFFFBBF24)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = statusBg,
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = streakDetails.motivationalText,
                                color = Color(0xFFFED7AA),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // 7-Day Rolling Streak Calendar Tracker
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AppTheme.colors.surfaceElevated,
                        border = BorderStroke(1.dp, AppTheme.colors.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "7-Day Consistency",
                                    color = AppTheme.colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val completedInWeek = streakDetails.recent7Days.count { it.isCompleted }
                                Text(
                                    text = "$completedInWeek / 7 Days Active",
                                    color = Color(0xFFF97316),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                streakDetails.recent7Days.forEach { dayItem ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = dayItem.dayName,
                                            color = if (dayItem.isToday) Color(0xFFF97316) else AppTheme.colors.textMuted,
                                            fontSize = 11.sp,
                                            fontWeight = if (dayItem.isToday) FontWeight.Bold else FontWeight.Medium
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        dayItem.isCompleted -> Color(0xFFEA580C)
                                                        dayItem.isToday && isFastingNow -> Color(0xFF3B82F6)
                                                        dayItem.isToday -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                        else -> Color(0xFF27272A)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when {
                                                dayItem.isCompleted -> {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                dayItem.isToday && isFastingNow -> {
                                                    Text("⚡", fontSize = 14.sp)
                                                }
                                                dayItem.isToday -> {
                                                    Text("🔥", fontSize = 14.sp)
                                                }
                                                else -> {
                                                    Text(
                                                        text = "•",
                                                        color = Color(0xFF71717A),
                                                        fontSize = 18.sp
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = dayItem.dateLabel,
                                            color = AppTheme.colors.textMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4-Stat Metrics Grid
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AppTheme.colors.surfaceElevated,
                        border = BorderStroke(1.dp, AppTheme.colors.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StreakStatBox(
                                    modifier = Modifier.weight(1f),
                                    emoji = "🔥",
                                    label = "Current Streak",
                                    value = "${streakDetails.currentStreak} d",
                                    highlightColor = Color(0xFFF97316)
                                )
                                StreakStatBox(
                                    modifier = Modifier.weight(1f),
                                    emoji = "🏆",
                                    label = "Best Streak",
                                    value = "${streakDetails.longestStreak} d",
                                    highlightColor = Color(0xFFF59E0B)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StreakStatBox(
                                    modifier = Modifier.weight(1f),
                                    emoji = "📅",
                                    label = "Fasting Days",
                                    value = "${streakDetails.totalFastingDays}",
                                    highlightColor = Color(0xFF38BDF8)
                                )
                                StreakStatBox(
                                    modifier = Modifier.weight(1f),
                                    emoji = "⏳",
                                    label = "Total Fasted",
                                    value = "${String.format(Locale.getDefault(), "%.0f", streakDetails.totalFastingHours)} hrs",
                                    highlightColor = Color(0xFF34D399)
                                )
                            }
                        }
                    }

                    // Next Milestone Goal Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AppTheme.colors.surfaceElevated,
                        border = BorderStroke(1.dp, AppTheme.colors.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Next Streak Milestone: ${streakDetails.nextMilestone} Days",
                                        color = AppTheme.colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = "${streakDetails.daysToNextMilestone}d to go",
                                    color = Color(0xFFF97316),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { streakDetails.milestoneProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFFF97316),
                                trackColor = AppTheme.colors.border
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Progress: ${streakDetails.currentStreak} / ${streakDetails.nextMilestone} days",
                                    color = AppTheme.colors.textMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${(streakDetails.milestoneProgress * 100).toInt()}%",
                                    color = Color(0xFFF97316),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // How Streaks Work Info Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "How Streaks Work in Chomp Clock",
                                    color = Color(0xFF93C5FD),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Complete at least one fast of your chosen duration every calendar day to keep your chain unbroken. Streaks unlock special badges, titles, and metabolic achievements!",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewAchievements,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("view_streak_achievements_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFF97316))
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Achievements",
                            color = Color(0xFFF97316),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    if (!isFastingNow && onStartFast != null) {
                        Button(
                            onClick = onStartFast,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("start_fast_streak_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEA580C)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Start Fast",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Got It",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakStatBox(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String,
    highlightColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = AppTheme.colors.surface,
        border = BorderStroke(1.dp, AppTheme.colors.border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    color = AppTheme.colors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = highlightColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
