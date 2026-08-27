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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Event
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
import com.example.data.repository.HeightUnit
import com.example.data.repository.UserPreferences
import com.example.data.repository.WeighInFrequency
import com.example.data.repository.WeightUnit
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.UserAvatarView
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.AppTheme
import com.example.util.CalorieWeightCalculator
import com.example.util.WeightReminderManager
import com.example.util.WeightTrajectory
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
    var showReminderDialog by remember { mutableStateOf(false) }

    val username = userPrefs?.username ?: "User"
    val avatarId = userPrefs?.avatarId
    val heightCm = userPrefs?.heightCm ?: 170f
    val gender = userPrefs?.gender ?: "Male"
    val weightUnit = userPrefs?.weightUnit ?: WeightUnit.KG
    val heightUnit = userPrefs?.heightUnit ?: HeightUnit.CM
    val useImperial = (heightUnit == HeightUnit.FT_IN || weightUnit != WeightUnit.KG)

    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    val latestWeight = entries.firstOrNull()?.weightKg ?: 70f
    val latestWaist = entries.firstOrNull()?.waistCm ?: userPrefs?.waistCm
    val bmi = viewModel.calculateBmi(latestWeight, heightCm)
    val dailyTdee = viewModel.calculateDailyCalories(latestWeight, heightCm, 30, gender, latestWaist)

    val calorieBudget = if (userPrefs?.useCustomCalories == true && userPrefs.customDailyCalories > 0) {
        userPrefs.customDailyCalories
    } else {
        dailyTdee
    }

    val weeklyProjection = remember(calorieBudget, latestWeight, heightCm, latestWaist, gender, weightUnit) {
        CalorieWeightCalculator.calculateWeeklyProjection(
            dailyBudget = calorieBudget,
            weightKg = latestWeight,
            heightCm = heightCm,
            waistCm = latestWaist,
            age = 30,
            gender = gender,
            unit = weightUnit
        )
    }

    val bmiCategory = when {
        bmi < 18.5f -> "Underweight" to AppTheme.colors.primary
        bmi < 25.0f -> "Normal Weight" to AppTheme.colors.success
        bmi < 30.0f -> "Overweight" to AppTheme.colors.warning
        else -> "Obese" to AppTheme.colors.danger
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
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                }
                selectedDateMillis = pickedCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
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
                        viewModel.deleteWeightEntry(entry)
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

    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentAvatarId = avatarId,
            onDismiss = { showAvatarPicker = false },
            onAvatarSelected = { newAvatar ->
                settingsViewModel.updateAvatarId(newAvatar)
            }
        )
    }

    if (showReminderDialog) {
        WeighInReminderDialog(
            userPrefs = userPrefs,
            onDismiss = { showReminderDialog = false },
            onSaveReminder = { enabled, frequency, dayOfWeek, hour, minute ->
                viewModel.updateWeighInReminder(
                    enabled = enabled,
                    frequency = frequency,
                    dayOfWeek = dayOfWeek,
                    hour = hour,
                    minute = minute
                )
            },
            onSendTestNotification = {
                viewModel.sendTestReminder()
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
                    text = weightUnit.shortName.uppercase(),
                    color = AppTheme.colors.primary,
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
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
            border = BorderStroke(1.dp, AppTheme.colors.border),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CURRENT METRICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        "Height: ${WeightUtils.formatHeight(heightCm, heightUnit)}",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
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
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = "Latest Recorded Weight",
                            color = AppTheme.colors.textSecondary,
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
                HorizontalDivider(color = AppTheme.colors.border)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Estimated Maintenance (TDEE)",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        "$dailyTdee kcal / day",
                        color = AppTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Weekly Weight Loss Projection Card
                val projColor = when (weeklyProjection.trajectory) {
                    WeightTrajectory.WEIGHT_LOSS -> AppTheme.colors.success
                    WeightTrajectory.WEIGHT_GAIN -> AppTheme.colors.warning
                    WeightTrajectory.MAINTENANCE -> AppTheme.colors.primary
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = projColor.copy(alpha = if (AppTheme.colors.isDark) 0.15f else 0.10f),
                    border = BorderStroke(1.dp, projColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (weeklyProjection.trajectory == WeightTrajectory.WEIGHT_LOSS) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = projColor,
                                modifier = Modifier.size(16.dp)
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
                            text = "Budget: $calorieBudget kcal",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weigh-In Reminder Banner Card
        val isReminderOn = userPrefs?.weighInReminderEnabled ?: false
        val reminderFrequency = userPrefs?.weighInFrequency ?: WeighInFrequency.WEEKLY
        val reminderDay = userPrefs?.weighInDayOfWeek ?: Calendar.MONDAY
        val reminderHour = userPrefs?.weighInHour ?: 8
        val reminderMinute = userPrefs?.weighInMinute ?: 0

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showReminderDialog = true },
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
            border = BorderStroke(
                1.dp,
                if (isReminderOn) AppTheme.colors.primary.copy(alpha = 0.4f) else AppTheme.colors.border
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            shape = RoundedCornerShape(10.dp),
                            color = if (isReminderOn) AppTheme.colors.primary.copy(alpha = 0.15f) else AppTheme.colors.surfaceElevated,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isReminderOn) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = if (isReminderOn) AppTheme.colors.primary else AppTheme.colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "WEIGH-IN REMINDER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isReminderOn) AppTheme.colors.primary else AppTheme.colors.textMuted,
                                    letterSpacing = 1.1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isReminderOn) AppTheme.colors.primary.copy(alpha = 0.2f) else AppTheme.colors.border.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = if (isReminderOn) "ACTIVE" else "OFF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isReminderOn) AppTheme.colors.primary else AppTheme.colors.textMuted,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            val timeCal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, reminderHour)
                                set(Calendar.MINUTE, reminderMinute)
                            }
                            val formattedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(timeCal.time)
                            val scheduleSummary = if (isReminderOn) {
                                if (reminderFrequency == WeighInFrequency.DAILY) {
                                    "Daily at $formattedTime"
                                } else {
                                    "${reminderFrequency.displayName} on ${WeightReminderManager.getDayOfWeekDisplayName(reminderDay, short = true)} at $formattedTime"
                                }
                            } else {
                                "Set weekly or bi-weekly reminder"
                            }
                            Text(
                                text = scheduleSummary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.textPrimary
                            )
                        }
                    }

                    Switch(
                        checked = isReminderOn,
                        onCheckedChange = { checked ->
                            viewModel.setWeighInReminderEnabled(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppTheme.colors.primary,
                            uncheckedThumbColor = AppTheme.colors.textMuted,
                            uncheckedTrackColor = AppTheme.colors.border
                        )
                    )
                }

                if (isReminderOn) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AppTheme.colors.surfaceElevated,
                        border = BorderStroke(1.dp, AppTheme.colors.border.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Event,
                                    contentDescription = null,
                                    tint = AppTheme.colors.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val nextPreview = WeightReminderManager.formatNextReminderPreview(
                                    frequency = reminderFrequency,
                                    dayOfWeek = reminderDay,
                                    hour = reminderHour,
                                    minute = reminderMinute
                                )
                                Text(
                                    text = "Next: $nextPreview",
                                    fontSize = 11.sp,
                                    color = AppTheme.colors.textSecondary,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showReminderDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Edit",
                                    tint = AppTheme.colors.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Edit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.primary
                                )
                            }
                        }
                    }
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
                color = AppTheme.colors.textMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            
            // Custom Date Selector Chip
            val isToday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(selectedDateMillis)) ==
                    SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val dateLabel = if (isToday) "Today" else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isToday) AppTheme.colors.surfaceElevated else AppTheme.colors.primary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, if (isToday) AppTheme.colors.border else AppTheme.colors.primary),
                modifier = Modifier.clickable { openDatePicker() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "Pick Date",
                        tint = if (isToday) AppTheme.colors.textSecondary else AppTheme.colors.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateLabel,
                        color = if (isToday) AppTheme.colors.textPrimary else AppTheme.colors.primary,
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
                        unfocusedContainerColor = AppTheme.colors.inputBackground,
                        focusedContainerColor = AppTheme.colors.inputBackground,
                        unfocusedBorderColor = AppTheme.colors.border,
                        focusedBorderColor = AppTheme.colors.primary,
                        focusedTextColor = AppTheme.colors.textPrimary,
                        unfocusedTextColor = AppTheme.colors.textPrimary
                    )
                )
                OutlinedTextField(
                    value = lbsInput,
                    onValueChange = { lbsInput = it },
                    label = { Text("Pounds (lbs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = AppTheme.colors.inputBackground,
                        focusedContainerColor = AppTheme.colors.inputBackground,
                        unfocusedBorderColor = AppTheme.colors.border,
                        focusedBorderColor = AppTheme.colors.primary,
                        focusedTextColor = AppTheme.colors.textPrimary,
                        unfocusedTextColor = AppTheme.colors.textPrimary
                    )
                )
                OutlinedTextField(
                    value = waistInput,
                    onValueChange = { waistInput = it },
                    label = { Text(if (useImperial) "Waist (in)" else "Waist (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = AppTheme.colors.inputBackground,
                        focusedContainerColor = AppTheme.colors.inputBackground,
                        unfocusedBorderColor = AppTheme.colors.border,
                        focusedBorderColor = AppTheme.colors.primary,
                        focusedTextColor = AppTheme.colors.textPrimary,
                        unfocusedTextColor = AppTheme.colors.textPrimary
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
                        unfocusedContainerColor = AppTheme.colors.inputBackground,
                        focusedContainerColor = AppTheme.colors.inputBackground,
                        unfocusedBorderColor = AppTheme.colors.border,
                        focusedBorderColor = AppTheme.colors.primary,
                        focusedTextColor = AppTheme.colors.textPrimary,
                        unfocusedTextColor = AppTheme.colors.textPrimary
                    )
                )
                OutlinedTextField(
                    value = waistInput,
                    onValueChange = { waistInput = it },
                    label = { Text(if (useImperial) "Waist (in)" else "Waist (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = AppTheme.colors.inputBackground,
                        focusedContainerColor = AppTheme.colors.inputBackground,
                        unfocusedBorderColor = AppTheme.colors.border,
                        focusedBorderColor = AppTheme.colors.primary,
                        focusedTextColor = AppTheme.colors.textPrimary,
                        unfocusedTextColor = AppTheme.colors.textPrimary
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
                    selectedDateMillis = System.currentTimeMillis()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.primary,
                contentColor = Color.White
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
            color = AppTheme.colors.textMuted,
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
                        color = AppTheme.colors.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.border)
                    ) {
                        Text(
                            "No weight logs recorded yet. Add your current or past weight above!",
                            color = AppTheme.colors.textMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(entries) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                        border = BorderStroke(1.dp, AppTheme.colors.border),
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
                                    color = AppTheme.colors.textPrimary,
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
                                        color = AppTheme.colors.textMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = WeightUtils.formatWeight(entry.weightKg, weightUnit),
                                    color = AppTheme.colors.primary,
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
                                        tint = AppTheme.colors.textMuted,
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
