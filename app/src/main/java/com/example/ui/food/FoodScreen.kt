package com.example.ui.food

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.SavedFoodItem
import com.example.data.repository.FoodSearchResult
import com.example.data.repository.HeightUnit
import com.example.data.repository.UserPreferences
import com.example.data.repository.WeightUnit
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.BarcodeScannerScreen
import com.example.ui.components.SlideUpBottomSheetDialog
import com.example.ui.components.UserAvatarView
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.AppTheme
import com.example.ui.weight.WeightViewModel
import com.example.util.CalorieWeightCalculator
import com.example.util.PortionCalculator
import com.example.util.WeightTrajectory
import com.example.util.WeightUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

val UK_SUPERMARKET_CHIPS = listOf("All", "Tesco", "Sainsbury's", "ASDA", "M&S", "Morrisons", "Aldi/Lidl", "Cereals", "Dinner Combos", "Snacks & Drinks", "UK Brands")

data class MealCategory(
    val name: String,
    val iconEmoji: String,
    val recommendedPct: String
)

val ALL_MEAL_CATEGORIES = listOf(
    MealCategory("Breakfast", "🍳", "25%"),
    MealCategory("Lunch", "🥗", "35%"),
    MealCategory("Dinner", "🍲", "30%"),
    MealCategory("Snacks", "🍎", "10%"),
    MealCategory("Drinks", "💧", "0-5%")
)

