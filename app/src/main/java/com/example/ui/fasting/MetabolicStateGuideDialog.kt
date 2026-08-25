package com.example.ui.fasting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

@Composable
fun MetabolicStateGuideDialog(
    hoursElapsed: Float?,
    onDismiss: () -> Unit
) {
    var expandedStageId by remember {
        mutableStateOf(
            if (hoursElapsed != null && hoursElapsed > 0) {
                FastingMetabolicStages.getCurrentStage(hoursElapsed).id
            } else {
                FastingMetabolicStages.ALL_STAGES.first().id
            }
        )
    }

    val currentStage = hoursElapsed?.let { FastingMetabolicStages.getCurrentStage(it) }
    val nextStage = hoursElapsed?.let { FastingMetabolicStages.getNextStage(it) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF121215),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                            color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🧬", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Metabolic States Guide",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Text(
                                "Biological & Cellular Milestones",
                                color = Color(0xFF71717A),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA1A1AA))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Fast Current Stage Banner (if fasting)
                if (hoursElapsed != null && currentStage != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF18181B),
                        border = BorderStroke(1.dp, currentStage.accentColor.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(currentStage.icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "CURRENT METABOLIC STAGE",
                                            color = currentStage.accentColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            currentStage.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = currentStage.accentColor.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, currentStage.accentColor)
                                ) {
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%.1f", hoursElapsed)}h elapsed",
                                        color = currentStage.accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (nextStage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val hoursToNext = maxOf(0f, nextStage.hourStart - hoursElapsed)
                                val currentStageSpan = nextStage.hourStart - currentStage.hourStart
                                val stageProgress = if (currentStageSpan > 0) {
                                    ((hoursElapsed - currentStage.hourStart) / currentStageSpan).coerceIn(0f, 1f)
                                } else 0f

                                LinearProgressIndicator(
                                    progress = { stageProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = currentStage.accentColor,
                                    trackColor = Color(0xFF27272A),
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Next: ${nextStage.title}",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        "in ${String.format(Locale.getDefault(), "%.1f", hoursToNext)}h",
                                        color = Color(0xFF60A5FA),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Text(
                    text = "ALL 7 METABOLIC STAGES",
                    color = Color(0xFF71717A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable List of All Stages
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(FastingMetabolicStages.ALL_STAGES) { stage ->
                        val isExpanded = expandedStageId == stage.id
                        val isCurrent = currentStage?.id == stage.id
                        val isCompleted = hoursElapsed != null && (stage.hourEnd != null && hoursElapsed >= stage.hourEnd)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCurrent) Color(0xFF1C1C22) else Color(0xFF18181B),
                            border = BorderStroke(
                                if (isCurrent) 1.5.dp else 1.dp,
                                if (isCurrent) stage.accentColor else Color(0xFF27272A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedStageId = if (isExpanded) "" else stage.id
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Stage Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stage.icon, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = stage.hoursRangeLabel,
                                                    color = stage.accentColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                if (isCurrent) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = stage.accentColor.copy(alpha = 0.25f)
                                                    ) {
                                                        Text(
                                                            "ACTIVE",
                                                            color = stage.accentColor,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                } else if (isCompleted) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF064E3B)
                                                    ) {
                                                        Text(
                                                            "PASSED ✓",
                                                            color = Color(0xFF34D399),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = stage.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = if (isExpanded) stage.accentColor else Color(0xFF71717A)
                                    )
                                }

                                Text(
                                    text = stage.shortSummary,
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 6.dp)
                                )

                                // Expanded Deep Information
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                    ) {
                                        HorizontalDivider(color = Color(0xFF27272A))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Scientific Name & Fuel
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    "SCIENTIFIC CLASSIFICATION",
                                                    color = Color(0xFF71717A),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.8.sp
                                                )
                                                Text(
                                                    stage.scientificTitle,
                                                    color = stage.accentColor,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Primary Fuel Badge
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF27272A),
                                            border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("⚡", fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Primary Fuel: ${stage.primaryFuelSource}",
                                                    color = Color(0xFFE4E4E7),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Physiological Details
                                        Text(
                                            "HOW YOUR BODY RESPONDS:",
                                            color = Color(0xFF71717A),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stage.physiologicalProcess,
                                            color = Color(0xFFD4D4D8),
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Key Benefits List
                                        Text(
                                            "KEY BIOLOGICAL BENEFITS:",
                                            color = Color(0xFF71717A),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        stage.keyBenefits.forEach { benefit ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text("✓", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(benefit, color = Color(0xFFE4E4E7), fontSize = 12.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Tips & Guidance Box
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text("💡", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        "FASTING TIP",
                                                        color = Color(0xFF93C5FD),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.8.sp
                                                    )
                                                    Text(
                                                        stage.tips,
                                                        color = Color(0xFFBFDBFE),
                                                        fontSize = 11.sp,
                                                        lineHeight = 15.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Close Guide", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
