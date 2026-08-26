package com.example.ui.stats

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.FastSession
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.WeightEntry
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class StatsTimeRange(val label: String, val days: Int) {
    DAYS_7("7 Days", 7),
    DAYS_30("30 Days", 30),
    DAYS_90("90 Days", 90),
    ALL_TIME("All Time", 3650),
    CUSTOM("Custom 📅", -1)
}

data class FastingChartPoint(
    val session: FastSession,
    val durationHours: Float,
    val targetHours: Float,
    val dateLabel: String,
    val formattedDate: String,
    val isGoalAchieved: Boolean
)

data class WeightChartPoint(
    val entry: WeightEntry,
    val weightValue: Float,
    val dateLabel: String,
    val formattedDate: String,
    val diffFromPrevious: Float?
)

data class CalorieChartPoint(
    val dateMillis: Long,
    val dateLabel: String,
    val formattedDate: String,
    val totalCalories: Int,
    val targetBudget: Int,
    val breakfastCal: Int,
    val lunchCal: Int,
    val dinnerCal: Int,
    val snacksCal: Int,
    val drinksCal: Int
)

@Composable
fun StatsScreen(
    userPrefs: UserPreferences?,
    fastingViewModel: FastingViewModel = viewModel(),
    weightViewModel: WeightViewModel = viewModel(),
    foodViewModel: FoodViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val fastSessions by fastingViewModel.pastSessions.collectAsState()
    val weightEntries by weightViewModel.weightEntries.collectAsState()
    val foodEntries by foodViewModel.foodEntries.collectAsState()

    var selectedRange by remember { mutableStateOf(StatsTimeRange.DAYS_30) }
    var customStartDate by remember {
        mutableStateOf(System.currentTimeMillis() - (30L * 86400000L))
    }
    var customEndDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

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

    if (showCustomDateDialog) {
        CustomDateRangeDialog(
            initialStart = customStartDate,
            initialEnd = customEndDate,
            onDismiss = { showCustomDateDialog = false },
            onApply = { start, end ->
                customStartDate = start
                customEndDate = end
                selectedRange = StatsTimeRange.CUSTOM
                showCustomDateDialog = false
            }
        )
    }

    // Determine cutoff timestamps based on selection
    val (startTimeRange, endTimeRange) = remember(selectedRange, customStartDate, customEndDate) {
        val now = System.currentTimeMillis()
        when (selectedRange) {
            StatsTimeRange.DAYS_7 -> Pair(now - (7L * 86400000L), now)
            StatsTimeRange.DAYS_30 -> Pair(now - (30L * 86400000L), now)
            StatsTimeRange.DAYS_90 -> Pair(now - (90L * 86400000L), now)
            StatsTimeRange.ALL_TIME -> Pair(0L, now)
            StatsTimeRange.CUSTOM -> Pair(customStartDate, customEndDate)
        }
    }

    val filteredFasts = remember(fastSessions, startTimeRange, endTimeRange) {
        fastSessions.filter { it.startTime in startTimeRange..endTimeRange || (it.endTime in startTimeRange..endTimeRange) }
            .sortedBy { it.startTime }
    }

    val filteredWeights = remember(weightEntries, startTimeRange, endTimeRange) {
        weightEntries.filter { it.date in startTimeRange..endTimeRange }
            .sortedBy { it.date }
    }

    val filteredFoods = remember(foodEntries, startTimeRange, endTimeRange) {
        foodEntries.filter { it.date in startTimeRange..endTimeRange }
            .sortedBy { it.date }
    }

    // Fasting stats calculation
    val totalFasts = filteredFasts.size
    val totalFastingHours = filteredFasts.sumOf { (it.endTime - it.startTime).toDouble() } / (1000 * 3600.0)
    val avgFastHours = if (totalFasts > 0) totalFastingHours / totalFasts else 0.0
    val longestFastHours = filteredFasts.maxOfOrNull { (it.endTime - it.startTime) / (1000 * 3600f) } ?: 0f
    val hitGoalFasts = filteredFasts.count { (it.endTime - it.startTime) >= it.durationTargetMillis }
    val successRate = if (totalFasts > 0) (hitGoalFasts * 100) / totalFasts else 0

    // Weight stats calculation
    val latestWeight = filteredWeights.lastOrNull()?.weightKg ?: (weightEntries.firstOrNull()?.weightKg ?: 70f)
    val oldestWeight = filteredWeights.firstOrNull()?.weightKg ?: latestWeight
    val weightChangeKg = latestWeight - oldestWeight
    val currentBmi = weightViewModel.calculateBmi(latestWeight, heightCm)

    // Calories stats calculation
    val dailyBudget = if (userPrefs?.useCustomCalories == true && userPrefs.customDailyCalories > 0) {
        userPrefs.customDailyCalories
    } else {
        weightViewModel.calculateDailyCalories(latestWeight, heightCm, 30, gender, waistCm)
    }

    val totalCalories = filteredFoods.sumOf { it.calories }
    val uniqueDaysWithFood = filteredFoods.map {
        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
        "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }.distinct().size
    val avgDailyCalories = if (uniqueDaysWithFood > 0) totalCalories / uniqueDaysWithFood else totalCalories

    // Prepare chart data points
    val fastingChartPoints = remember(filteredFasts) {
        val df = SimpleDateFormat("d MMM", Locale.getDefault())
        val fullDf = SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.getDefault())
        filteredFasts.map { session ->
            val durationHours = (session.endTime - session.startTime) / (1000 * 3600f)
            val targetHours = session.durationTargetMillis / (1000 * 3600f)
            FastingChartPoint(
                session = session,
                durationHours = durationHours,
                targetHours = targetHours,
                dateLabel = df.format(Date(session.startTime)),
                formattedDate = fullDf.format(Date(session.startTime)),
                isGoalAchieved = durationHours >= targetHours
            )
        }
    }

    val weightChartPoints = remember(filteredWeights, weightUnit) {
        val df = SimpleDateFormat("d MMM", Locale.getDefault())
        val fullDf = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
        var prevWeight: Float? = null
        filteredWeights.map { entry ->
            val pt = WeightChartPoint(
                entry = entry,
                weightValue = entry.weightKg,
                dateLabel = df.format(Date(entry.date)),
                formattedDate = fullDf.format(Date(entry.date)),
                diffFromPrevious = prevWeight?.let { entry.weightKg - it }
            )
            prevWeight = entry.weightKg
            pt
        }
    }

    val calorieChartPoints = remember(filteredFoods, dailyBudget) {
        val df = SimpleDateFormat("d MMM", Locale.getDefault())
        val fullDf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val groupedByDay = filteredFoods.groupBy { entry ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = entry.date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }

        groupedByDay.map { (dayMillis, items) ->
            val breakfast = items.filter { it.mealType.equals("Breakfast", true) }.sumOf { it.calories }
            val lunch = items.filter { it.mealType.equals("Lunch", true) }.sumOf { it.calories }
            val dinner = items.filter { it.mealType.equals("Dinner", true) }.sumOf { it.calories }
            val snacks = items.filter { it.mealType.equals("Snacks", true) || it.mealType.equals("Snack", true) }.sumOf { it.calories }
            val drinks = items.filter { it.mealType.equals("Drinks", true) || it.mealType.equals("Drink", true) }.sumOf { it.calories }
            val total = items.sumOf { it.calories }

            CalorieChartPoint(
                dateMillis = dayMillis,
                dateLabel = df.format(Date(dayMillis)),
                formattedDate = fullDf.format(Date(dayMillis)),
                totalCalories = total,
                targetBudget = dailyBudget,
                breakfastCal = breakfast,
                lunchCal = lunch,
                dinnerCal = dinner,
                snacksCal = snacks,
                drinksCal = drinks
            )
        }.sortedBy { it.dateMillis }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoGraph,
                        contentDescription = null,
                        tint = AppTheme.colors.success,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "INTERACTIVE STATS",
                        color = AppTheme.colors.success,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Time Range Filter Tabs with Custom Range
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            StatsTimeRange.values().forEach { range ->
                val isSelected = selectedRange == range
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) AppTheme.colors.primary.copy(alpha = 0.22f) else AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) AppTheme.colors.primary else AppTheme.colors.border),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (range == StatsTimeRange.CUSTOM) {
                                showCustomDateDialog = true
                            } else {
                                selectedRange = range
                            }
                        }
                ) {
                    Text(
                        text = range.label,
                        color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }

        // Active Date Range Pill Banner (shows dates when Custom is selected)
        if (selectedRange == StatsTimeRange.CUSTOM) {
            Spacer(modifier = Modifier.height(8.dp))
            val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AppTheme.colors.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomDateDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = AppTheme.colors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${dateFmt.format(Date(customStartDate))} — ${dateFmt.format(Date(customEndDate))}",
                            color = AppTheme.colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "Edit ✏️",
                        color = AppTheme.colors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. FASTING ANALYTICS SECTION
            item {
                SectionCard(
                    title = "FASTING PERFORMANCE",
                    icon = "⏱️",
                    badge = "${filteredFasts.size} logged"
                ) {
                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "TOTAL FASTS",
                            value = "$totalFasts",
                            sub = "Sessions",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "LONGEST FAST",
                            value = String.format(Locale.getDefault(), "%.1fh", longestFastHours),
                            sub = "Personal Best",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "AVG DURATION",
                            value = String.format(Locale.getDefault(), "%.1fh", avgFastHours),
                            sub = "Per Fast",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "SUCCESS RATE",
                            value = "$successRate%",
                            sub = "$hitGoalFasts of $totalFasts hit goal",
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "FASTING DURATION (TOUCH BARS TO INSPECT)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            "Target: 16h",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Interactive Fasting Bar Chart
                    InteractiveFastingBarChart(
                        points = fastingChartPoints,
                        targetLine = 16f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            // 2. WEIGHT & BODY METRICS SECTION
            item {
                SectionCard(
                    title = "WEIGHT & BODY PROGRESS",
                    icon = "⚖️",
                    badge = "${filteredWeights.size} entries"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "CURRENT WEIGHT",
                            value = WeightUtils.formatWeight(latestWeight, weightUnit),
                            sub = "Latest record",
                            modifier = Modifier.weight(1f)
                        )
                        val changeSign = if (weightChangeKg <= 0) "" else "+"
                        val changeColor = if (weightChangeKg <= 0) AppTheme.colors.success else AppTheme.colors.danger
                        StatTile(
                            label = "NET CHANGE",
                            value = "$changeSign${WeightUtils.formatWeight(abs(weightChangeKg), weightUnit)}",
                            sub = "Over ${selectedRange.label}",
                            highlightColor = changeColor,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "CURRENT BMI",
                            value = String.format(Locale.getDefault(), "%.1f", currentBmi),
                            sub = if (currentBmi < 25) "Normal weight" else "Elevated",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "WEIGHT TRAJECTORY (TOUCH / DRAG TO INSPECT)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    InteractiveWeightLineChart(
                        points = weightChartPoints,
                        unit = weightUnit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            // 3. CALORIE & NUTRITION SECTION
            item {
                SectionCard(
                    title = "NUTRITION & CALORIES",
                    icon = "🥗",
                    badge = "$totalCalories kcal total"
                ) {
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
                            label = "TOTAL LOGGED",
                            value = "${filteredFoods.size}",
                            sub = "Items across range",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "DAILY INTAKE VS BUDGET (TOUCH FOR BREAKDOWN)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    InteractiveCalorieChart(
                        points = calorieChartPoints,
                        targetBudget = dailyBudget,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Meal Type Breakdown Pills
                    Text(
                        "MEAL TYPE DISTRIBUTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val breakfastCal = filteredFoods.filter { it.mealType.equals("Breakfast", true) }.sumOf { it.calories }
                    val lunchCal = filteredFoods.filter { it.mealType.equals("Lunch", true) }.sumOf { it.calories }
                    val dinnerCal = filteredFoods.filter { it.mealType.equals("Dinner", true) }.sumOf { it.calories }
                    val snacksCal = filteredFoods.filter { it.mealType.equals("Snacks", true) || it.mealType.equals("Snack", true) }.sumOf { it.calories }
                    val drinksCal = filteredFoods.filter { it.mealType.equals("Drinks", true) || it.mealType.equals("Drink", true) }.sumOf { it.calories }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MealDistributionChip("🍳 Brk", breakfastCal, totalCalories, Color(0xFFFB923C), Modifier.weight(1f))
                        MealDistributionChip("🥗 Lun", lunchCal, totalCalories, AppTheme.colors.success, Modifier.weight(1f))
                        MealDistributionChip("🍲 Din", dinnerCal, totalCalories, AppTheme.colors.primary, Modifier.weight(1f))
                        MealDistributionChip("🥨 Snk", snacksCal, totalCalories, Color(0xFFA78BFA), Modifier.weight(1f))
                        MealDistributionChip("💧 Drk", drinksCal, totalCalories, Color(0xFF38BDF8), Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: String,
    badge: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        border = BorderStroke(1.dp, AppTheme.colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 17.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = AppTheme.colors.textMuted
                    )
                }
                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AppTheme.colors.surfaceElevated,
                        border = BorderStroke(1.dp, AppTheme.colors.border)
                    ) {
                        Text(
                            text = badge,
                            color = AppTheme.colors.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
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
fun MealDistributionChip(label: String, calories: Int, totalCalories: Int, color: Color, modifier: Modifier = Modifier) {
    val pct = if (totalCalories > 0) (calories * 100) / totalCalories else 0
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
            Text("${calories}k", color = AppTheme.colors.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text("$pct%", color = AppTheme.colors.textMuted, fontSize = 9.sp)
        }
    }
}

// -------------------------------------------------------------
// 1. INTERACTIVE FASTING BAR CHART WITH TOUCH TOOLTIP
// -------------------------------------------------------------
@Composable
fun InteractiveFastingBarChart(
    points: List<FastingChartPoint>,
    targetLine: Float = 16f,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val borderColor = AppTheme.colors.border
    val successCol = AppTheme.colors.success
    val primCol = AppTheme.colors.primary

    // Sample fallback if empty
    val displayPoints = if (points.isNotEmpty()) points else {
        listOf(
            FastingChartPoint(FastSession(0, 0, 16 * 3600000L, 16 * 3600000L), 16.0f, 16f, "1 Aug", "1 Aug 2026", true),
            FastingChartPoint(FastSession(0, 0, 17 * 3600000L, 16 * 3600000L), 17.5f, 16f, "2 Aug", "2 Aug 2026", true),
            FastingChartPoint(FastSession(0, 0, 15 * 3600000L, 16 * 3600000L), 14.8f, 16f, "3 Aug", "3 Aug 2026", false),
            FastingChartPoint(FastSession(0, 0, 18 * 3600000L, 16 * 3600000L), 18.2f, 16f, "4 Aug", "4 Aug 2026", true),
            FastingChartPoint(FastSession(0, 0, 16 * 3600000L, 16 * 3600000L), 16.1f, 16f, "5 Aug", "5 Aug 2026", true)
        )
    }

    Column {
        // Selected Item Interactive Banner / Tooltip
        if (selectedIndex != null && selectedIndex!! in displayPoints.indices) {
            val pt = displayPoints[selectedIndex!!]
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AppTheme.colors.surfaceElevated,
                border = BorderStroke(1.dp, if (pt.isGoalAchieved) successCol else primCol),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = pt.formattedDate,
                            color = AppTheme.colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f hrs", pt.durationHours),
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val diff = pt.durationHours - pt.targetHours
                            val diffText = if (diff >= 0) String.format(Locale.getDefault(), "+%.1fh vs goal", diff) else String.format(Locale.getDefault(), "%.1fh vs goal", diff)
                            Text(
                                text = diffText,
                                color = if (pt.isGoalAchieved) successCol else AppTheme.colors.danger,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = (if (pt.isGoalAchieved) successCol else primCol).copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = if (pt.isGoalAchieved) "GOAL MET ✅" else "INCOMPLETE",
                            color = if (pt.isGoalAchieved) successCol else primCol,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Canvas(
            modifier = modifier
                .pointerInput(displayPoints) {
                    detectTapGestures { offset ->
                        val barCount = displayPoints.size
                        val gap = size.width / barCount
                        val idx = (offset.x / gap).toInt().coerceIn(0, barCount - 1)
                        selectedIndex = if (selectedIndex == idx) null else idx
                    }
                }
                .pointerInput(displayPoints) {
                    detectDragGestures { change, _ ->
                        val barCount = displayPoints.size
                        val gap = size.width / barCount
                        val idx = (change.position.x / gap).toInt().coerceIn(0, barCount - 1)
                        selectedIndex = idx
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val barCount = displayPoints.size
            if (barCount == 0) return@Canvas

            val maxVal = max(24f, (displayPoints.maxOfOrNull { it.durationHours } ?: 16f) * 1.15f)
            val barWidth = (width / barCount) * 0.52f
            val gap = width / barCount

            // Draw target guideline
            val targetY = height - (targetLine / maxVal) * height
            drawLine(
                color = borderColor,
                start = Offset(0f, targetY),
                end = Offset(width, targetY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            displayPoints.forEachIndexed { index, pt ->
                val barHeight = (pt.durationHours / maxVal) * height
                val x = (index * gap) + (gap - barWidth) / 2
                val y = height - barHeight
                val isSelected = selectedIndex == index

                val brush = Brush.verticalGradient(
                    colors = if (pt.isGoalAchieved) {
                        if (isSelected) listOf(successCol, successCol.copy(alpha = 0.9f))
                        else listOf(successCol.copy(alpha = 0.85f), successCol.copy(alpha = 0.45f))
                    } else {
                        if (isSelected) listOf(primCol, primCol.copy(alpha = 0.9f))
                        else listOf(primCol.copy(alpha = 0.85f), primCol.copy(alpha = 0.45f))
                    },
                    startY = y,
                    endY = height
                )

                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )

                if (isSelected) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(x - 2f, y - 2f),
                        size = Size(barWidth + 4f, barHeight + 4f),
                        style = Stroke(width = 2.dp.toPx()),
                        cornerRadius = CornerRadius(7.dp.toPx())
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. INTERACTIVE WEIGHT LINE CHART WITH TOUCH SCRUBBING
// -------------------------------------------------------------
@Composable
fun InteractiveWeightLineChart(
    points: List<WeightChartPoint>,
    unit: WeightUnit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val primCol = AppTheme.colors.primary
    val surfaceCol = AppTheme.colors.surface
    val borderCol = AppTheme.colors.border

    val displayPoints = if (points.size >= 2) points else {
        listOf(
            WeightChartPoint(WeightEntry(0, 73.5f, System.currentTimeMillis() - 6 * 86400000L), 73.5f, "1 Aug", "1 Aug 2026", null),
            WeightChartPoint(WeightEntry(0, 73.0f, System.currentTimeMillis() - 5 * 86400000L), 73.0f, "2 Aug", "2 Aug 2026", -0.5f),
            WeightChartPoint(WeightEntry(0, 72.4f, System.currentTimeMillis() - 4 * 86400000L), 72.4f, "3 Aug", "3 Aug 2026", -0.6f),
            WeightChartPoint(WeightEntry(0, 72.0f, System.currentTimeMillis() - 3 * 86400000L), 72.0f, "4 Aug", "4 Aug 2026", -0.4f),
            WeightChartPoint(WeightEntry(0, 71.5f, System.currentTimeMillis() - 2 * 86400000L), 71.5f, "5 Aug", "5 Aug 2026", -0.5f),
            WeightChartPoint(WeightEntry(0, 71.1f, System.currentTimeMillis() - 1 * 86400000L), 71.1f, "6 Aug", "6 Aug 2026", -0.4f),
            WeightChartPoint(WeightEntry(0, 70.8f, System.currentTimeMillis()), 70.8f, "7 Aug", "7 Aug 2026", -0.3f)
        )
    }

    Column {
        if (selectedIndex != null && selectedIndex!! in displayPoints.indices) {
            val pt = displayPoints[selectedIndex!!]
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AppTheme.colors.surfaceElevated,
                border = BorderStroke(1.dp, primCol),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = pt.formattedDate,
                            color = AppTheme.colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = WeightUtils.formatWeight(pt.weightValue, unit),
                            color = AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    if (pt.diffFromPrevious != null) {
                        val diffSign = if (pt.diffFromPrevious <= 0) "" else "+"
                        val diffColor = if (pt.diffFromPrevious <= 0) AppTheme.colors.success else AppTheme.colors.danger
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = diffColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$diffSign${WeightUtils.formatWeight(abs(pt.diffFromPrevious), unit)} vs prev",
                                color = diffColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        Canvas(
            modifier = modifier
                .pointerInput(displayPoints) {
                    detectTapGestures { offset ->
                        val count = displayPoints.size
                        val gap = size.width / (count - 1).coerceAtLeast(1)
                        val idx = ((offset.x + gap / 2) / gap).toInt().coerceIn(0, count - 1)
                        selectedIndex = if (selectedIndex == idx) null else idx
                    }
                }
                .pointerInput(displayPoints) {
                    detectDragGestures { change, _ ->
                        val count = displayPoints.size
                        val gap = size.width / (count - 1).coerceAtLeast(1)
                        val idx = ((change.position.x + gap / 2) / gap).toInt().coerceIn(0, count - 1)
                        selectedIndex = idx
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            if (displayPoints.size < 2) return@Canvas

            val minVal = (displayPoints.minOfOrNull { it.weightValue } ?: 60f) * 0.98f
            val maxVal = (displayPoints.maxOfOrNull { it.weightValue } ?: 80f) * 1.02f
            val range = max(0.1f, maxVal - minVal)

            val coords = displayPoints.mapIndexed { index, pt ->
                val x = (index.toFloat() / (displayPoints.size - 1)) * width
                val y = height - ((pt.weightValue - minVal) / range) * height
                Offset(x, y)
            }

            // Draw Area Fill
            val fillPath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 0 until coords.size - 1) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
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

            // Draw Line
            val linePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 0 until coords.size - 1) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val cx = (p0.x + p1.x) / 2
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            drawPath(
                path = linePath,
                color = primCol,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Points & Halo
            coords.forEachIndexed { index, pt ->
                val isSelected = selectedIndex == index
                if (isSelected) {
                    // Vertical guide line
                    drawLine(
                        color = primCol.copy(alpha = 0.6f),
                        start = Offset(pt.x, 0f),
                        end = Offset(pt.x, height),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                    // Glow halo
                    drawCircle(
                        color = primCol.copy(alpha = 0.3f),
                        radius = 10.dp.toPx(),
                        center = pt
                    )
                }

                drawCircle(
                    color = surfaceCol,
                    radius = (if (isSelected) 6.dp else 4.5.dp).toPx(),
                    center = pt
                )
                drawCircle(
                    color = primCol,
                    radius = (if (isSelected) 4.5.dp else 3.dp).toPx(),
                    center = pt
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 3. INTERACTIVE CALORIE INTAKE CHART WITH TOUCH BREAKDOWN
// -------------------------------------------------------------
@Composable
fun InteractiveCalorieChart(
    points: List<CalorieChartPoint>,
    targetBudget: Int = 2000,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val successCol = AppTheme.colors.success
    val dangerCol = AppTheme.colors.danger
    val primCol = AppTheme.colors.primary

    val displayPoints = if (points.isNotEmpty()) points else {
        listOf(
            CalorieChartPoint(System.currentTimeMillis() - 4 * 86400000L, "1 Aug", "1 Aug 2026", 1950, targetBudget, 450, 650, 700, 150, 0),
            CalorieChartPoint(System.currentTimeMillis() - 3 * 86400000L, "2 Aug", "2 Aug 2026", 2150, targetBudget, 500, 750, 750, 150, 0),
            CalorieChartPoint(System.currentTimeMillis() - 2 * 86400000L, "3 Aug", "3 Aug 2026", 1820, targetBudget, 400, 620, 650, 150, 0),
            CalorieChartPoint(System.currentTimeMillis() - 1 * 86400000L, "4 Aug", "4 Aug 2026", 2050, targetBudget, 480, 700, 720, 150, 0),
            CalorieChartPoint(System.currentTimeMillis(), "5 Aug", "5 Aug 2026", 1920, targetBudget, 420, 650, 700, 150, 0)
        )
    }

    Column {
        if (selectedIndex != null && selectedIndex!! in displayPoints.indices) {
            val pt = displayPoints[selectedIndex!!]
            val isOver = pt.totalCalories > pt.targetBudget
            val diff = pt.totalCalories - pt.targetBudget
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AppTheme.colors.surfaceElevated,
                border = BorderStroke(1.dp, if (isOver) dangerCol else successCol),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pt.formattedDate,
                            color = AppTheme.colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = (if (isOver) dangerCol else successCol).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isOver) "+${diff} kcal OVER" else "${abs(diff)} kcal REMAINING",
                                color = if (isOver) dangerCol else successCol,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${pt.totalCalories} / ${pt.targetBudget} kcal",
                        color = AppTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Brk: ${pt.breakfastCal}k • Lun: ${pt.lunchCal}k • Din: ${pt.dinnerCal}k • Snk: ${pt.snacksCal}k • Drk: ${pt.drinksCal}k",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Canvas(
            modifier = modifier
                .pointerInput(displayPoints) {
                    detectTapGestures { offset ->
                        val barCount = displayPoints.size
                        val gap = size.width / barCount
                        val idx = (offset.x / gap).toInt().coerceIn(0, barCount - 1)
                        selectedIndex = if (selectedIndex == idx) null else idx
                    }
                }
                .pointerInput(displayPoints) {
                    detectDragGestures { change, _ ->
                        val barCount = displayPoints.size
                        val gap = size.width / barCount
                        val idx = (change.position.x / gap).toInt().coerceIn(0, barCount - 1)
                        selectedIndex = idx
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val barCount = displayPoints.size
            if (barCount == 0) return@Canvas

            val maxVal = max(targetBudget * 1.3f, (displayPoints.maxOfOrNull { it.totalCalories.toFloat() } ?: 2000f) * 1.1f)
            val barWidth = (width / barCount) * 0.5f
            val gap = width / barCount

            // Budget guideline
            val budgetY = height - (targetBudget.toFloat() / maxVal) * height
            drawLine(
                color = successCol.copy(alpha = 0.6f),
                start = Offset(0f, budgetY),
                end = Offset(width, budgetY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            displayPoints.forEachIndexed { index, pt ->
                val barHeight = (pt.totalCalories.toFloat() / maxVal) * height
                val x = (index * gap) + (gap - barWidth) / 2
                val y = height - barHeight
                val overBudget = pt.totalCalories > pt.targetBudget
                val isSelected = selectedIndex == index

                val brush = Brush.verticalGradient(
                    colors = if (overBudget) {
                        if (isSelected) listOf(dangerCol, dangerCol.copy(alpha = 0.9f))
                        else listOf(dangerCol.copy(alpha = 0.85f), dangerCol.copy(alpha = 0.45f))
                    } else {
                        if (isSelected) listOf(successCol, successCol.copy(alpha = 0.9f))
                        else listOf(successCol.copy(alpha = 0.85f), successCol.copy(alpha = 0.45f))
                    },
                    startY = y,
                    endY = height
                )

                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )

                if (isSelected) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(x - 2f, y - 2f),
                        size = Size(barWidth + 4f, barHeight + 4f),
                        style = Stroke(width = 2.dp.toPx()),
                        cornerRadius = CornerRadius(7.dp.toPx())
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. CUSTOM DATE RANGE SELECTOR DIALOG
// -------------------------------------------------------------
@Composable
fun CustomDateRangeDialog(
    initialStart: Long,
    initialEnd: Long,
    onDismiss: () -> Unit,
    onApply: (Long, Long) -> Unit
) {
    val context = LocalContext.current
    var startMillis by remember { mutableStateOf(initialStart) }
    var endMillis by remember { mutableStateOf(initialEnd) }

    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AppTheme.colors.surface,
            border = BorderStroke(1.dp, AppTheme.colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CUSTOM DATE RANGE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppTheme.colors.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Start Date Selector
                Text("START DATE", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = startMillis }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(y, m, d, 0, 0, 0)
                                    }
                                    startMillis = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dateFormat.format(Date(startMillis)), fontWeight = FontWeight.SemiBold, color = AppTheme.colors.textPrimary)
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = AppTheme.colors.primary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // End Date Selector
                Text("END DATE", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = endMillis }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(y, m, d, 23, 59, 59)
                                    }
                                    endMillis = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dateFormat.format(Date(endMillis)), fontWeight = FontWeight.SemiBold, color = AppTheme.colors.textPrimary)
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = AppTheme.colors.primary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = AppTheme.colors.textSecondary)
                    }

                    Button(
                        onClick = {
                            val s = min(startMillis, endMillis)
                            val e = max(startMillis, endMillis)
                            onApply(s, e)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply Filter", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
