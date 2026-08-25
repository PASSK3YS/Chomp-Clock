package com.example.ui.weight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.WeightEntry
import com.example.data.repository.UserPreferences
import com.example.data.repository.WeightUnit
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.UserAvatarView
import com.example.ui.settings.SettingsViewModel
import com.example.util.WeightUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeightScreen(
    userPrefs: UserPreferences?,
    viewModel: WeightViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val entries by viewModel.weightEntries.collectAsState()
    
    var weightInput by remember { mutableStateOf("") }
    var stoneInput by remember { mutableStateOf("") }
    var lbsInput by remember { mutableStateOf("") }
    var waistInput by remember { mutableStateOf("") }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var entryToDelete by remember { mutableStateOf<WeightEntry?>(null) }

    val username = userPrefs?.username ?: "User"
    val avatarId = userPrefs?.avatarId
    val heightCm = userPrefs?.heightCm ?: 170f
    val gender = userPrefs?.gender ?: "Male"
    val weightUnit = userPrefs?.weightUnit ?: WeightUnit.KG
    val useImperial = userPrefs?.useImperial ?: (weightUnit != WeightUnit.KG)

    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    val latestWeight = entries.firstOrNull()?.weightKg ?: 70f
    val bmi = viewModel.calculateBmi(latestWeight, heightCm)
    val dailyCalories = viewModel.calculateDailyCalories(latestWeight, heightCm, 30, gender)

    val bmiCategory = when {
        bmi < 18.5f -> "Underweight" to Color(0xFF60A5FA)
        bmi < 25.0f -> "Normal Weight" to Color(0xFF34D399)
        bmi < 30.0f -> "Overweight" to Color(0xFFFBBF24)
        else -> "Obese" to Color(0xFFF87171)
    }

    // Date Picker launcher
    val openDatePicker = {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        val dialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    // Keep current hour/minute
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                }
                selectedDateMillis = pickedCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        // Disallow future dates
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }

    if (entryToDelete != null) {
        val entry = entryToDelete!!
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(entry.date))
        val weightStr = WeightUtils.formatWeight(entry.weightKg, weightUnit)
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Weight Log", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete the weigh-in of $weightStr recorded for $dateStr? This action cannot be undone.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWeightEntry(entry)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel", color = Color(0xFFA1A1AA))
                }
            },
            containerColor = Color(0xFF18181B),
            shape = RoundedCornerShape(16.dp)
        )
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
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF18181B),
                border = BorderStroke(1.dp, Color(0xFF27272A))
            ) {
                Text(
                    text = weightUnit.shortName.uppercase(),
                    color = Color(0xFF60A5FA),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Current Metrics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
            border = BorderStroke(1.dp, Color(0xFF27272A)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "CURRENT METRICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF71717A),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = WeightUtils.formatWeight(latestWeight, weightUnit),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Latest Recorded Weight",
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = bmiCategory.second.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, bmiCategory.second.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "BMI ${String.format(Locale.getDefault(), "%.1f", bmi)}",
                                color = bmiCategory.second,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = bmiCategory.first,
                                color = bmiCategory.second,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF27272A))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Target Daily Calories",
                        color = Color(0xFFA1A1AA),
                        fontSize = 13.sp
                    )
                    Text(
                        "$dailyCalories kcal",
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Log New Weight Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "LOG WEIGHT",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF71717A),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            
            // Custom Date Selector Chip
            val isToday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(selectedDateMillis)) ==
                    SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val dateLabel = if (isToday) "Today" else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isToday) Color(0xFF27272A) else Color(0xFF3B82F6).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, if (isToday) Color(0xFF3F3F46) else Color(0xFF3B82F6)),
                modifier = Modifier.clickable { openDatePicker() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "Pick Date",
                        tint = if (isToday) Color(0xFFA1A1AA) else Color(0xFF60A5FA),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateLabel,
                        color = if (isToday) Color(0xFFD4D4D8) else Color(0xFF60A5FA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (weightUnit == WeightUnit.STONE_LBS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = stoneInput,
                    onValueChange = { stoneInput = it },
                    label = { Text("Stone (st)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF18181B),
                        focusedContainerColor = Color(0xFF18181B),
                        unfocusedBorderColor = Color(0xFF27272A),
                        focusedBorderColor = Color(0xFF3B82F6)
                    )
                )
                OutlinedTextField(
                    value = lbsInput,
                    onValueChange = { lbsInput = it },
                    label = { Text("Pounds (lbs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF18181B),
                        focusedContainerColor = Color(0xFF18181B),
                        unfocusedBorderColor = Color(0xFF27272A),
                        focusedBorderColor = Color(0xFF3B82F6)
                    )
                )
                OutlinedTextField(
                    value = waistInput,
                    onValueChange = { waistInput = it },
                    label = { Text(if (useImperial) "Waist (in)" else "Waist (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF18181B),
                        focusedContainerColor = Color(0xFF18181B),
                        unfocusedBorderColor = Color(0xFF27272A),
                        focusedBorderColor = Color(0xFF3B82F6)
                    )
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text(if (weightUnit == WeightUnit.LBS) "Weight (lbs)" else "Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1.2f),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF18181B),
                        focusedContainerColor = Color(0xFF18181B),
                        unfocusedBorderColor = Color(0xFF27272A),
                        focusedBorderColor = Color(0xFF3B82F6)
                    )
                )
                OutlinedTextField(
                    value = waistInput,
                    onValueChange = { waistInput = it },
                    label = { Text(if (useImperial) "Waist (in)" else "Waist (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF18181B),
                        focusedContainerColor = Color(0xFF18181B),
                        unfocusedBorderColor = Color(0xFF27272A),
                        focusedBorderColor = Color(0xFF3B82F6)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val parsedKg = if (weightUnit == WeightUnit.STONE_LBS) {
                    WeightUtils.parseToKg(stoneInput, lbsInput, WeightUnit.STONE_LBS)
                } else {
                    WeightUtils.parseToKg(weightInput, "", weightUnit)
                }

                if (parsedKg != null && parsedKg > 0f) {
                    val rawWaist = waistInput.toFloatOrNull()
                    val waistCm = if (useImperial && rawWaist != null) rawWaist * 2.54f else rawWaist
                    viewModel.addWeightEntry(parsedKg, waistCm, selectedDateMillis)
                    weightInput = ""
                    stoneInput = ""
                    lbsInput = ""
                    waistInput = ""
                    // Reset to today
                    selectedDateMillis = System.currentTimeMillis()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF4F4F5),
                contentColor = Color.Black
            )
        ) {
            val isToday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(selectedDateMillis)) ==
                    SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val btnLabel = if (isToday) "Save Today's Weight" else "Save Weight for ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))}"
            Text(btnLabel, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Past Logs List
        Text(
            "PAST WEIGHT LOGS",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF71717A),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (entries.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF18181B),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF27272A))
                    ) {
                        Text(
                            "No weight logs recorded yet. Add your current or past weight above!",
                            color = Color(0xFF71717A),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(entries) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                        border = BorderStroke(1.dp, Color(0xFF27272A)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 8.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                Text(
                                    text = dateFormat.format(Date(entry.date)),
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                if (entry.waistCm != null && entry.waistCm > 0f) {
                                    val waistStr = if (useImperial) {
                                        String.format(Locale.getDefault(), "Waist: %.1f in", entry.waistCm / 2.54f)
                                    } else {
                                        String.format(Locale.getDefault(), "Waist: %.1f cm", entry.waistCm)
                                    }
                                    Text(
                                        text = waistStr,
                                        color = Color(0xFF71717A),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = WeightUtils.formatWeight(entry.weightKg, weightUnit),
                                    color = Color(0xFF60A5FA),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { entryToDelete = entry },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete weight log",
                                        tint = Color(0xFF71717A),
                                        modifier = Modifier.size(18.dp)
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
