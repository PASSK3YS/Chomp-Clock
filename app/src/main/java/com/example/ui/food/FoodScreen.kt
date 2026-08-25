package com.example.ui.food

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.data.repository.FoodSearchResult
import com.example.data.repository.HeightUnit
import com.example.data.repository.UserPreferences
import com.example.data.repository.WeightUnit
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.BarcodeScannerScreen
import com.example.ui.components.UserAvatarView
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.AppTheme
import com.example.ui.weight.WeightViewModel
import com.example.util.CalorieWeightCalculator
import com.example.util.WeightTrajectory
import com.example.util.WeightUtils
import java.text.SimpleDateFormat
import java.util.*

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
            onConfirm = { name, serving, calories, mealType, barcode ->
                viewModel.addFoodEntry(name, serving, calories, mealType, barcode)
                scannedProductToConfirm = null
                viewModel.clearBarcodeState()
                Toast.makeText(context, "Logged: $name ($calories kcal)", Toast.LENGTH_SHORT).show()
            }
        )
    } else if (scannedFallbackBarcode != null) {
        val code = scannedFallbackBarcode!!
        LogScannedProductDialog(
            product = FoodSearchResult(
                id = code,
                name = "",
                brandOrSupermarket = "Unlisted UK Item",
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
            onConfirm = { name, serving, calories, mealType, barcode ->
                viewModel.addFoodEntry(name, serving, calories, mealType, barcode)
                scannedFallbackBarcode = null
                viewModel.clearBarcodeState()
                Toast.makeText(context, "Logged: $name ($calories kcal)", Toast.LENGTH_SHORT).show()
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
            onSave = { name, serving, calories, mealType, barcode ->
                viewModel.addFoodEntry(name, serving, calories, mealType, barcode)
                showFoodSearchDialog = false
                Toast.makeText(context, "Logged $name ($calories kcal)", Toast.LENGTH_SHORT).show()
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
    onSave: (name: String, serving: String, calories: Int, mealType: String, barcode: String?) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSupermarket by viewModel.selectedSupermarket.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    var selectedMeal by remember { mutableStateOf(initialMealType) }
    var activeTab by remember { mutableStateOf(0) }

    var selectedItemForPortion by remember { mutableStateOf<FoodSearchResult?>(null) }
    var portionMultiplier by remember { mutableStateOf(1.0f) }

    var customName by remember { mutableStateOf("") }
    var customServing by remember { mutableStateOf("1 serving") }
    var customCalories by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AppTheme.colors.surface,
            border = BorderStroke(1.dp, AppTheme.colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                            "UK Supermarket Database & Barcodes",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.primary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppTheme.colors.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Meal category selector pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Switcher (UK Database vs Custom Entry)
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
                        text = { Text("UK Supermarkets", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Custom Food", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeTab == 0) {
                    // Search Bar with Barcode Scanner Icon
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

                    Spacer(modifier = Modifier.height(10.dp))

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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Portion Selector Overlay if an item is selected
                    if (selectedItemForPortion != null) {
                        val item = selectedItemForPortion!!
                        val calculatedCalories = (item.caloriesPerServing * portionMultiplier).toInt()
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = AppTheme.colors.surfaceElevated,
                            border = BorderStroke(1.5.dp, AppTheme.colors.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
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
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${item.brandOrSupermarket} • ${item.servingSize}",
                                            color = AppTheme.colors.textSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { selectedItemForPortion = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Deselect", tint = AppTheme.colors.textMuted)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

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
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$calculatedCalories kcal",
                                        color = AppTheme.colors.success,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Button(
                                        onClick = {
                                            val servingDesc = if (portionMultiplier == 1.0f) item.servingSize else "${portionMultiplier}x ${item.servingSize}"
                                            onSave(item.name, servingDesc, calculatedCalories, selectedMeal, item.barcode)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Log to $selectedMeal", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
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
                                    "Try switching to 'Custom Food' tab to add manually",
                                    color = AppTheme.colors.primary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { activeTab = 1 }
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
                } else {
                    // Manual Custom Entry
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Food / Beverage Name") },
                            placeholder = { Text("e.g. Homemade Chicken Stew") },
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customCalories,
                                onValueChange = { if (it.all { c -> c.isDigit() }) customCalories = it },
                                label = { Text("Calories (kcal)") },
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
                                value = customServing,
                                onValueChange = { customServing = it },
                                label = { Text("Portion / Serving") },
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

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                val cal = customCalories.toIntOrNull() ?: 0
                                onSave(customName, customServing, cal, selectedMeal, null)
                            },
                            enabled = customName.isNotBlank() && customCalories.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary)
                        ) {
                            Text("Add Custom Food to $selectedMeal", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
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
    onConfirm: (name: String, serving: String, calories: Int, mealType: String, barcode: String?) -> Unit
) {
    var editableName by remember { mutableStateOf(product.name) }
    var editableServing by remember { mutableStateOf(product.servingSize) }
    var editableCalories by remember { mutableStateOf(product.caloriesPerServing.toString()) }
    var selectedMeal by remember { mutableStateOf(initialMealType) }

    val brandColor = getSupermarketBrandColor(product.brandOrSupermarket)
    val isFound = product.name.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = AppTheme.colors.surface,
            border = BorderStroke(1.dp, AppTheme.colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
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
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
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
                        label = { Text("Calories (kcal)") },
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
                        label = { Text("Portion") },
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

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val baseCal = editableCalories.toIntOrNull() ?: 0
                        onConfirm(editableName, editableServing, baseCal, selectedMeal, product.barcode)
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
                    Text("Confirm & Log Scanned Food", color = Color.White, fontWeight = FontWeight.Bold)
                }
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AppTheme.colors.surface,
            border = BorderStroke(1.dp, AppTheme.colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
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
                    TextButton(onClick = onDismiss) {
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
}
