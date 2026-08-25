package com.example.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.UserPreferences
import com.example.data.repository.WeightUnit
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.UserAvatarView
import com.example.ui.fasting.FastingViewModel
import com.example.ui.food.FoodViewModel
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.AppTheme
import com.example.ui.weight.WeightViewModel
import com.example.util.WeightUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

enum class StatsTimeRange(val label: String, val days: Int) {
    DAYS_7("7 Days", 7),
    DAYS_30("30 Days", 30),
    DAYS_90("90 Days", 90),
    ALL_TIME("All Time", 3650)
}

@Composable
fun StatsScreen(
    userPrefs: UserPreferences?,
    fastingViewModel: FastingViewModel = viewModel(),
    weightViewModel: WeightViewModel = viewModel(),
    foodViewModel: FoodViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val fastSessions by fastingViewModel.pastSessions.collectAsState()
    val weightEntries by weightViewModel.weightEntries.collectAsState()
    val foodEntries by foodViewModel.foodEntries.collectAsState()

    var selectedRange by remember { mutableStateOf(StatsTimeRange.DAYS_30) }
    var showAvatarPicker by remember { mutableStateOf(false) }

    val username = userPrefs?.username ?: "User"
    val avatarId = userPrefs?.avatarId
    val weightUnit = userPrefs?.weightUnit ?: WeightUnit.KG
    val heightCm = userPrefs?.heightCm ?: 170f
    val gender = userPrefs?.gender ?: "Male"
    val waistCm = userPrefs?.waistCm

    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
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

    // Filter data by selected time range
    val cutoffTime = if (selectedRange == StatsTimeRange.ALL_TIME) 0L else {
        System.currentTimeMillis() - (selectedRange.days.toLong() * 86400000L)
    }

    val filteredFasts = fastSessions.filter { it.startTime >= cutoffTime }
    val filteredWeights = weightEntries.filter { it.date >= cutoffTime }
    val filteredFoods = foodEntries.filter { it.date >= cutoffTime }

    // Fasting stats calculation
    val totalFasts = filteredFasts.size
    val totalFastingHours = filteredFasts.sumOf { (it.endTime - it.startTime).toDouble() } / (1000 * 3600.0)
    val avgFastHours = if (totalFasts > 0) totalFastingHours / totalFasts else 0.0
    val longestFastHours = filteredFasts.maxOfOrNull { (it.endTime - it.startTime) / (1000 * 3600f) } ?: 0f
    val hitGoalFasts = filteredFasts.count { (it.endTime - it.startTime) >= it.durationTargetMillis }
    val successRate = if (totalFasts > 0) (hitGoalFasts * 100) / totalFasts else 0

    // Weight stats calculation
    val latestWeight = filteredWeights.firstOrNull()?.weightKg ?: (weightEntries.firstOrNull()?.weightKg ?: 70f)
    val oldestWeight = filteredWeights.lastOrNull()?.weightKg ?: latestWeight
    val weightChangeKg = latestWeight - oldestWeight
    val currentBmi = weightViewModel.calculateBmi(latestWeight, heightCm)

    // Calories stats calculation
    val dailyBudget = weightViewModel.calculateDailyCalories(latestWeight, heightCm, 30, gender, waistCm)
    val totalCalories = filteredFoods.sumOf { it.calories }
    val uniqueDaysWithFood = filteredFoods.map {
        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
        "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }.distinct().size
    val avgDailyCalories = if (uniqueDaysWithFood > 0) totalCalories / uniqueDaysWithFood else totalCalories

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

            Surface(
                shape = RoundedCornerShape(50),
                color = AppTheme.colors.surfaceElevated,
                border = BorderStroke(1.dp, AppTheme.colors.border)
            ) {
                Text(
                    text = "INSIGHTS",
                    color = AppTheme.colors.success,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Time Range Selector Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatsTimeRange.values().forEach { range ->
                val isSelected = selectedRange == range
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) AppTheme.colors.primary.copy(alpha = 0.2f) else AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) AppTheme.colors.primary else AppTheme.colors.border),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedRange = range }
                ) {
                    Text(
                        text = range.label,
                        color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. FASTING ANALYTICS SECTION
            item {
                SectionCard(title = "FASTING PERFORMANCE", icon = "⏱️") {
                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(label = "TOTAL FASTS", value = "$totalFasts", sub = "Sessions", modifier = Modifier.weight(1f))
                        StatTile(label = "LONGEST FAST", value = String.format(Locale.getDefault(), "%.1fh", longestFastHours), sub = "Personal Best", modifier = Modifier.weight(1f))
                        StatTile(label = "AVG DURATION", value = String.format(Locale.getDefault(), "%.1fh", avgFastHours), sub = "Per Fast", modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "SUCCESS RATE",
                            value = "$successRate%",
                            sub = "$hitGoalFasts of $totalFasts on target",
                            highlightColor = if (successRate >= 80) AppTheme.colors.success else AppTheme.colors.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "TOTAL FASTED",
                            value = String.format(Locale.getDefault(), "%.0fh", totalFastingHours),
                            sub = "Time in Ketosis",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("FASTING DURATION HISTORY (HOURS)", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Fasting Interactive Bar Chart
                    val fastingDataPoints = if (filteredFasts.isNotEmpty()) {
                        filteredFasts.take(10).reversed().map { (it.endTime - it.startTime) / (1000 * 3600f) }
                    } else {
                        listOf(16f, 16.5f, 18f, 16f, 20f, 16f, 17f)
                    }
                    FastingBarChart(
                        data = fastingDataPoints,
                        targetLine = 16f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            }

            // 2. WEIGHT & BODY METRICS SECTION
            item {
                SectionCard(title = "WEIGHT & BODY PROGRESS", icon = "⚖️") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "CURRENT WEIGHT",
                            value = WeightUtils.formatWeight(latestWeight, weightUnit),
                            sub = "Latest log",
                            modifier = Modifier.weight(1f)
                        )
                        val changeSign = if (weightChangeKg <= 0) "" else "+"
                        val changeColor = if (weightChangeKg <= 0) AppTheme.colors.success else AppTheme.colors.danger
                        StatTile(
                            label = "NET CHANGE",
                            value = "$changeSign${WeightUtils.formatWeight(kotlin.math.abs(weightChangeKg), weightUnit)}",
                            sub = "Over ${selectedRange.label}",
                            highlightColor = changeColor,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "CURRENT BMI",
                            value = String.format(Locale.getDefault(), "%.1f", currentBmi),
                            sub = if (currentBmi < 25) "Normal" else "Elevated",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("WEIGHT TREND TRAJECTORY", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val weightDataPoints = if (filteredWeights.isNotEmpty()) {
                        filteredWeights.take(10).reversed().map { it.weightKg }
                    } else {
                        listOf(73.5f, 73.0f, 72.4f, 72.0f, 71.5f, 71.2f, 70.8f)
                    }
                    WeightLineChart(
                        data = weightDataPoints,
                        unit = weightUnit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            }

            // 3. CALORIE & NUTRITION SECTION
            item {
                SectionCard(title = "NUTRITION & CALORIES", icon = "🥗") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "DAILY AVERAGE",
                            value = "$avgDailyCalories kcal",
                            sub = "Target: $dailyBudget kcal",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "LOGGED ITEMS",
                            value = "${filteredFoods.size}",
                            sub = "Food entries",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("DAILY CALORIE INTAKE VS BUDGET", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val calDataPoints = if (filteredFoods.isNotEmpty()) {
                        filteredFoods.take(10).reversed().map { it.calories.toFloat() }
                    } else {
                        listOf(2100f, 1950f, 2200f, 1800f, 2050f, 1900f, 1980f)
                    }
                    CalorieIntakeChart(
                        data = calDataPoints,
                        targetBudget = dailyBudget.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Meal Type Breakdown Pills
                    Text("MEAL TYPE DISTRIBUTION", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    val breakfastCal = filteredFoods.filter { it.mealType.equals("Breakfast", true) }.sumOf { it.calories }
                    val lunchCal = filteredFoods.filter { it.mealType.equals("Lunch", true) }.sumOf { it.calories }
                    val dinnerCal = filteredFoods.filter { it.mealType.equals("Dinner", true) }.sumOf { it.calories }
                    val snacksCal = filteredFoods.filter { it.mealType.equals("Snacks", true) || it.mealType.equals("Snack", true) }.sumOf { it.calories }
                    val drinksCal = filteredFoods.filter { it.mealType.equals("Drinks", true) || it.mealType.equals("Drink", true) }.sumOf { it.calories }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MealDistributionChip("🍳 Brk", breakfastCal, Color(0xFFFB923C), Modifier.weight(1f))
                        MealDistributionChip("🥗 Lun", lunchCal, AppTheme.colors.success, Modifier.weight(1f))
                        MealDistributionChip("🍲 Din", dinnerCal, AppTheme.colors.primary, Modifier.weight(1f))
                        MealDistributionChip("🥨 Snk", snacksCal, Color(0xFFA78BFA), Modifier.weight(1f))
                        MealDistributionChip("💧 Drk", drinksCal, Color(0xFF38BDF8), Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        border = BorderStroke(1.dp, AppTheme.colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = AppTheme.colors.textMuted
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    sub: String,
    highlightColor: Color = AppTheme.colors.textPrimary,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppTheme.colors.surfaceElevated,
        border = BorderStroke(1.dp, AppTheme.colors.border),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, color = AppTheme.colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = highlightColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            Text(sub, color = AppTheme.colors.textSecondary, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
fun MealDistributionChip(label: String, calories: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = if (AppTheme.colors.isDark) 0.15f else 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("${calories}k", color = AppTheme.colors.textPrimary, fontSize = 9.sp)
        }
    }
}

@Composable
fun FastingBarChart(
    data: List<Float>,
    targetLine: Float = 16f,
    modifier: Modifier = Modifier
) {
    val borderColor = AppTheme.colors.border
    val successCol = AppTheme.colors.success
    val primCol = AppTheme.colors.primary

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barCount = data.size
        if (barCount == 0) return@Canvas

        val maxVal = max(24f, (data.maxOrNull() ?: 16f) * 1.15f)
        val barWidth = (width / barCount) * 0.55f
        val gap = width / barCount

        // Draw target dashed guideline
        val targetY = height - (targetLine / maxVal) * height
        drawLine(
            color = borderColor,
            start = Offset(0f, targetY),
            end = Offset(width, targetY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        data.forEachIndexed { index, value ->
            val barHeight = (value / maxVal) * height
            val x = (index * gap) + (gap - barWidth) / 2
            val y = height - barHeight
            val reachedGoal = value >= targetLine

            val brush = Brush.verticalGradient(
                colors = if (reachedGoal) {
                    listOf(successCol, successCol.copy(alpha = 0.7f))
                } else {
                    listOf(primCol, primCol.copy(alpha = 0.7f))
                },
                startY = y,
                endY = height
            )

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
            )
        }
    }
}

@Composable
fun WeightLineChart(
    data: List<Float>,
    unit: WeightUnit,
    modifier: Modifier = Modifier
) {
    val primCol = AppTheme.colors.primary
    val surfaceCol = AppTheme.colors.surface

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (data.size < 2) return@Canvas

        val minVal = (data.minOrNull() ?: 60f) * 0.98f
        val maxVal = (data.maxOrNull() ?: 80f) * 1.02f
        val range = max(0.1f, maxVal - minVal)

        val points = data.mapIndexed { index, weight ->
            val x = (index.toFloat() / (data.size - 1)) * width
            val y = height - ((weight - minVal) / range) * height
            Offset(x, y)
        }

        // Draw smooth gradient area underneath
        val fillPath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primCol.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw line
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }

        drawPath(
            path = linePath,
            color = primCol,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw points
        points.forEach { pt ->
            drawCircle(
                color = surfaceCol,
                radius = 5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = primCol,
                radius = 3.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
fun CalorieIntakeChart(
    data: List<Float>,
    targetBudget: Float = 2150f,
    modifier: Modifier = Modifier
) {
    val successCol = AppTheme.colors.success
    val dangerCol = AppTheme.colors.danger

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barCount = data.size
        if (barCount == 0) return@Canvas

        val maxVal = max(targetBudget * 1.3f, (data.maxOrNull() ?: 2000f) * 1.1f)
        val barWidth = (width / barCount) * 0.5f
        val gap = width / barCount

        // Budget line
        val budgetY = height - (targetBudget / maxVal) * height
        drawLine(
            color = successCol.copy(alpha = 0.6f),
            start = Offset(0f, budgetY),
            end = Offset(width, budgetY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        )

        data.forEachIndexed { index, calories ->
            val barHeight = (calories / maxVal) * height
            val x = (index * gap) + (gap - barWidth) / 2
            val y = height - barHeight
            val overBudget = calories > targetBudget

            val brush = Brush.verticalGradient(
                colors = if (overBudget) {
                    listOf(dangerCol, dangerCol.copy(alpha = 0.7f))
                } else {
                    listOf(successCol, successCol.copy(alpha = 0.7f))
                },
                startY = y,
                endY = height
            )

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
            )
        }
    }
}
