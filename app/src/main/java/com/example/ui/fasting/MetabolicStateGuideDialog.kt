package com.example.ui.fasting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

enum class MetabolicViewMode(val title: String, val icon: String) {
    STAGE_FOCUS("Stage Details", "🔬"),
    TIMELINE_JOURNEY("Full Timeline", "🗺️")
}

@Composable
fun MetabolicStateGuideDialog(
    hoursElapsed: Float?,
    onDismiss: () -> Unit
) {
    val allStages = FastingMetabolicStages.ALL_STAGES
    val currentActiveStage = hoursElapsed?.let { FastingMetabolicStages.getCurrentStage(it) }
    val nextActiveStage = hoursElapsed?.let { FastingMetabolicStages.getNextStage(it) }

    var selectedStageIndex by remember {
        val initialIdx = if (hoursElapsed != null && hoursElapsed > 0) {
            allStages.indexOfFirst { it.id == currentActiveStage?.id }.coerceAtLeast(0)
        } else {
            0
        }
        mutableIntStateOf(initialIdx)
    }

    var viewMode by remember { mutableStateOf(MetabolicViewMode.STAGE_FOCUS) }
    val selectedStage = allStages.getOrElse(selectedStageIndex) { allStages.first() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111114),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = selectedStage.accentColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, selectedStage.accentColor.copy(alpha = 0.6f)),
                            modifier = Modifier.size(38.dp)
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
                                fontSize = 17.sp
                            )
                            Text(
                                if (hoursElapsed != null && hoursElapsed > 0)
                                    "Fast Active: ${String.format(Locale.getDefault(), "%.1f", hoursElapsed)}h elapsed"
                                else
                                    "7 Biological & Cellular Phases",
                                color = if (hoursElapsed != null && hoursElapsed > 0) Color(0xFF34D399) else Color(0xFF71717A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA1A1AA))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Fast Mini Progression Ribbon (if fasting)
                if (hoursElapsed != null && currentActiveStage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF18181C),
                        border = BorderStroke(1.dp, currentActiveStage.accentColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(currentActiveStage.icon, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "NOW: ${currentActiveStage.title}",
                                        color = currentActiveStage.accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                if (nextActiveStage != null) {
                                    val hoursLeft = maxOf(0f, nextActiveStage.hourStart - hoursElapsed)
                                    Text(
                                        "Next in ${String.format(Locale.getDefault(), "%.1f", hoursLeft)}h",
                                        color = Color(0xFF60A5FA),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (nextActiveStage != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                val span = nextActiveStage.hourStart - currentActiveStage.hourStart
                                val progress = if (span > 0) ((hoursElapsed - currentActiveStage.hourStart) / span).coerceIn(0f, 1f) else 1f
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = currentActiveStage.accentColor,
                                    trackColor = Color(0xFF27272A)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // View Mode Switcher Tab Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF18181C), RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MetabolicViewMode.values().forEach { mode ->
                        val isSelected = viewMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewMode = mode },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF27272E) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, Color(0xFF3F3F46)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(mode.icon, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    mode.title,
                                    color = if (isSelected) Color.White else Color(0xFFA1A1AA),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stage Horizontal Navigation Chips (in Stage Details Mode)
                if (viewMode == MetabolicViewMode.STAGE_FOCUS) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(allStages) { index, stage ->
                            val isSelected = index == selectedStageIndex
                            val isCurrentActive = hoursElapsed != null && currentActiveStage?.id == stage.id
                            val isPassed = hoursElapsed != null && stage.hourEnd != null && hoursElapsed >= stage.hourEnd

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) stage.accentColor.copy(alpha = 0.2f) else Color(0xFF18181C),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) stage.accentColor else if (isCurrentActive) stage.accentColor.copy(alpha = 0.7f) else Color(0xFF27272A)
                                ),
                                modifier = Modifier.clickable { selectedStageIndex = index }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stage.icon, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                stage.hoursRangeLabel,
                                                color = if (isSelected) Color.White else Color(0xFFA1A1AA),
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (isCurrentActive) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(3.dp),
                                                    color = stage.accentColor
                                                ) {
                                                    Text(
                                                        "NOW",
                                                        color = Color.Black,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                                    )
                                                }
                                            } else if (isPassed) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("✓", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(
                                            stage.title.take(18) + if (stage.title.length > 18) "…" else "",
                                            color = if (isSelected) stage.accentColor else Color(0xFF71717A),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Main Content Body (fills available space completely with rich content)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (viewMode) {
                        MetabolicViewMode.STAGE_FOCUS -> {
                            StageFocusDetailView(
                                stage = selectedStage,
                                hoursElapsed = hoursElapsed,
                                isCurrentActive = hoursElapsed != null && currentActiveStage?.id == selectedStage.id,
                                isPassed = hoursElapsed != null && selectedStage.hourEnd != null && hoursElapsed >= selectedStage.hourEnd,
                                onPrevious = {
                                    if (selectedStageIndex > 0) selectedStageIndex--
                                },
                                onNext = {
                                    if (selectedStageIndex < allStages.size - 1) selectedStageIndex++
                                },
                                hasPrevious = selectedStageIndex > 0,
                                hasNext = selectedStageIndex < allStages.size - 1,
                                stageIndex = selectedStageIndex,
                                totalStages = allStages.size
                            )
                        }
                        MetabolicViewMode.TIMELINE_JOURNEY -> {
                            FullTimelineJourneyView(
                                stages = allStages,
                                hoursElapsed = hoursElapsed,
                                onSelectStage = { stage ->
                                    selectedStageIndex = allStages.indexOfFirst { it.id == stage.id }.coerceAtLeast(0)
                                    viewMode = MetabolicViewMode.STAGE_FOCUS
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StageFocusDetailView(
    stage: MetabolicState,
    hoursElapsed: Float?,
    isCurrentActive: Boolean,
    isPassed: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean,
    stageIndex: Int,
    totalStages: Int
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Stage Hero Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF18181D),
            border = BorderStroke(1.dp, stage.accentColor.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = stage.accentColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, stage.accentColor.copy(alpha = 0.4f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(stage.icon, fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stage.hoursRangeLabel,
                                    color = stage.accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Stage ${stageIndex + 1} of $totalStages",
                                    color = Color(0xFF71717A),
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                stage.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isCurrentActive -> stage.accentColor.copy(alpha = 0.25f)
                            isPassed -> Color(0xFF064E3B)
                            else -> Color(0xFF27272A)
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isCurrentActive -> stage.accentColor
                                isPassed -> Color(0xFF059669)
                                else -> Color(0xFF3F3F46)
                            }
                        )
                    ) {
                        Text(
                            text = when {
                                isCurrentActive -> "ACTIVE NOW"
                                isPassed -> "COMPLETED ✓"
                                else -> "UPCOMING"
                            },
                            color = when {
                                isCurrentActive -> stage.accentColor
                                isPassed -> Color(0xFF34D399)
                                else -> Color(0xFFA1A1AA)
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stage.scientificTitle,
                    color = stage.accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stage.shortSummary,
                    color = Color(0xFFD4D4D8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Primary Energy Fuel Source
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1A1A22),
            border = BorderStroke(1.dp, Color(0xFF2D2D38)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚡", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "PRIMARY ENERGY SOURCE",
                        color = Color(0xFF71717A),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        stage.primaryFuelSource,
                        color = Color(0xFFE4E4E7),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Biomarker Matrix Dashboard (2x2 Grid)
        Text(
            "HORMONAL & METABOLIC TELEMETRY",
            color = Color(0xFF71717A),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BiomarkerTile(
                    icon = "🩸",
                    label = "Blood Glucose",
                    value = stage.biomarkers.bloodSugar,
                    accentColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
                BiomarkerTile(
                    icon = "🧪",
                    label = "Insulin",
                    value = stage.biomarkers.insulin,
                    accentColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BiomarkerTile(
                    icon = "⚡",
                    label = "Ketones (BHB)",
                    value = stage.biomarkers.ketones,
                    accentColor = Color(0xFFFBBF24),
                    modifier = Modifier.weight(1f)
                )
                BiomarkerTile(
                    icon = "🧬",
                    label = "Autophagy",
                    value = stage.biomarkers.autophagy,
                    accentColor = Color(0xFFA78BFA),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                BiomarkerTile(
                    icon = "💪",
                    label = "Growth Hormone (HGH)",
                    value = stage.biomarkers.growthHormone,
                    accentColor = Color(0xFF34D399),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Cellular & Physiological Mechanisms
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF18181C),
            border = BorderStroke(1.dp, Color(0xFF27272A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔬", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "PHYSIOLOGICAL MECHANISM",
                        color = Color(0xFF71717A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stage.physiologicalProcess,
                    color = Color(0xFFD4D4D8),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        // Key Biological Benefits
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF18181C),
            border = BorderStroke(1.dp, Color(0xFF27272A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "KEY BIOLOGICAL BENEFITS",
                    color = Color(0xFF71717A),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
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
            }
        }

        // Pro Fasting Tip Box
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.5f),
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
                        "PRACTICAL GUIDANCE & TIP",
                        color = Color(0xFF93C5FD),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        stage.tips,
                        color = Color(0xFFBFDBFE),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Bottom Navigation Switcher Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = hasPrevious,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFA1A1AA),
                    disabledContentColor = Color(0xFF3F3F46)
                ),
                border = BorderStroke(1.dp, if (hasPrevious) Color(0xFF3F3F46) else Color(0xFF27272A))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Stage", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Previous", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onNext,
                enabled = hasNext,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6),
                    disabledContainerColor = Color(0xFF27272A)
                )
            ) {
                Text("Next Stage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Stage", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun BiomarkerTile(
    icon: String,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF18181C),
        border = BorderStroke(1.dp, Color(0xFF27272A))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    label,
                    color = Color(0xFF71717A),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun FullTimelineJourneyView(
    stages: List<MetabolicState>,
    hoursElapsed: Float?,
    onSelectStage: (MetabolicState) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "COMPLETE 7-STAGE FASTING PATHWAY",
                color = Color(0xFF71717A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        itemsIndexed(stages) { index, stage ->
            val isCurrentActive = hoursElapsed != null && FastingMetabolicStages.getCurrentStage(hoursElapsed).id == stage.id
            val isPassed = hoursElapsed != null && stage.hourEnd != null && hoursElapsed >= stage.hourEnd

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isCurrentActive) Color(0xFF1E1E28) else Color(0xFF18181C),
                border = BorderStroke(
                    if (isCurrentActive) 1.5.dp else 1.dp,
                    if (isCurrentActive) stage.accentColor else Color(0xFF27272A)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectStage(stage) }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = stage.accentColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(stage.icon, fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    stage.hoursRangeLabel,
                                    color = stage.accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stage.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                isCurrentActive -> stage.accentColor.copy(alpha = 0.2f)
                                isPassed -> Color(0xFF064E3B)
                                else -> Color(0xFF27272A)
                            }
                        ) {
                            Text(
                                text = when {
                                    isCurrentActive -> "ACTIVE NOW"
                                    isPassed -> "PASSED ✓"
                                    else -> "EXPLORE →"
                                },
                                color = when {
                                    isCurrentActive -> stage.accentColor
                                    isPassed -> Color(0xFF34D399)
                                    else -> Color(0xFF60A5FA)
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stage.shortSummary,
                        color = Color(0xFFA1A1AA),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fuel: ${stage.primaryFuelSource.take(30)}...",
                            color = Color(0xFF71717A),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Tap for deep dive",
                            color = Color(0xFF3B82F6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
