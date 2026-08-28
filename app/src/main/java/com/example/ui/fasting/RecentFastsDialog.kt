package com.example.ui.fasting

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FastSession
import com.example.ui.components.SlideUpBottomSheetDialog
import com.example.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentFastsDialog(
    pastSessions: List<FastSession>,
    onDismiss: () -> Unit,
    onDeleteSession: (FastSession) -> Unit,
    onLogManualFast: () -> Unit
) {
    var fastToDelete by remember { mutableStateOf<FastSession?>(null) }

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
                        onDeleteSession(session)
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

    SlideUpBottomSheetDialog(
        onDismissRequest = onDismiss,
        maxHeightFraction = 0.92f
    ) { dismissWithAnim ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = AppTheme.colors.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Recent Fasts",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AppTheme.colors.primary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "${pastSessions.size}",
                                    color = AppTheme.colors.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Fasting history & performance logs",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textMuted
                        )
                    }
                }

                IconButton(
                    onClick = { dismissWithAnim() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppTheme.colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (pastSessions.isNotEmpty()) {
                    // Summary statistics card
                    item {
                        val totalFasts = pastSessions.size
                        val totalDurationHours = pastSessions.sumOf { (it.endTime - it.startTime).toDouble() } / (1000 * 3600.0)
                        val avgDurationHours = if (totalFasts > 0) totalDurationHours / totalFasts else 0.0
                        val longestFastHours = pastSessions.maxOfOrNull { (it.endTime - it.startTime).toDouble() }?.div(1000 * 3600.0) ?: 0.0
                        val hitGoalCount = pastSessions.count { (it.endTime - it.startTime) >= it.durationTargetMillis }
                        val goalHitRate = if (totalFasts > 0) ((hitGoalCount.toFloat() / totalFasts) * 100).toInt() else 0

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = AppTheme.colors.surfaceElevated,
                            border = BorderStroke(1.dp, AppTheme.colors.border)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "FASTING SUMMARY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = AppTheme.colors.textMuted
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FastStatChip(
                                        label = "Total Fasts",
                                        value = "$totalFasts",
                                        icon = Icons.Default.CheckCircle,
                                        modifier = Modifier.weight(1f),
                                        accentColor = AppTheme.colors.primary
                                    )
                                    FastStatChip(
                                        label = "Avg Length",
                                        value = String.format(Locale.getDefault(), "%.1fh", avgDurationHours),
                                        icon = Icons.Default.QueryBuilder,
                                        modifier = Modifier.weight(1f),
                                        accentColor = Color(0xFF06B6D4)
                                    )
                                    FastStatChip(
                                        label = "Longest",
                                        value = String.format(Locale.getDefault(), "%.1fh", longestFastHours),
                                        icon = Icons.Default.TrendingUp,
                                        modifier = Modifier.weight(1f),
                                        accentColor = Color(0xFFEAB308)
                                    )
                                    FastStatChip(
                                        label = "Goal Hit",
                                        value = "$goalHitRate%",
                                        icon = Icons.Default.Speed,
                                        modifier = Modifier.weight(1f),
                                        accentColor = AppTheme.colors.success
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ALL COMPLETED SESSIONS",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.textMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "Latest first",
                                fontSize = 11.sp,
                                color = AppTheme.colors.textMuted
                            )
                        }
                    }

                    items(pastSessions, key = { it.id }) { session ->
                        val durationHrs = (session.endTime - session.startTime) / (1000 * 3600f)
                        val targetHrs = session.durationTargetMillis / (1000 * 3600f)
                        val hitGoal = durationHrs >= targetHrs
                        val startDateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        val endDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                            border = BorderStroke(
                                1.dp,
                                if (hitGoal) AppTheme.colors.success.copy(alpha = 0.35f) else AppTheme.colors.border
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = startDateFormat.format(Date(session.startTime)),
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppTheme.colors.textPrimary,
                                        fontSize = 13.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Target: ${String.format(Locale.getDefault(), "%.0fh", targetHrs)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppTheme.colors.textMuted,
                                            fontSize = 11.5.sp
                                        )
                                        Text(
                                            text = " • Finished at ${endDateFormat.format(Date(session.endTime))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppTheme.colors.textMuted,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (hitGoal) AppTheme.colors.success.copy(alpha = 0.15f) else AppTheme.colors.surfaceElevated,
                                        border = BorderStroke(
                                            1.dp,
                                            if (hitGoal) AppTheme.colors.success.copy(alpha = 0.4f) else AppTheme.colors.border
                                        )
                                    ) {
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%.1f", durationHrs)} hrs ${if (hitGoal) "✓" else ""}",
                                            color = if (hitGoal) AppTheme.colors.success else AppTheme.colors.textSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { fastToDelete = session },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete fast log",
                                            tint = AppTheme.colors.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            color = AppTheme.colors.surfaceElevated,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, AppTheme.colors.border)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AppTheme.colors.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            tint = AppTheme.colors.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    "No Fasts Recorded Yet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = AppTheme.colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Your completed and manually logged fasts will appear here with detailed statistics, goal compliance, and durations.",
                                    color = AppTheme.colors.textMuted,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Sticky Bottom Action Bar
            Surface(
                color = AppTheme.colors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 14.dp)
                ) {
                    HorizontalDivider(color = AppTheme.colors.border.copy(alpha = 0.5f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                dismissWithAnim()
                                onLogManualFast()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Log Past Fast",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { dismissWithAnim() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                "Done",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FastStatChip(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = AppTheme.colors.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppTheme.colors.border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = AppTheme.colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = label,
                fontSize = 9.5.sp,
                color = AppTheme.colors.textMuted,
                maxLines = 1
            )
        }
    }
}