@Composable
fun FoodScreen(
    userPrefs: UserPreferences?,
    viewModel: FoodViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    weightViewModel: WeightViewModel = viewModel()
) {
    val context = LocalContext.current
    val entries by viewModel.foodEntries.collectAsState()
    val weightEntries by weightViewModel.weightEntries.collectAsState()

    var showFoodSearchDialog by remember { mutableStateOf(false) }
    var showCalorieGoalDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var selectedMealForAdd by remember { mutableStateOf("Breakfast") }
    var selectedMealForScan by remember { mutableStateOf("Breakfast") }

    // Scanned product dialog state
    var scannedProductToConfirm by remember { mutableStateOf<FoodSearchResult?>(null) }
    var scannedFallbackBarcode by remember { mutableStateOf<String?>(null) }

    // Food History Selected Date State (Defaults to yesterday)
    var historySelectedDateMillis by remember {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        mutableStateOf(cal.timeInMillis)
    }

    val username = userPrefs?.username ?: "User"
    val avatarId = userPrefs?.avatarId

    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    val latestWeightKg = weightEntries.firstOrNull()?.weightKg ?: 70f
    val userHeightCm = userPrefs?.heightCm ?: 170f
    val userWaistCm = userPrefs?.waistCm
    val userGender = userPrefs?.gender ?: "Male"
    val weightUnit = userPrefs?.weightUnit ?: WeightUnit.KG
    val heightUnit = userPrefs?.heightUnit ?: HeightUnit.CM

    val calculatedBmr = remember(latestWeightKg, userHeightCm, userGender, userWaistCm) {
        weightViewModel.calculateDailyCalories(latestWeightKg, userHeightCm, 30, userGender, userWaistCm)
    }

    val targetDailyCalories = if (userPrefs?.useCustomCalories == true && userPrefs.customDailyCalories > 0) {
        userPrefs.customDailyCalories
    } else {
        calculatedBmr
    }

    // Weekly weight loss projection
    val weeklyProjection = remember(targetDailyCalories, latestWeightKg, userHeightCm, userWaistCm, userGender, weightUnit) {
        CalorieWeightCalculator.calculateWeeklyProjection(
            dailyBudget = targetDailyCalories,
            weightKg = latestWeightKg,
            heightCm = userHeightCm,
            waistCm = userWaistCm,
            age = 30,
            gender = userGender,
            unit = weightUnit
        )
    }

    var isBarcodeLookingUp by remember { mutableStateOf(false) }

    if (showScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = { barcode ->
                showScanner = false
                isBarcodeLookingUp = true
                viewModel.scanBarcodeAndLookup(barcode) { result ->
                    isBarcodeLookingUp = false
                    if (result != null) {
                        scannedProductToConfirm = result
                    } else {
                        scannedFallbackBarcode = barcode
                    }
                }
            },
            onClose = { showScanner = false }
        )
        return
    }

    if (isBarcodeLookingUp) {
        Dialog(onDismissRequest = { isBarcodeLookingUp = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AppTheme.colors.surface,
                border = BorderStroke(1.dp, AppTheme.colors.border),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = AppTheme.colors.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Looking up UK Barcode...",
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Searching Open Food Facts & UK Supermarket database",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
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

    // Barcode confirmation / autofill dialog
    if (scannedProductToConfirm != null) {
        val prod = scannedProductToConfirm!!
        LogScannedProductDialog(
            product = prod,
            initialMealType = selectedMealForScan,
            onDismiss = {
                scannedProductToConfirm = null
                viewModel.clearBarcodeState()
            },
            onConfirm = { name, serving, calories, mealType, barcode, brand, saveForFuture ->
                viewModel.addFoodEntry(
                    name = name,
                    servingSize = serving,
                    calories = calories,
                    mealType = mealType,
                    barcode = barcode,
                    brandOrSupermarket = brand,
                    saveForFuture = saveForFuture
                )
                scannedProductToConfirm = null
                viewModel.clearBarcodeState()
                val saveNotice = if (saveForFuture) " (Saved to My Foods ⭐)" else ""
                Toast.makeText(context, "Logged: $name ($calories kcal)$saveNotice", Toast.LENGTH_SHORT).show()
            }
        )
    } else if (scannedFallbackBarcode != null) {
        val code = scannedFallbackBarcode!!
        LogScannedProductDialog(
            product = FoodSearchResult(
                id = code,
                name = "",
                brandOrSupermarket = "Scanned Item",
                category = "Scanned Food",
                caloriesPerServing = 150,
                servingSize = "1 serving (100g)",
                caloriesPer100g = 150,
                barcode = code,
                isUkSupermarket = false
            ),
            initialMealType = selectedMealForScan,
            onDismiss = {
                scannedFallbackBarcode = null
                viewModel.clearBarcodeState()
            },
            onConfirm = { name, serving, calories, mealType, barcode, brand, saveForFuture ->
                viewModel.addFoodEntry(
                    name = name,
                    servingSize = serving,
                    calories = calories,
                    mealType = mealType,
                    barcode = barcode,
                    brandOrSupermarket = brand,
                    saveForFuture = saveForFuture
                )
                scannedFallbackBarcode = null
                viewModel.clearBarcodeState()
                val saveNotice = if (saveForFuture) " (Saved to My Foods ⭐)" else ""
                Toast.makeText(context, "Logged: $name ($calories kcal)$saveNotice", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // UK Food Search & Log Dialog
    if (showFoodSearchDialog) {
        UkFoodSearchDialog(
            initialMealType = selectedMealForAdd,
            viewModel = viewModel,
            onDismiss = { showFoodSearchDialog = false },
            onScanBarcodeClicked = { meal ->
                selectedMealForScan = meal
                showFoodSearchDialog = false
                showScanner = true
            },
            onSave = { name, serving, calories, mealType, barcode, brand, saveForFuture ->
                viewModel.addFoodEntry(
                    name = name,
                    servingSize = serving,
                    calories = calories,
                    mealType = mealType,
                    barcode = barcode,
                    brandOrSupermarket = brand,
                    saveForFuture = saveForFuture
                )
                showFoodSearchDialog = false
                val saveNotice = if (saveForFuture) " (Saved to My Foods ⭐)" else ""
                Toast.makeText(context, "Logged $name ($calories kcal)$saveNotice", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCalorieGoalDialog) {
        EditCalorieGoalDialog(
            currentUseCustom = userPrefs?.useCustomCalories ?: false,
            currentCustomCalories = userPrefs?.customDailyCalories ?: 2000,
            calculatedBmr = calculatedBmr,
            latestWeightKg = latestWeightKg,
            heightCm = userHeightCm,
            waistCm = userWaistCm,
            gender = userGender,
            weightUnit = weightUnit,
            onDismiss = { showCalorieGoalDialog = false },
            onSave = { useCustom, calories ->
                settingsViewModel.updateUseCustomCalories(useCustom)
                settingsViewModel.updateCustomDailyCalories(calories)
                showCalorieGoalDialog = false
                Toast.makeText(context, "Calorie budget updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Filter today's entries
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val todayEntries = entries.filter { it.date >= todayCal }
    val totalCaloriesConsumed = todayEntries.sumOf { it.calories }
    val remainingCalories = maxOf(0, targetDailyCalories - totalCaloriesConsumed)
    val calProgress = (totalCaloriesConsumed.toFloat() / maxOf(1, targetDailyCalories)).coerceIn(0f, 1f)

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

            // Quick general add food button
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = {
                        selectedMealForScan = "Breakfast"
                        showScanner = true
                    },
                    shape = CircleShape,
                    color = AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = AppTheme.colors.primary, modifier = Modifier.size(20.dp))
                    }
                }

                Surface(
                    onClick = {
                        selectedMealForAdd = "Breakfast"
                        showFoodSearchDialog = true
                    },
                    shape = CircleShape,
                    color = AppTheme.colors.primary,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Add Food", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Calorie Target Card with Weekly Weight Loss Projection
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AppTheme.colors.surface,
            border = BorderStroke(1.dp, AppTheme.colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCalorieGoalDialog = true }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "DAILY CALORIE BUDGET",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.textMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (userPrefs?.useCustomCalories == true) AppTheme.colors.primary.copy(alpha = 0.18f) else AppTheme.colors.surfaceElevated,
                                border = BorderStroke(1.dp, if (userPrefs?.useCustomCalories == true) AppTheme.colors.primary else AppTheme.colors.border)
                            ) {
                                Text(
                                    text = if (userPrefs?.useCustomCalories == true) "CUSTOM" else "AUTO BMR",
                                    color = if (userPrefs?.useCustomCalories == true) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "$totalCaloriesConsumed",
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            Text(
                                " / $targetDailyCalories kcal",
                                color = AppTheme.colors.textSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "REMAINING",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.textMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Edit Calorie Goal",
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            "$remainingCalories kcal",
                            color = if (totalCaloriesConsumed > targetDailyCalories) AppTheme.colors.danger else AppTheme.colors.success,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { calProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (totalCaloriesConsumed > targetDailyCalories) AppTheme.colors.danger else AppTheme.colors.success,
                    trackColor = AppTheme.colors.surfaceElevated,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Weekly Weight Loss Projection Chip
                val projColor = when (weeklyProjection.trajectory) {
                    WeightTrajectory.WEIGHT_LOSS -> AppTheme.colors.success
                    WeightTrajectory.WEIGHT_GAIN -> AppTheme.colors.warning
                    WeightTrajectory.MAINTENANCE -> AppTheme.colors.primary
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = projColor.copy(alpha = if (AppTheme.colors.isDark) 0.15f else 0.10f),
                    border = BorderStroke(1.dp, projColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (weeklyProjection.trajectory == WeightTrajectory.WEIGHT_LOSS) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = projColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = weeklyProjection.summaryText,
                                color = projColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "TDEE: ${weeklyProjection.tdee} kcal/day",
                            color = AppTheme.colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "TODAY'S MEALS",
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.textMuted,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // All 5 persistent meal categories: Breakfast, Lunch, Dinner, Snacks, Drinks
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ALL_MEAL_CATEGORIES) { mealCat ->
                val mealItems = todayEntries.filter { it.mealType.equals(mealCat.name, ignoreCase = true) }
                val mealTotalCalories = mealItems.sumOf { it.calories }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                    border = BorderStroke(1.dp, AppTheme.colors.border)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(mealCat.iconEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = mealCat.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = "Rec: ${mealCat.recommendedPct} • $mealTotalCalories kcal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (mealTotalCalories > 0) AppTheme.colors.primary else AppTheme.colors.textMuted
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        selectedMealForScan = mealCat.name
                                        showScanner = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan barcode for ${mealCat.name}",
                                        tint = AppTheme.colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        selectedMealForAdd = mealCat.name
                                        showFoodSearchDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add to ${mealCat.name}",
                                        tint = AppTheme.colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // List of items or empty state
                        if (mealItems.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedMealForAdd = mealCat.name
                                        showFoodSearchDialog = true
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = AppTheme.colors.surfaceElevated,
                                border = BorderStroke(1.dp, AppTheme.colors.border)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "No items logged yet",
                                        color = AppTheme.colors.textMuted,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "+ Log ${mealCat.name}",
                                        color = AppTheme.colors.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                mealItems.forEach { item ->
                                    FoodItemRow(
                                        item = item,
                                        onDelete = { viewModel.deleteFoodEntry(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                FoodHistorySection(
                    entries = entries,
                    selectedDateMillis = historySelectedDateMillis,
                    targetDailyCalories = targetDailyCalories,
                    onSelectDate = { historySelectedDateMillis = it },
                    onCopyItemToToday = { item ->
                        viewModel.copyEntryToToday(item)
                        Toast.makeText(context, "Copied \"${item.name}\" to today's log!", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteItem = { item ->
                        viewModel.deleteFoodEntry(item)
                        Toast.makeText(context, "Removed \"${item.name}\"", Toast.LENGTH_SHORT).show()
                    },
                    onLogNewForDate = { mealName, date ->
                        selectedMealForAdd = mealName
                        showFoodSearchDialog = true
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun FoodItemRow(
    item: FoodEntry,
    onDelete: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Delete Food Log", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Remove \"${item.name}\" (${item.calories} kcal) from today's log?",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showConfirmDelete = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.danger)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Cancel", color = AppTheme.colors.textMuted)
                }
            },
            containerColor = AppTheme.colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = AppTheme.colors.surfaceElevated,
        border = BorderStroke(1.dp, AppTheme.colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = AppTheme.colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.servingSize,
                    color = AppTheme.colors.textMuted,
                    fontSize = 11.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${item.calories} kcal",
                    color = AppTheme.colors.success,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                IconButton(
                    onClick = { showConfirmDelete = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete food entry",
                        tint = AppTheme.colors.textMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FoodHistorySection(
    entries: List<FoodEntry>,
    selectedDateMillis: Long,
    targetDailyCalories: Int,
    onSelectDate: (Long) -> Unit,
    onCopyItemToToday: (FoodEntry) -> Unit,
    onDeleteItem: (FoodEntry) -> Unit,
    onLogNewForDate: (String, Long) -> Unit
) {
    val context = LocalContext.current
    val fullDateFmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
    val shortDateFmt = SimpleDateFormat("EEE d MMM", Locale.getDefault())
    val dayOnlyFmt = SimpleDateFormat("d MMM", Locale.getDefault())

    // Normalize selected date to midnight
    val selectedCal = Calendar.getInstance().apply {
        timeInMillis = selectedDateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfDay = selectedCal.timeInMillis
    val endOfDay = startOfDay + 86400000L - 1L

    // Filter items for selected historical date
    val dayItems = entries.filter { it.date in startOfDay..endOfDay }
    val dayTotalCalories = dayItems.sumOf { it.calories }

    // Group items by meal category
    val breakfastItems = dayItems.filter { it.mealType.equals("Breakfast", true) }
    val lunchItems = dayItems.filter { it.mealType.equals("Lunch", true) }
    val dinnerItems = dayItems.filter { it.mealType.equals("Dinner", true) }
    val snacksItems = dayItems.filter { it.mealType.equals("Snacks", true) || it.mealType.equals("Snack", true) }
    val drinksItems = dayItems.filter { it.mealType.equals("Drinks", true) || it.mealType.equals("Drink", true) }

    // Generate recent 7 past days for quick tabs
    val recentPastDays = remember {
        (1..7).map { daysAgo ->
            val c = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            c.timeInMillis
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        border = BorderStroke(1.dp, AppTheme.colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📅", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "FOOD LOG HISTORY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = AppTheme.colors.textMuted
                        )
                        Text(
                            text = "Browse what you ate on past days",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AppTheme.colors.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${dayItems.size} items",
                        color = AppTheme.colors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date Navigation Bar (< Date Selector >)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppTheme.colors.surfaceElevated,
                border = BorderStroke(1.dp, AppTheme.colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val prevCal = Calendar.getInstance().apply {
                                timeInMillis = startOfDay
                                add(Calendar.DAY_OF_YEAR, -1)
                            }
                            onSelectDate(prevCal.timeInMillis)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = AppTheme.colors.textPrimary)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppTheme.colors.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable {
                            val cur = Calendar.getInstance().apply { timeInMillis = startOfDay }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(y, m, d, 0, 0, 0)
                                    }
                                    onSelectDate(newCal.timeInMillis)
                                },
                                cur.get(Calendar.YEAR),
                                cur.get(Calendar.MONTH),
                                cur.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AppTheme.colors.primary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = fullDateFmt.format(Date(startOfDay)),
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    val todayMidnight = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val canGoNext = startOfDay < todayMidnight

                    IconButton(
                        onClick = {
                            if (canGoNext) {
                                val nextCal = Calendar.getInstance().apply {
                                    timeInMillis = startOfDay
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }
                                onSelectDate(nextCal.timeInMillis)
                            }
                        },
                        enabled = canGoNext,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next Day",
                            tint = if (canGoNext) AppTheme.colors.textPrimary else AppTheme.colors.textMuted.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Past Days Carousel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recentPastDays.forEachIndexed { idx, dayMillis ->
                    val isSelected = (dayMillis in startOfDay..endOfDay)
                    val label = if (idx == 0) "Yesterday" else shortDateFmt.format(Date(dayMillis))
                    val hasFood = entries.any { it.date in dayMillis..(dayMillis + 86400000L - 1L) }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) AppTheme.colors.primary.copy(alpha = 0.22f) else AppTheme.colors.surfaceElevated,
                        border = BorderStroke(1.dp, if (isSelected) AppTheme.colors.primary else AppTheme.colors.border),
                        modifier = Modifier.clickable { onSelectDate(dayMillis) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            if (hasFood) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AppTheme.colors.success)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = label,
                                color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Historical Day Summary Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AppTheme.colors.surfaceElevated,
                border = BorderStroke(1.dp, AppTheme.colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL CONSUMED",
                                color = AppTheme.colors.textMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$dayTotalCalories",
                                    color = AppTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = " / $targetDailyCalories kcal",
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                                )
                            }
                        }

                        val diff = dayTotalCalories - targetDailyCalories
                        val isOver = diff > 0
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = (if (isOver) AppTheme.colors.danger else AppTheme.colors.success).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (dayTotalCalories == 0) "No calories" else if (isOver) "+$diff kcal over" else "${abs(diff)} kcal under",
                                color = if (isOver) AppTheme.colors.danger else AppTheme.colors.success,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (dayTotalCalories > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val brkCal = breakfastItems.sumOf { it.calories }
                            val lunCal = lunchItems.sumOf { it.calories }
                            val dinCal = dinnerItems.sumOf { it.calories }
                            val snkCal = snacksItems.sumOf { it.calories }
                            val drkCal = drinksItems.sumOf { it.calories }

                            MiniMealChip("🍳 Brk", brkCal, Color(0xFFFB923C), Modifier.weight(1f))
                            MiniMealChip("🥗 Lun", lunCal, AppTheme.colors.success, Modifier.weight(1f))
                            MiniMealChip("🍲 Din", dinCal, AppTheme.colors.primary, Modifier.weight(1f))
                            MiniMealChip("🥨 Snk", snkCal, Color(0xFFA78BFA), Modifier.weight(1f))
                            MiniMealChip("💧 Drk", drkCal, Color(0xFF38BDF8), Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Food Items List for that Past Day
            if (dayItems.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppTheme.colors.surfaceElevated.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            tint = AppTheme.colors.textMuted,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No meals logged on ${dayOnlyFmt.format(Date(startOfDay))}",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { onLogNewForDate("Breakfast", startOfDay) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Food For This Date", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text(
                    text = "LOGGED ITEMS (${dayItems.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dayItems.forEach { item ->
                        HistoryFoodItemRow(
                            item = item,
                            onCopyToday = { onCopyItemToToday(item) },
                            onDelete = { onDeleteItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniMealChip(label: String, calories: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("${calories}k", color = AppTheme.colors.textPrimary, fontSize = 9.sp)
        }
    }
}

@Composable
fun HistoryFoodItemRow(
    item: FoodEntry,
    onCopyToday: () -> Unit,
    onDelete: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Delete Past Log", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Remove \"${item.name}\" (${item.calories} kcal) from this past day?",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showConfirmDelete = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.danger)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Cancel", color = AppTheme.colors.textMuted)
                }
            },
            containerColor = AppTheme.colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    val mealIcon = when (item.mealType.lowercase()) {
        "breakfast" -> "🍳"
        "lunch" -> "🥗"
        "dinner" -> "🍲"
        "drinks", "drink" -> "💧"
        else -> "🥨"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppTheme.colors.surfaceElevated,
        border = BorderStroke(1.dp, AppTheme.colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(mealIcon, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = item.name,
                        color = AppTheme.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.servingSize} • ${item.mealType}",
                        color = AppTheme.colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${item.calories} kcal",
                    color = AppTheme.colors.success,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )

                // 1-Tap Copy to Today button
                Surface(
                    onClick = onCopyToday,
                    shape = RoundedCornerShape(8.dp),
                    color = AppTheme.colors.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy to Today's log",
                            tint = AppTheme.colors.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { showConfirmDelete = true },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete entry",
                        tint = AppTheme.colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

fun getSupermarketBrandColor(brand: String): Color {
    return when {
        brand.contains("Tesco", ignoreCase = true) -> Color(0xFF0284C7)
        brand.contains("Sainsbury", ignoreCase = true) -> Color(0xFFEA580C)
        brand.contains("ASDA", ignoreCase = true) -> Color(0xFF16A34A)
        brand.contains("M&S", ignoreCase = true) || brand.contains("Marks", ignoreCase = true) -> Color(0xFFD97706)
        brand.contains("Morrisons", ignoreCase = true) -> Color(0xFF65A30D)
        brand.contains("Aldi", ignoreCase = true) -> Color(0xFF2563EB)
        brand.contains("Lidl", ignoreCase = true) -> Color(0xFFDC2626)
        brand.contains("Heinz", ignoreCase = true) -> Color(0xFF0D9488)
        brand.contains("Warburtons", ignoreCase = true) -> Color(0xFFDB2777)
        brand.contains("Cadbury", ignoreCase = true) -> Color(0xFF7C3AED)
        brand.contains("Greggs", ignoreCase = true) -> Color(0xFF0284C7)
        else -> Color(0xFF64748B)
    }
}

/**
 * Revamped UK Supermarket Food Search & Log Dialog
 */
@Composable
fun UkFoodSearchDialog(
    initialMealType: String,
    viewModel: FoodViewModel,
    onDismiss: () -> Unit,
    onScanBarcodeClicked: (meal: String) -> Unit,
    onSave: (name: String, serving: String, calories: Int, mealType: String, barcode: String?, brand: String, saveForFuture: Boolean) -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSupermarket by viewModel.selectedSupermarket.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val savedFoods by viewModel.savedFoodItems.collectAsState()

    var selectedMeal by remember { mutableStateOf(initialMealType) }
    var activeTab by remember { mutableStateOf(0) } // 0: UK Supermarkets, 1: Saved Items, 2: Custom Entry

    // UK Search Portions
    var selectedItemForPortion by remember { mutableStateOf<FoodSearchResult?>(null) }
    var portionMultiplier by remember { mutableStateOf(1.0f) }

    // Saved Items Tab state
    var savedSearchQuery by remember { mutableStateOf("") }
    var selectedSavedForPortion by remember { mutableStateOf<SavedFoodItem?>(null) }
    var savedPortionMultiplier by remember { mutableStateOf(1.0f) }

    // Custom Food Tab state (Automatic Portion & Gram Calculator)
    var customName by remember { mutableStateOf("") }
    var customBrand by remember { mutableStateOf("Homemade") }
    var customBaseGramsText by remember { mutableStateOf("100") }
    var customBaseCaloriesText by remember { mutableStateOf("") }
    var customServingUnit by remember { mutableStateOf("g") } // "g", "ml", "serv"
    var customEatenGramsText by remember { mutableStateOf("100") }
    var customMultiplier by remember { mutableStateOf(1.0f) }
    var customServingLabelOverride by remember { mutableStateOf("") }
    var isManualServingLabel by remember { mutableStateOf(false) }
    var saveCustomForFuture by remember { mutableStateOf(true) }

    SlideUpBottomSheetDialog(
        onDismissRequest = onDismiss,
        maxHeightFraction = 0.92f
    ) { dismissWithAnim ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            // Top header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "LOG FOOD & DRINKS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textMuted,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        "UK Database, Saved Items & Custom",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.primary,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = dismissWithAnim, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = AppTheme.colors.textMuted)
                }
            }

                Spacer(modifier = Modifier.height(10.dp))

                // Meal category selector pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf("Breakfast", "Lunch", "Dinner", "Snacks", "Drinks").forEach { meal ->
                        val isSelected = selectedMeal.equals(meal, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AppTheme.colors.primary.copy(alpha = 0.18f) else AppTheme.colors.surfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) AppTheme.colors.primary else AppTheme.colors.border),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMeal = meal }
                        ) {
                            Text(
                                text = meal.take(4),
                                color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Switcher (UK Database, Saved Items, Custom Food)
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = AppTheme.colors.surfaceElevated,
                    contentColor = AppTheme.colors.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("UK Store", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (activeTab == 1) Color(0xFFFFB300) else AppTheme.colors.textMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Saved (${savedFoods.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Custom", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (activeTab) {
                    0 -> {
                        // UK Supermarket Database Search
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search Tesco, ASDA, Sainsbury's, Heinz...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = AppTheme.colors.textMuted)
                            },
                            trailingIcon = {
                                IconButton(onClick = { onScanBarcodeClicked(selectedMeal) }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = AppTheme.colors.primary)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colors.primary,
                                unfocusedBorderColor = AppTheme.colors.border,
                                focusedContainerColor = AppTheme.colors.inputBackground,
                                unfocusedContainerColor = AppTheme.colors.inputBackground,
                                focusedTextColor = AppTheme.colors.textPrimary,
                                unfocusedTextColor = AppTheme.colors.textPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Supermarket Chips Filter Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            UK_SUPERMARKET_CHIPS.forEach { market ->
                                val isSelected = selectedSupermarket == market
                                val chipColor = getSupermarketBrandColor(market)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) chipColor.copy(alpha = 0.25f) else AppTheme.colors.surfaceElevated,
                                    border = BorderStroke(1.dp, if (isSelected) chipColor else AppTheme.colors.border),
                                    modifier = Modifier.clickable {
                                        viewModel.onSupermarketFilterChanged(market)
                                    }
                                ) {
                                    Text(
                                        text = market,
                                        color = if (isSelected) chipColor else AppTheme.colors.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Portion Selector Overlay if an item is selected
                        if (selectedItemForPortion != null) {
                            val item = selectedItemForPortion!!
                            val calculatedCalories = (item.caloriesPerServing * portionMultiplier).toInt()
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = AppTheme.colors.surfaceElevated,
                                border = BorderStroke(1.5.dp, AppTheme.colors.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.Bold,
                                                color = AppTheme.colors.textPrimary,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.brandOrSupermarket} • ${item.servingSize}",
                                                color = AppTheme.colors.textSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = { selectedItemForPortion = null },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Deselect", tint = AppTheme.colors.textMuted)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(0.5f to "1/2", 1.0f to "1x", 1.5f to "1.5x", 2.0f to "2x").forEach { (mult, label) ->
                                            val isMultSelected = portionMultiplier == mult
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isMultSelected) AppTheme.colors.primary else AppTheme.colors.surface,
                                                border = BorderStroke(1.dp, if (isMultSelected) AppTheme.colors.primary else AppTheme.colors.border),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { portionMultiplier = mult }
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isMultSelected) Color.White else AppTheme.colors.textPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$calculatedCalories kcal",
                                            color = AppTheme.colors.success,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.saveFoodItemDirectly(
                                                        name = item.name,
                                                        servingSize = item.servingSize,
                                                        calories = item.caloriesPerServing,
                                                        defaultMealType = selectedMeal,
                                                        barcode = item.barcode,
                                                        brandOrSupermarket = item.brandOrSupermarket
                                                    )
                                                    Toast.makeText(context, "Saved '${item.name}' to My Foods ⭐", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                border = BorderStroke(1.dp, Color(0xFFFFB300))
                                            ) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Save", fontSize = 11.sp, color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    val servingDesc = if (portionMultiplier == 1.0f) item.servingSize else "${portionMultiplier}x ${item.servingSize}"
                                                    onSave(item.name, servingDesc, calculatedCalories, selectedMeal, item.barcode, item.brandOrSupermarket, false)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Log to $selectedMeal", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Results list
                        if (isSearching) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AppTheme.colors.primary, modifier = Modifier.size(24.dp))
                            }
                        } else if (searchResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("No UK items found matching query", color = AppTheme.colors.textMuted, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Try switching to 'Custom' tab to add manually",
                                        color = AppTheme.colors.primary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.clickable { activeTab = 2 }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(searchResults) { item ->
                                    UkFoodResultItemRow(
                                        item = item,
                                        onSelect = {
                                            selectedItemForPortion = item
                                            portionMultiplier = 1.0f
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Saved Foods Tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (savedFoods.isNotEmpty()) {
                                OutlinedTextField(
                                    value = savedSearchQuery,
                                    onValueChange = { savedSearchQuery = it },
                                    placeholder = { Text("Filter your saved foods...", fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = "Search", tint = AppTheme.colors.textMuted)
                                    },
                                    trailingIcon = {
                                        if (savedSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { savedSearchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = AppTheme.colors.textMuted)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppTheme.colors.primary,
                                        unfocusedBorderColor = AppTheme.colors.border,
                                        focusedContainerColor = AppTheme.colors.inputBackground,
                                        unfocusedContainerColor = AppTheme.colors.inputBackground,
                                        focusedTextColor = AppTheme.colors.textPrimary,
                                        unfocusedTextColor = AppTheme.colors.textPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Portion Selector Overlay if a saved item is selected
                            if (selectedSavedForPortion != null) {
                                val item = selectedSavedForPortion!!
                                val calculatedCalories = (item.calories * savedPortionMultiplier).toInt()
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = AppTheme.colors.surfaceElevated,
                                    border = BorderStroke(1.5.dp, AppTheme.colors.primary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.name,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AppTheme.colors.textPrimary,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${item.brandOrSupermarket} • ${item.servingSize}",
                                                    color = AppTheme.colors.textSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            IconButton(
                                                onClick = { selectedSavedForPortion = null },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Deselect", tint = AppTheme.colors.textMuted)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(0.5f to "1/2", 1.0f to "1x", 1.5f to "1.5x", 2.0f to "2x").forEach { (mult, label) ->
                                                val isMultSelected = savedPortionMultiplier == mult
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isMultSelected) AppTheme.colors.primary else AppTheme.colors.surface,
                                                    border = BorderStroke(1.dp, if (isMultSelected) AppTheme.colors.primary else AppTheme.colors.border),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { savedPortionMultiplier = mult }
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = if (isMultSelected) Color.White else AppTheme.colors.textPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$calculatedCalories kcal",
                                                color = AppTheme.colors.success,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            Button(
                                                onClick = {
                                                    val servingDesc = if (savedPortionMultiplier == 1.0f) item.servingSize else "${savedPortionMultiplier}x ${item.servingSize}"
                                                    onSave(item.name, servingDesc, calculatedCalories, selectedMeal, item.barcode, item.brandOrSupermarket, false)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Text("Log to $selectedMeal", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            val filteredSaved = remember(savedFoods, savedSearchQuery) {
                                if (savedSearchQuery.isBlank()) savedFoods
                                else savedFoods.filter {
                                    it.name.contains(savedSearchQuery, ignoreCase = true) ||
                                    it.brandOrSupermarket.contains(savedSearchQuery, ignoreCase = true) ||
                                    (it.barcode != null && it.barcode.contains(savedSearchQuery))
                                }
                            }

                            if (savedFoods.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = AppTheme.colors.primary.copy(alpha = 0.12f),
                                            modifier = Modifier.size(52.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.StarBorder, contentDescription = null, tint = AppTheme.colors.primary, modifier = Modifier.size(26.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("No Saved Products Yet", fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "When scanning barcodes or adding custom foods, check 'Save product for future use' to build your quick-select list.",
                                            color = AppTheme.colors.textSecondary,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Button(
                                            onClick = { activeTab = 2 },
                                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add Custom Food", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else if (filteredSaved.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No saved items matching \"$savedSearchQuery\"", color = AppTheme.colors.textMuted, fontSize = 13.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredSaved, key = { it.id }) { item ->
                                        SavedFoodItemRow(
                                            item = item,
                                            onSelect = {
                                                selectedSavedForPortion = item
                                                savedPortionMultiplier = 1.0f
                                            },
                                            onDelete = {
                                                viewModel.deleteSavedFoodItem(item)
                                                Toast.makeText(context, "Removed ${item.name} from saved items", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Manual Custom Entry Tab with Automatic Portion & Gram Calorie Scaling
                        val baseGrams = customBaseGramsText.toFloatOrNull() ?: 100f
                        val baseCalories = customBaseCaloriesText.toFloatOrNull() ?: 0f
                        val eatenGrams = customEatenGramsText.toFloatOrNull() ?: baseGrams

                        // Real-time automatic calorie calculation based on gram ratio or multiplier
                        val autoCalculatedCalories = if (baseGrams > 0f && baseCalories > 0f) {
                            PortionCalculator.calculateScaledCalories(
                                baseCalories = baseCalories,
                                baseGrams = baseGrams,
                                targetGrams = eatenGrams
                            )
                        } else {
                            (baseCalories * customMultiplier).toInt()
                        }

                        val autoServingDesc = PortionCalculator.formatPortionDescription(
                            targetGrams = eatenGrams,
                            baseGrams = baseGrams,
                            multiplier = customMultiplier,
                            baseServingUnit = customServingUnit
                        )
                        val effectiveServing = if (isManualServingLabel && customServingLabelOverride.isNotBlank()) {
                            customServingLabelOverride
                        } else {
                            autoServingDesc
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Food / Beverage Name") },
                                placeholder = { Text("e.g. Homemade Roast Chicken, Oats & Honey") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppTheme.colors.primary,
                                    unfocusedBorderColor = AppTheme.colors.border,
                                    focusedContainerColor = AppTheme.colors.inputBackground,
                                    unfocusedContainerColor = AppTheme.colors.inputBackground,
                                    focusedTextColor = AppTheme.colors.textPrimary,
                                    unfocusedTextColor = AppTheme.colors.textPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = customBrand,
                                onValueChange = { customBrand = it },
                                label = { Text("Brand / Tag (Optional)") },
                                placeholder = { Text("e.g. Homemade, Meal Prep, Local Cafe") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppTheme.colors.primary,
                                    unfocusedBorderColor = AppTheme.colors.border,
                                    focusedContainerColor = AppTheme.colors.inputBackground,
                                    unfocusedContainerColor = AppTheme.colors.inputBackground,
                                    focusedTextColor = AppTheme.colors.textPrimary,
                                    unfocusedTextColor = AppTheme.colors.textPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 1. Reference Serving & Calories Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppTheme.colors.surfaceElevated,
                                border = BorderStroke(1.dp, AppTheme.colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "1. BASELINE / PACKET REFERENCE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppTheme.colors.primary,
                                            letterSpacing = 0.8.sp
                                        )
                                        // Unit selector chips
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf("g" to "g", "ml" to "ml", "portion" to "serv").forEach { (u, label) ->
                                                val isSelected = customServingUnit == u
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.surface,
                                                    border = BorderStroke(1.dp, if (isSelected) AppTheme.colors.primary else AppTheme.colors.border),
                                                    modifier = Modifier.clickable { customServingUnit = u }
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else AppTheme.colors.textSecondary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = customBaseGramsText,
                                            onValueChange = {
                                                if (it.all { c -> c.isDigit() || c == '.' }) {
                                                    customBaseGramsText = it
                                                    // Also keep eaten grams in sync if multiplier is 1x
                                                    if (customMultiplier == 1.0f) {
                                                        customEatenGramsText = it
                                                    } else {
                                                        val bg = it.toFloatOrNull() ?: 100f
                                                        customEatenGramsText = ((bg * customMultiplier * 10).toInt() / 10f).toString().removeSuffix(".0")
                                                    }
                                                }
                                            },
                                            label = { Text("Base Size ($customServingUnit)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AppTheme.colors.primary,
                                                unfocusedBorderColor = AppTheme.colors.border,
                                                focusedContainerColor = AppTheme.colors.inputBackground,
                                                unfocusedContainerColor = AppTheme.colors.inputBackground,
                                                focusedTextColor = AppTheme.colors.textPrimary,
                                                unfocusedTextColor = AppTheme.colors.textPrimary
                                            )
                                        )

                                        OutlinedTextField(
                                            value = customBaseCaloriesText,
                                            onValueChange = { if (it.all { c -> c.isDigit() }) customBaseCaloriesText = it },
                                            label = { Text("Base Calories (kcal)") },
                                            placeholder = { Text("e.g. 250") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AppTheme.colors.primary,
                                                unfocusedBorderColor = AppTheme.colors.border,
                                                focusedContainerColor = AppTheme.colors.inputBackground,
                                                unfocusedContainerColor = AppTheme.colors.inputBackground,
                                                focusedTextColor = AppTheme.colors.textPrimary,
                                                unfocusedTextColor = AppTheme.colors.textPrimary
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Quick baseline presets
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(100f to "100$customServingUnit", 50f to "50$customServingUnit", 30f to "30$customServingUnit", 1f to "1 serv").forEach { (presetGrams, label) ->
                                            val isSelected = baseGrams == presetGrams
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSelected) AppTheme.colors.primary.copy(alpha = 0.15f) else AppTheme.colors.surface,
                                                border = BorderStroke(1.dp, if (isSelected) AppTheme.colors.primary else AppTheme.colors.border),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        val strVal = if (presetGrams % 1f == 0f) presetGrams.toInt().toString() else presetGrams.toString()
                                                        customBaseGramsText = strVal
                                                        customEatenGramsText = ((presetGrams * customMultiplier * 10).toInt() / 10f).toString().removeSuffix(".0")
                                                    }
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 2. Portion Size & Auto-Calculation Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppTheme.colors.surfaceElevated,
                                border = BorderStroke(1.5.dp, AppTheme.colors.primary.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "2. EATEN AMOUNT & AUTO-CALCULATOR",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppTheme.colors.primary,
                                            letterSpacing = 0.8.sp
                                        )
                                        Surface(
                                            color = AppTheme.colors.success.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Auto-Scaling Active",
                                                color = AppTheme.colors.success,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Gram input with stepper buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Minus 50g
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AppTheme.colors.surface,
                                            border = BorderStroke(1.dp, AppTheme.colors.border),
                                            modifier = Modifier.clickable {
                                                val next = (eatenGrams - 50f).coerceAtLeast(5f)
                                                customEatenGramsText = if (next % 1f == 0f) next.toInt().toString() else next.toString()
                                                if (baseGrams > 0f) customMultiplier = (next / baseGrams)
                                            }
                                        ) {
                                            Text("-50", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                                        }

                                        // Minus 10g
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AppTheme.colors.surface,
                                            border = BorderStroke(1.dp, AppTheme.colors.border),
                                            modifier = Modifier.clickable {
                                                val next = (eatenGrams - 10f).coerceAtLeast(1f)
                                                customEatenGramsText = if (next % 1f == 0f) next.toInt().toString() else next.toString()
                                                if (baseGrams > 0f) customMultiplier = (next / baseGrams)
                                            }
                                        ) {
                                            Text("-10", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                                        }

                                        OutlinedTextField(
                                            value = customEatenGramsText,
                                            onValueChange = {
                                                if (it.all { c -> c.isDigit() || c == '.' }) {
                                                    customEatenGramsText = it
                                                    val eg = it.toFloatOrNull() ?: 0f
                                                    if (baseGrams > 0f && eg > 0f) {
                                                        customMultiplier = eg / baseGrams
                                                    }
                                                }
                                            },
                                            label = { Text("Eaten ($customServingUnit)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AppTheme.colors.primary,
                                                unfocusedBorderColor = AppTheme.colors.border,
                                                focusedContainerColor = AppTheme.colors.inputBackground,
                                                unfocusedContainerColor = AppTheme.colors.inputBackground,
                                                focusedTextColor = AppTheme.colors.textPrimary,
                                                unfocusedTextColor = AppTheme.colors.textPrimary
                                            )
                                        )

                                        // Plus 10g
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AppTheme.colors.surface,
                                            border = BorderStroke(1.dp, AppTheme.colors.border),
                                            modifier = Modifier.clickable {
                                                val next = eatenGrams + 10f
                                                customEatenGramsText = if (next % 1f == 0f) next.toInt().toString() else next.toString()
                                                if (baseGrams > 0f) customMultiplier = (next / baseGrams)
                                            }
                                        ) {
                                            Text("+10", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                                        }

                                        // Plus 50g
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AppTheme.colors.surface,
                                            border = BorderStroke(1.dp, AppTheme.colors.border),
                                            modifier = Modifier.clickable {
                                                val next = eatenGrams + 50f
                                                customEatenGramsText = if (next % 1f == 0f) next.toInt().toString() else next.toString()
                                                if (baseGrams > 0f) customMultiplier = (next / baseGrams)
                                            }
                                        ) {
                                            Text("+50", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Multiplier quick chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(0.5f to "½ Half", 1.0f to "1x Single", 1.5f to "1.5x", 2.0f to "2x Double", 3.0f to "3x").forEach { (mult, label) ->
                                            val isSelected = (Math.abs(customMultiplier - mult) < 0.05f) || (baseGrams > 0f && Math.abs(eatenGrams - (baseGrams * mult)) < 0.5f)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.surface,
                                                border = BorderStroke(1.dp, if (isSelected) AppTheme.colors.primary else AppTheme.colors.border),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        customMultiplier = mult
                                                        val targetG = baseGrams * mult
                                                        customEatenGramsText = if (targetG % 1f == 0f) targetG.toInt().toString() else ((targetG * 10).toInt() / 10f).toString().removeSuffix(".0")
                                                    }
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else AppTheme.colors.textPrimary,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Live calculation banner
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AppTheme.colors.surface,
                                        border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "CALCULATED TOTAL",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AppTheme.colors.textMuted,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Text(
                                                    text = if (baseCalories > 0f) {
                                                        "${baseCalories.toInt()} kcal per ${baseGrams.toInt()}$customServingUnit × ${eatenGrams.toInt()}$customServingUnit"
                                                    } else {
                                                        "Enter base calories above to calculate"
                                                    },
                                                    fontSize = 11.sp,
                                                    color = AppTheme.colors.textSecondary
                                                )
                                            }
                                            Text(
                                                text = "$autoCalculatedCalories kcal",
                                                color = AppTheme.colors.success,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. Portion label & Custom Description
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AppTheme.colors.surfaceElevated,
                                border = BorderStroke(1.dp, AppTheme.colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Logged Portion Label:",
                                            fontSize = 11.sp,
                                            color = AppTheme.colors.textMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = if (isManualServingLabel) "Manual Edit" else "Auto-generated",
                                            fontSize = 10.sp,
                                            color = AppTheme.colors.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                isManualServingLabel = !isManualServingLabel
                                                if (isManualServingLabel && customServingLabelOverride.isBlank()) {
                                                    customServingLabelOverride = autoServingDesc
                                                }
                                            }
                                        )
                                    }
                                    if (isManualServingLabel) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = customServingLabelOverride,
                                            onValueChange = { customServingLabelOverride = it },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AppTheme.colors.primary,
                                                unfocusedBorderColor = AppTheme.colors.border,
                                                focusedContainerColor = AppTheme.colors.inputBackground,
                                                unfocusedContainerColor = AppTheme.colors.inputBackground,
                                                focusedTextColor = AppTheme.colors.textPrimary,
                                                unfocusedTextColor = AppTheme.colors.textPrimary
                                            )
                                        )
                                    } else {
                                        Text(
                                            text = effectiveServing,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppTheme.colors.textPrimary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Save for future use toggle
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (saveCustomForFuture) AppTheme.colors.primary.copy(alpha = 0.12f) else AppTheme.colors.surfaceElevated,
                                border = BorderStroke(1.dp, if (saveCustomForFuture) AppTheme.colors.primary.copy(alpha = 0.45f) else AppTheme.colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { saveCustomForFuture = !saveCustomForFuture }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = saveCustomForFuture,
                                        onCheckedChange = { saveCustomForFuture = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = AppTheme.colors.primary,
                                            checkmarkColor = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (saveCustomForFuture) Color(0xFFFFB300) else AppTheme.colors.textMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Save baseline to My Saved Foods",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = AppTheme.colors.textPrimary
                                            )
                                        }
                                        Text(
                                            "Easily re-scale and log this item anytime from the Saved tab",
                                            fontSize = 10.sp,
                                            color = AppTheme.colors.textSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val cal = if (baseCalories > 0f) baseCalories.toInt() else autoCalculatedCalories
                                        val baseServ = "${baseGrams.toInt()}$customServingUnit"
                                        viewModel.saveFoodItemDirectly(
                                            name = customName,
                                            servingSize = baseServ,
                                            calories = cal,
                                            defaultMealType = selectedMeal,
                                            barcode = null,
                                            brandOrSupermarket = customBrand.ifBlank { "Custom Food" }
                                        )
                                        Toast.makeText(context, "Saved '$customName' ($baseServ = $cal kcal) ⭐", Toast.LENGTH_SHORT).show()
                                        activeTab = 1 // Switch to Saved tab
                                    },
                                    enabled = customName.isNotBlank() && (customBaseCaloriesText.isNotBlank() || autoCalculatedCalories > 0),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(46.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Baseline", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        onSave(
                                            customName,
                                            effectiveServing,
                                            autoCalculatedCalories,
                                            selectedMeal,
                                            null,
                                            customBrand.ifBlank { "Custom Food" },
                                            saveCustomForFuture
                                        )
                                    },
                                    enabled = customName.isNotBlank() && (customBaseCaloriesText.isNotBlank() || autoCalculatedCalories > 0),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary)
                                ) {
                                    Text("Log $autoCalculatedCalories kcal to $selectedMeal", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
fun SavedFoodItemRow(
    item: SavedFoodItem,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val brandColor = getSupermarketBrandColor(item.brandOrSupermarket)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppTheme.colors.surfaceElevated,
        border = BorderStroke(1.dp, AppTheme.colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = brandColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, brandColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = item.brandOrSupermarket,
                            color = brandColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    if (item.barcode != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = "Barcode available",
                            tint = AppTheme.colors.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.name,
                    color = AppTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.servingSize,
                    color = AppTheme.colors.textMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${item.calories} kcal",
                        color = AppTheme.colors.success,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Tap to add",
                        color = AppTheme.colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Remove saved item",
                        tint = AppTheme.colors.textMuted.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UkFoodResultItemRow(
    item: FoodSearchResult,
    onSelect: () -> Unit
) {
    val brandColor = getSupermarketBrandColor(item.brandOrSupermarket)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppTheme.colors.surfaceElevated,
        border = BorderStroke(1.dp, AppTheme.colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = brandColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, brandColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = item.brandOrSupermarket,
                            color = brandColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    if (item.barcode != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = "Barcode available",
                            tint = AppTheme.colors.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.name,
                    color = AppTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${item.servingSize} • ${item.caloriesPer100g} kcal/100g",
                    color = AppTheme.colors.textMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.caloriesPerServing} kcal",
                    color = AppTheme.colors.success,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Tap to add",
                    color = AppTheme.colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Scanned Barcode Autofill & Confirm Dialog
 */
@Composable
fun LogScannedProductDialog(
    product: FoodSearchResult,
    initialMealType: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, serving: String, calories: Int, mealType: String, barcode: String?, brand: String, saveForFuture: Boolean) -> Unit
) {
    var editableName by remember { mutableStateOf(product.name) }
    var editableServing by remember { mutableStateOf(product.servingSize) }
    var editableCalories by remember { mutableStateOf(product.caloriesPerServing.toString()) }
    var scannedMultiplier by remember { mutableStateOf(1.0f) }
    var selectedMeal by remember { mutableStateOf(initialMealType) }
    var saveForFuture by remember { mutableStateOf(true) }

    val parsedBaseCal = editableCalories.toIntOrNull() ?: product.caloriesPerServing
    val scaledCalories = (parsedBaseCal * scannedMultiplier).toInt()
    val finalServing = if (scannedMultiplier == 1.0f) editableServing else "${if (scannedMultiplier % 1f == 0f) scannedMultiplier.toInt().toString() else scannedMultiplier.toString()}x $editableServing"

    val brandColor = getSupermarketBrandColor(product.brandOrSupermarket)
    val isFound = product.name.isNotBlank()

    SlideUpBottomSheetDialog(
        onDismissRequest = onDismiss,
        maxHeightFraction = 0.90f
    ) { dismissWithAnim ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = if (isFound) AppTheme.colors.success else AppTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFound) "PRODUCT IDENTIFIED" else "NEW SCANNED PRODUCT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFound) AppTheme.colors.success else AppTheme.colors.primary,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(onClick = dismissWithAnim, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = AppTheme.colors.textMuted)
                }
            }

                Spacer(modifier = Modifier.height(14.dp))

                // Status Banner
                if (!isFound) {
                    Surface(
                        color = AppTheme.colors.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Barcode scanned! Enter product name and calories below to log.",
                            color = AppTheme.colors.primary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Brand Pill & Barcode
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = brandColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, brandColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = product.brandOrSupermarket,
                            color = brandColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    if (product.barcode != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Barcode: ${product.barcode}",
                            color = AppTheme.colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editableName,
                    onValueChange = { editableName = it },
                    label = { Text("Product Name") },
                    placeholder = { Text("e.g. Walkers Ready Salted, Tesco Meal Deal") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primary,
                        unfocusedBorderColor = AppTheme.colors.border,
                        focusedContainerColor = AppTheme.colors.inputBackground,
                        unfocusedContainerColor = AppTheme.colors.inputBackground,
                        focusedTextColor = AppTheme.colors.textPrimary,
                        unfocusedTextColor = AppTheme.colors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Meal category pills
                Text("Log to Meal:", color = AppTheme.colors.textMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Breakfast", "Lunch", "Dinner", "Snacks", "Drinks").forEach { meal ->
                        val isSelected = selectedMeal.equals(meal, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AppTheme.colors.primary.copy(alpha = 0.20f) else AppTheme.colors.surfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) AppTheme.colors.primary else AppTheme.colors.border),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMeal = meal }
                        ) {
                            Text(
                                text = meal.take(4),
                                color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editableCalories,
                        onValueChange = { if (it.all { c -> c.isDigit() }) editableCalories = it },
                        label = { Text("Base Calories (kcal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.colors.primary,
                            unfocusedBorderColor = AppTheme.colors.border,
                            focusedContainerColor = AppTheme.colors.inputBackground,
                            unfocusedContainerColor = AppTheme.colors.inputBackground,
                            focusedTextColor = AppTheme.colors.textPrimary,
                            unfocusedTextColor = AppTheme.colors.textPrimary
                        )
                    )

                    OutlinedTextField(
                        value = editableServing,
                        onValueChange = { editableServing = it },
                        label = { Text("Base Portion") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.colors.primary,
                            unfocusedBorderColor = AppTheme.colors.border,
                            focusedContainerColor = AppTheme.colors.inputBackground,
                            unfocusedContainerColor = AppTheme.colors.inputBackground,
                            focusedTextColor = AppTheme.colors.textPrimary,
                            unfocusedTextColor = AppTheme.colors.textPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Portion Multiplier & Auto-Scale
                Text("Serving Scale / Multiplier:", color = AppTheme.colors.textMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.5f to "½ Half", 1.0f to "1x Single", 1.5f to "1.5x", 2.0f to "2x Double", 3.0f to "3x").forEach { (mult, label) ->
                        val isSelected = Math.abs(scannedMultiplier - mult) < 0.05f
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.surfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) AppTheme.colors.primary else AppTheme.colors.border),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { scannedMultiplier = mult }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else AppTheme.colors.textPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dynamic Total Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
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
                                text = "CALCULATED LOG",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textMuted
                            )
                            Text(
                                text = finalServing,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.textPrimary
                            )
                        }
                        Text(
                            text = "$scaledCalories kcal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.success
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save for future use toggle
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (saveForFuture) AppTheme.colors.primary.copy(alpha = 0.12f) else AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, if (saveForFuture) AppTheme.colors.primary.copy(alpha = 0.45f) else AppTheme.colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { saveForFuture = !saveForFuture }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveForFuture,
                            onCheckedChange = { saveForFuture = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AppTheme.colors.primary,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (saveForFuture) Color(0xFFFFB300) else AppTheme.colors.textMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Save product for future use",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = AppTheme.colors.textPrimary
                                )
                            }
                            Text(
                                "Easily re-select this product anytime without needing the barcode",
                                fontSize = 10.sp,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        onConfirm(
                            editableName,
                            finalServing,
                            scaledCalories,
                            selectedMeal,
                            product.barcode,
                            product.brandOrSupermarket,
                            saveForFuture
                        )
                    },
                    enabled = editableName.isNotBlank() && editableCalories.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.success)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm & Log $scaledCalories kcal", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

@Composable
fun EditCalorieGoalDialog(
    currentUseCustom: Boolean,
    currentCustomCalories: Int,
    calculatedBmr: Int,
    latestWeightKg: Float,
    heightCm: Float,
    waistCm: Float?,
    gender: String,
    weightUnit: WeightUnit,
    onDismiss: () -> Unit,
    onSave: (Boolean, Int) -> Unit
) {
    var useCustom by remember { mutableStateOf(currentUseCustom) }
    var calorieInput by remember { mutableStateOf(currentCustomCalories.toString()) }

    val activeCalories = if (useCustom) (calorieInput.toIntOrNull() ?: calculatedBmr) else calculatedBmr
    val projection = remember(activeCalories, latestWeightKg, heightCm, waistCm, gender, weightUnit) {
        CalorieWeightCalculator.calculateWeeklyProjection(
            dailyBudget = activeCalories,
            weightKg = latestWeightKg,
            heightCm = heightCm,
            waistCm = waistCm,
            age = 30,
            gender = gender,
            unit = weightUnit
        )
    }

    SlideUpBottomSheetDialog(onDismissRequest = onDismiss) { dismissWithAnim ->
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = "SET DAILY CALORIE GOAL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textMuted,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Mode 1: Auto BMR
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (!useCustom) AppTheme.colors.primary.copy(alpha = 0.18f) else AppTheme.colors.surfaceElevated,
                border = BorderStroke(
                    1.dp,
                    if (!useCustom) AppTheme.colors.primary else AppTheme.colors.border
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { useCustom = false }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Auto-Calculated (BMR / TDEE)",
                            color = if (!useCustom) AppTheme.colors.primary else AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            "$calculatedBmr kcal / day (Mifflin-St Jeor formula)",
                            color = AppTheme.colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                    RadioButton(
                        selected = !useCustom,
                        onClick = { useCustom = false },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = AppTheme.colors.primary,
                            unselectedColor = AppTheme.colors.textMuted
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mode 2: Custom Daily Calories
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (useCustom) AppTheme.colors.primary.copy(alpha = 0.18f) else AppTheme.colors.surfaceElevated,
                border = BorderStroke(
                    1.dp,
                    if (useCustom) AppTheme.colors.primary else AppTheme.colors.border
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { useCustom = true }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Custom Daily Target",
                                color = if (useCustom) AppTheme.colors.primary else AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                "Manually define your daily calorie ceiling",
                                color = AppTheme.colors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                        RadioButton(
                            selected = useCustom,
                            onClick = { useCustom = true },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AppTheme.colors.primary,
                                unselectedColor = AppTheme.colors.textMuted
                            )
                        )
                    }

                    if (useCustom) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = calorieInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 5) {
                                    calorieInput = input
                                }
                            },
                            label = { Text("Daily Target (kcal)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colors.primary,
                                unfocusedBorderColor = AppTheme.colors.border,
                                focusedContainerColor = AppTheme.colors.inputBackground,
                                unfocusedContainerColor = AppTheme.colors.inputBackground,
                                focusedTextColor = AppTheme.colors.textPrimary,
                                unfocusedTextColor = AppTheme.colors.textPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(1500, 1800, 2000, 2200, 2500).forEach { preset ->
                                val isSelected = calorieInput == preset.toString()
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.surface,
                                    border = BorderStroke(1.dp, if (isSelected) AppTheme.colors.primary else AppTheme.colors.border),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { calorieInput = preset.toString() }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "$preset",
                                            color = if (isSelected) Color.White else AppTheme.colors.textSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Weight Loss Projection Preview
            val bannerColor = when (projection.trajectory) {
                WeightTrajectory.WEIGHT_LOSS -> AppTheme.colors.success
                WeightTrajectory.WEIGHT_GAIN -> AppTheme.colors.warning
                WeightTrajectory.MAINTENANCE -> AppTheme.colors.primary
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = bannerColor.copy(alpha = if (AppTheme.colors.isDark) 0.15f else 0.10f),
                border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROJECTED CHANGE:",
                            color = bannerColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = projection.summaryText,
                            color = bannerColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Daily deficit: ${projection.dailyDeficit} kcal vs estimated TDEE (${projection.tdee} kcal/day).",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = dismissWithAnim) {
                    Text("Cancel", color = AppTheme.colors.textMuted)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val parsed = calorieInput.toIntOrNull() ?: 2000
                        onSave(useCustom, if (parsed > 0) parsed else 2000)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Goal", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
