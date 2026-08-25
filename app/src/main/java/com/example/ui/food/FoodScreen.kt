package com.example.ui.food

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.FoodEntry
import com.example.data.repository.UserPreferences
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.BarcodeScannerScreen
import com.example.ui.components.UserAvatarView
import com.example.ui.settings.SettingsViewModel
import com.example.ui.weight.WeightViewModel
import java.text.SimpleDateFormat
import java.util.*

data class MealCategory(
    val name: String,
    val icon: String,
    val subtitle: String
)

val ALL_MEAL_CATEGORIES = listOf(
    MealCategory("Breakfast", "🍳", "Morning meal"),
    MealCategory("Lunch", "🥗", "Midday meal"),
    MealCategory("Dinner", "🍲", "Evening meal"),
    MealCategory("Snacks", "🥨", "Bites & snacks"),
    MealCategory("Drinks", "💧", "Hydration & beverages")
)

@Composable
fun FoodScreen(
    userPrefs: UserPreferences?,
    viewModel: FoodViewModel = viewModel(),
    weightViewModel: WeightViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val entries by viewModel.foodEntries.collectAsState()
    val weightEntries by weightViewModel.weightEntries.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedMealForAdd by remember { mutableStateOf("Breakfast") }
    var showScanner by remember { mutableStateOf(false) }
    var selectedMealForScan by remember { mutableStateOf("Breakfast") }
    var showAvatarPicker by remember { mutableStateOf(false) }

    val username = userPrefs?.username ?: "User"
    val avatarId = userPrefs?.avatarId
    val gender = userPrefs?.gender ?: "Male"
    val heightCm = userPrefs?.heightCm ?: 170f
    val latestWeight = weightEntries.firstOrNull()?.weightKg ?: 70f
    val targetDailyCalories = weightViewModel.calculateDailyCalories(latestWeight, heightCm, 30, gender)

    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    if (showScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = { barcode ->
                showScanner = false
                viewModel.scanBarcode(barcode, selectedMealForScan)
            },
            onClose = { showScanner = false }
        )
        return
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

    if (showAddDialog) {
        AddFoodDialog(
            initialMealType = selectedMealForAdd,
            onDismiss = { showAddDialog = false },
            onSave = { name, serving, calories, mealType ->
                viewModel.addFoodEntry(name, serving, calories, mealType)
                showAddDialog = false
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
                        color = Color(0xFFA1A1AA),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$greeting, $username",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Quick general add food button
            Surface(
                onClick = {
                    selectedMealForAdd = "Breakfast"
                    showAddDialog = true
                },
                shape = CircleShape,
                color = Color(0xFF3B82F6),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Add Food", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Calorie Target Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF18181B),
            border = BorderStroke(1.dp, Color(0xFF27272A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "DAILY CALORIE BUDGET",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF71717A),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "$totalCaloriesConsumed",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            Text(
                                " / $targetDailyCalories kcal",
                                color = Color(0xFFA1A1AA),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "REMAINING",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF71717A),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "$remainingCalories kcal",
                            color = if (totalCaloriesConsumed > targetDailyCalories) Color(0xFFF87171) else Color(0xFF34D399),
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
                    color = if (totalCaloriesConsumed > targetDailyCalories) Color(0xFFEF4444) else Color(0xFF34D399),
                    trackColor = Color(0xFF27272A),
                    strokeCap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "TODAY'S MEALS",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF71717A),
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Header of the meal card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF27272A),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(mealCat.icon, fontSize = 18.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = mealCat.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = if (mealItems.isEmpty()) "0 kcal" else "$mealTotalCalories kcal",
                                        color = if (mealItems.isNotEmpty()) Color(0xFF34D399) else Color(0xFF71717A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Quick buttons: Barcode scan & Add
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        selectedMealForScan = mealCat.name
                                        showScanner = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan for ${mealCat.name}",
                                        tint = Color(0xFFA1A1AA),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        selectedMealForAdd = mealCat.name
                                        showAddDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add to ${mealCat.name}",
                                        tint = Color(0xFF60A5FA),
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
                                        showAddDialog = true
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF141416),
                                border = BorderStroke(1.dp, Color(0xFF202024))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "No items logged yet",
                                        color = Color(0xFF52525B),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "+ Log ${mealCat.name}",
                                        color = Color(0xFF60A5FA),
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
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF27272A).copy(alpha = 0.5f),
        border = BorderStroke(1.dp, Color(0xFF3F3F46).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = item.servingSize,
                    color = Color(0xFFA1A1AA),
                    fontSize = 11.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${item.calories} kcal",
                    color = Color(0xFF34D399),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = Color(0xFF71717A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddFoodDialog(
    initialMealType: String,
    onDismiss: () -> Unit,
    onSave: (name: String, serving: String, calories: Int, mealType: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var serving by remember { mutableStateOf("1 serving") }
    var calories by remember { mutableStateOf("") }
    var selectedMeal by remember { mutableStateOf(initialMealType) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF18181B),
            border = BorderStroke(1.dp, Color(0xFF27272A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ADD FOOD ENTRY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA1A1AA),
                        letterSpacing = 1.2.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA1A1AA))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Meal category selector pills
                Text("Select Meal", color = Color(0xFF71717A), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Breakfast", "Lunch", "Dinner", "Snacks", "Drinks").forEach { meal ->
                        val isSelected = selectedMeal.equals(meal, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color(0xFF27272A),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF3B82F6) else Color(0xFF3F3F46)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMeal = meal }
                        ) {
                            Text(
                                text = meal.take(4),
                                color = if (isSelected) Color(0xFF60A5FA) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food / Beverage Name") },
                    placeholder = { Text("e.g. Oatmeal with blueberries") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF27272A),
                        focusedContainerColor = Color(0xFF27272A),
                        unfocusedContainerColor = Color(0xFF27272A)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { if (it.all { char -> char.isDigit() }) calories = it },
                        label = { Text("Calories (kcal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF27272A),
                            focusedContainerColor = Color(0xFF27272A),
                            unfocusedContainerColor = Color(0xFF27272A)
                        )
                    )

                    OutlinedTextField(
                        value = serving,
                        onValueChange = { serving = it },
                        label = { Text("Portion / Serving") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF27272A),
                            focusedContainerColor = Color(0xFF27272A),
                            unfocusedContainerColor = Color(0xFF27272A)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val cal = calories.toIntOrNull() ?: 0
                        onSave(name, serving, cal, selectedMeal)
                    },
                    enabled = name.isNotBlank() && calories.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Add to $selectedMeal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
