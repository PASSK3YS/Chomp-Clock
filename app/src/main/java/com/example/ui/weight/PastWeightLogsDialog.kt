package com.example.ui.weight

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.WeightEntry
import com.example.data.repository.WeightUnit
import com.example.ui.components.SlideUpBottomSheetDialog
import com.example.ui.theme.AppTheme
import com.example.util.WeightUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun PastWeightLogsDialog(
    entries: List<WeightEntry>,
    weightUnit: WeightUnit,
    useImperial: Boolean,
    onDismiss: () -> Unit,
    onDeleteEntry: (WeightEntry) -> Unit,
    onLogNewWeighIn: () -> Unit
) {
    var entryToDelete by remember { mutableStateOf<WeightEntry?>(null) }

    val sortedEntries = remember(entries) {
        entries.sortedByDescending { it.date }
    }

    if (entryToDelete != null) {
        val entry = entryToDelete!!
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(entry.date))
        val weightStr = WeightUtils.formatWeight(entry.weightKg, weightUnit)
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Weight Log", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete the weigh-in of $weightStr recorded for $dateStr? This action cannot be undone.",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEntry(entry)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.danger)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
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
                                Icons.Default.Scale,
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
                                text = "Past Weight Logs",
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
                                    text = "${sortedEntries.size}",
                                    color = AppTheme.colors.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Weight history & body measurements",
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
                if (sortedEntries.isNotEmpty()) {
                    // Summary statistics card
                    item {
                        val totalLogs = sortedEntries.size
                        val latestEntry = sortedEntries.first()
                        val earliestEntry = sortedEntries.last()
                        val lowestWeightKg = sortedEntries.minOf { it.weightKg }
                        val highestWeightKg = sortedEntries.maxOf { it.weightKg }
                        val netChangeKg = latestEntry.weightKg - earliestEntry.weightKg

                        val formattedNetChange = when {
                            abs(netChangeKg) < 0.05f -> "0.0 ${weightUnit.shortName}"
                            weightUnit == WeightUnit.KG -> String.format(Locale.getDefault(), "%+.1f kg", netChangeKg)
                            weightUnit == WeightUnit.LBS -> String.format(Locale.getDefault(), "%+.1f lbs", netChangeKg * WeightUtils.LBS_PER_KG)
                            else -> {
                                val lbsChange = netChangeKg * WeightUtils.LBS_PER_KG
                                String.format(Locale.getDefault(), "%+.1f lbs", lbsChange)
                            }
                        }

                        val changeIcon = when {
                            netChangeKg < -0.05f -> Icons.Default.TrendingDown
                            netChangeKg > 0.05f -> Icons.Default.TrendingUp
                            else -> Icons.Default.TrendingFlat
                        }

                        val changeColor = when {
                            netChangeKg < -0.05f -> AppTheme.colors.success
                            netChangeKg > 0.05f -> Color(0xFFF97316)
                            else -> AppTheme.colors.textMuted
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = AppTheme.colors.surfaceElevated,
                            border = BorderStroke(1.dp, AppTheme.colors.border)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "WEIGHT PROGRESS OVERVIEW",
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
                                    WeightStatChip(
                                        label = "Total Logs",
                                        value = "$totalLogs",
                                        icon = Icons.Default.CheckCircle,
                                        modifier = Modifier.weight(1f),
                                        accentColor = AppTheme.colors.primary
                                    )
                                    WeightStatChip(
                                        label = "Latest",
                                        value = WeightUtils.formatWeight(latestEntry.weightKg, weightUnit),
                                        icon = Icons.Default.Scale,
                                        modifier = Modifier.weight(1.15f),
                                        accentColor = AppTheme.colors.primary
                                    )
                                    WeightStatChip(
                                        label = "Lowest",
                                        value = WeightUtils.formatWeight(lowestWeightKg, weightUnit),
                                        icon = Icons.Default.TrendingDown,
                                        modifier = Modifier.weight(1.15f),
                                        accentColor = Color(0xFF06B6D4)
                                    )
                                    WeightStatChip(
                                        label = "Net Change",
                                        value = formattedNetChange,
                                        icon = changeIcon,
                                        modifier = Modifier.weight(1.1f),
                                        accentColor = changeColor
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
                                "ALL RECORDED WEIGH-INS",
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

                    itemsIndexed(sortedEntries, key = { _, entry -> entry.id }) { index, entry ->
                        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        val nextOlderEntry = sortedEntries.getOrNull(index + 1)
                        val deltaKg = if (nextOlderEntry != null) entry.weightKg - nextOlderEntry.weightKg else null

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                            border = BorderStroke(1.dp, AppTheme.colors.border),
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
                                        text = dateFormat.format(Date(entry.date)),
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppTheme.colors.textPrimary,
                                        fontSize = 13.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (entry.waistCm != null && entry.waistCm > 0f) {
                                            val waistStr = if (useImperial) {
                                                String.format(Locale.getDefault(), "Waist: %.1f in", entry.waistCm / 2.54f)
                                            } else {
                                                String.format(Locale.getDefault(), "Waist: %.1f cm", entry.waistCm)
                                            }
                                            Icon(
                                                Icons.Default.Straighten,
                                                contentDescription = null,
                                                tint = AppTheme.colors.textMuted,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = waistStr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AppTheme.colors.textMuted,
                                                fontSize = 11.5.sp
                                            )
                                        } else {
                                            Text(
                                                text = "Weigh-in recorded",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AppTheme.colors.textMuted,
                                                fontSize = 11.5.sp
                                            )
                                        }

                                        if (deltaKg != null && abs(deltaKg) >= 0.05f) {
                                            val formattedDelta = when {
                                                weightUnit == WeightUnit.KG -> String.format(Locale.getDefault(), "%+.1f kg", deltaKg)
                                                else -> String.format(Locale.getDefault(), "%+.1f lbs", deltaKg * WeightUtils.LBS_PER_KG)
                                            }
                                            val isLoss = deltaKg < 0f
                                            Text(
                                                text = " • ${if (isLoss) "↓" else "↑"} $formattedDelta",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isLoss) AppTheme.colors.success else Color(0xFFF97316),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AppTheme.colors.primary.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = WeightUtils.formatWeight(entry.weightKg, weightUnit),
                                            color = AppTheme.colors.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { entryToDelete = entry },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete weight log",
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
                                            Icons.Default.Scale,
                                            contentDescription = null,
                                            tint = AppTheme.colors.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    "No Weight Logs Recorded Yet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = AppTheme.colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Your logged weigh-ins and waist measurements will appear here with history, progress trends, and deltas.",
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
                                onLogNewWeighIn()
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
                                "Log Weigh-In",
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
private fun WeightStatChip(
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
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
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
                fontSize = 11.5.sp,
                color = AppTheme.colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = AppTheme.colors.textMuted,
                maxLines = 1
            )
        }
    }
}
