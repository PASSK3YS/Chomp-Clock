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
import com.example.ui.theme.AppTheme
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
            color = AppTheme.colors.surface,
            border = BorderStroke(1.dp, AppTheme.colors.border)
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
                                color = AppTheme.colors.textPrimary,
                                fontSize = 17.sp
                            )
                            Text(
                                if (hoursElapsed != null && hoursElapsed > 0)
                                    "Fast Active: ${String.format(Locale.getDefault(), "%.1f", hoursElapsed)}h elapsed"
                                else
                                    "7 Biological & Cellular Phases",
                                color = if (hoursElapsed != null && hoursElapsed > 0) AppTheme.colors.success else AppTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppTheme.colors.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Fast Mini Progression Ribbon (if fasting)
                if (hoursElapsed != null && currentActiveStage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppTheme.colors.surfaceElevated,
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
                                        color = AppTheme.colors.primary,
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
                                    trackColor = AppTheme.colors.surfaceElevated
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
                        .background(AppTheme.colors.surfaceElevated, RoundedCornerShape(12.dp))
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
                            color = if (isSelected) AppTheme.colors.surface else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, AppTheme.colors.border) else null
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
                                    color = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.textMuted,
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
                                color = if (isSelected) stage.accentColor.copy(alpha = 0.18f) else AppTheme.colors.surfaceElevated,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) stage.accentColor else if (isCurrentActive) stage.accentColor.copy(alpha = 0.7f) else AppTheme.colors.border
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
                                                color = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.textMuted,
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
                                                Text("✓", color = AppTheme.colors.success, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(
                                            stage.title.take(18) + if (stage.title.length > 18) "…" else "",
                                            color = if (isSelected) stage.accentColor else AppTheme.colors.textSecondary,
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
            color = AppTheme.colors.surfaceElevated,
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
                                    color = AppTheme.colors.textMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                stage.title,
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isCurrentActive -> stage.accentColor.copy(alpha = 0.25f)
                            isPassed -> AppTheme.colors.success.copy(alpha = 0.2f)
                            else -> AppTheme.colors.surface
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isCurrentActive -> stage.accentColor
                                isPassed -> AppTheme.colors.success
                                else -> AppTheme.colors.border
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
                                isPassed -> AppTheme.colors.success
                                else -> AppTheme.colors.textMuted
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
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Primary Energy Fuel Source
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surfaceElevated,
            border = BorderStroke(1.dp, AppTheme.colors.border),
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
                        color = AppTheme.colors.textMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        stage.primaryFuelSource,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Biomarker Matrix Dashboard (2x2 Grid)
        Text(
            "HORMONAL & METABOLIC TELEMETRY",
            color = AppTheme.colors.textMuted,
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
                    accentColor = AppTheme.colors.success,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Cellular & Physiological Mechanisms
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AppTheme.colors.surfaceElevated,
            border = BorderStroke(1.dp, AppTheme.colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔬", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "PHYSIOLOGICAL MECHANISM",
                        color = AppTheme.colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stage.physiologicalProcess,
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        // Key Biological Benefits
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AppTheme.colors.surfaceElevated,
            border = BorderStroke(1.dp, AppTheme.colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "KEY BIOLOGICAL BENEFITS",
                    color = AppTheme.colors.textMuted,
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
                        Text("✓", color = AppTheme.colors.success, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(benefit, color = AppTheme.colors.textPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Pro Fasting Tip Box
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.primary.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.35f)),
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
                        color = AppTheme.colors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        stage.tips,
                        color = AppTheme.colors.textPrimary,
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
                    contentColor = AppTheme.colors.textPrimary,
                    disabledContentColor = AppTheme.colors.textMuted
                ),
                border = BorderStroke(1.dp, if (hasPrevious) AppTheme.colors.border else AppTheme.colors.border.copy(alpha = 0.4f))
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
                    containerColor = AppTheme.colors.primary,
                    disabledContainerColor = AppTheme.colors.surfaceElevated
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
        color = AppTheme.colors.surfaceElevated,
        border = BorderStroke(1.dp, AppTheme.colors.border)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    label,
                    color = AppTheme.colors.textMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = AppTheme.colors.textPrimary,
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
                color = AppTheme.colors.textMuted,
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
                color = if (isCurrentActive) stage.accentColor.copy(alpha = 0.15f) else AppTheme.colors.surfaceElevated,
                border = BorderStroke(
                    if (isCurrentActive) 1.5.dp else 1.dp,
                    if (isCurrentActive) stage.accentColor else AppTheme.colors.border
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
                                    color = AppTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                isCurrentActive -> stage.accentColor.copy(alpha = 0.2f)
                                isPassed -> AppTheme.colors.success.copy(alpha = 0.2f)
                                else -> AppTheme.colors.surface
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
                                    isPassed -> AppTheme.colors.success
                                    else -> AppTheme.colors.primary
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
                        color = AppTheme.colors.textSecondary,
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
                            color = AppTheme.colors.textMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Tap for deep dive",
                            color = AppTheme.colors.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
