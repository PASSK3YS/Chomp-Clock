package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.UserPreferences
import com.example.data.repository.WeightUnit
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.UserAvatarView

@Composable
fun SettingsScreen(
    userPrefs: UserPreferences?,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val updateCheckState by viewModel.updateCheckState.collectAsState()

    val p = userPrefs ?: UserPreferences("User", 170f, "Male", WeightUnit.KG, false, true, true)
    var editName by remember(p.username) { mutableStateOf(p.username) }
    var editHeight by remember(p.heightCm) { mutableStateOf(if (p.heightCm > 0) p.heightCm.toInt().toString() else "") }
    var customCaloriesInput by remember(p.customDailyCalories) { mutableStateOf(p.customDailyCalories.toString()) }

    var showAvatarPicker by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentAvatarId = p.avatarId,
            onDismiss = { showAvatarPicker = false },
            onAvatarSelected = { newAvatarId ->
                viewModel.updateAvatarId(newAvatarId)
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Delete All Device Data?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "This will permanently erase all your fasting history, food logs, weight records, and personalized settings from this device.\n\nThis action cannot be undone.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDeviceData {
                            showDeleteConfirmDialog = false
                            Toast.makeText(context, "All device data has been cleared.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF3F3F46))
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF18181B)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Manage profile, measurement units & updates",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF71717A)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. PROFILE & AVATAR SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "PROFILE & AVATAR",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF71717A),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box {
                        UserAvatarView(
                            avatarId = p.avatarId,
                            size = 64.dp,
                            onClick = { showAvatarPicker = true }
                        )
                        Surface(
                            onClick = { showAvatarPicker = true },
                            shape = CircleShape,
                            color = Color(0xFF3B82F6),
                            border = BorderStroke(2.dp, Color(0xFF18181B)),
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Avatar",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = p.username.ifEmpty { "User" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${p.gender} • ${p.heightCm.toInt()} cm",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA1A1AA)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap avatar to change icon or photo",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF60A5FA),
                            modifier = Modifier.clickable { showAvatarPicker = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = editName,
                    onValueChange = {
                        editName = it
                        viewModel.updateUsername(it)
                    },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF27272A),
                        focusedContainerColor = Color(0xFF27272A),
                        unfocusedContainerColor = Color(0xFF27272A)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editHeight,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() } && it.length <= 3) {
                                editHeight = it
                                it.toFloatOrNull()?.let { h -> viewModel.updateHeight(h) }
                            }
                        },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF27272A),
                            focusedContainerColor = Color(0xFF27272A),
                            unfocusedContainerColor = Color(0xFF27272A)
                        )
                    )

                    // Gender Selector
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Gender",
                            color = Color(0xFF71717A),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Male", "Female").forEach { g ->
                                val isSelected = p.gender.equals(g, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color(0xFF27272A),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF3B82F6) else Color(0xFF3F3F46)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clickable { viewModel.updateGender(g) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = g,
                                            color = if (isSelected) Color(0xFF60A5FA) else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. MEASUREMENT & WEIGHT UNITS (Stone & Pounds, Pounds, KG)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "MEASUREMENT & UNITS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF71717A),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select preferred weight unit system:",
                    color = Color(0xFFA1A1AA),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val unitOptions = listOf(
                        Triple(WeightUnit.STONE_LBS, "Stone & Pounds (st & lbs)", "British UK standard (e.g. 11 st 4 lbs)"),
                        Triple(WeightUnit.LBS, "Pounds (lbs)", "US standard (e.g. 158.0 lbs)"),
                        Triple(WeightUnit.KG, "Kilograms (kg)", "Metric standard (e.g. 71.5 kg)")
                    )

                    unitOptions.forEach { (unit, title, subtitle) ->
                        val isSelected = p.weightUnit == unit
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.18f) else Color(0xFF27272A),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) Color(0xFF3B82F6) else Color(0xFF3F3F46).copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateWeightUnit(unit)
                                    viewModel.updateUseImperial(unit != WeightUnit.KG)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = if (isSelected) Color(0xFF60A5FA) else Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = subtitle,
                                        color = Color(0xFF71717A),
                                        fontSize = 12.sp
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateWeightUnit(unit)
                                        viewModel.updateUseImperial(unit != WeightUnit.KG)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF3B82F6),
                                        unselectedColor = Color(0xFF71717A)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. DAILY CALORIE INTAKE & BUDGET
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "DAILY CALORIE BUDGET",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF71717A),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Configure how your daily calorie goal is determined:",
                    color = Color(0xFFA1A1AA),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Option 1: Automatic BMR/TDEE
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!p.useCustomCalories) Color(0xFF3B82F6).copy(alpha = 0.18f) else Color(0xFF27272A),
                    border = BorderStroke(
                        1.5.dp,
                        if (!p.useCustomCalories) Color(0xFF3B82F6) else Color(0xFF3F3F46).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateUseCustomCalories(false) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Calculate (BMR / TDEE)",
                                color = if (!p.useCustomCalories) Color(0xFF60A5FA) else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Uses Mifflin-St Jeor formula based on weight, height, and gender",
                                color = Color(0xFF71717A),
                                fontSize = 12.sp
                            )
                        }
                        RadioButton(
                            selected = !p.useCustomCalories,
                            onClick = { viewModel.updateUseCustomCalories(false) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF3B82F6),
                                unselectedColor = Color(0xFF71717A)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Custom Daily Calories
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (p.useCustomCalories) Color(0xFF3B82F6).copy(alpha = 0.18f) else Color(0xFF27272A),
                    border = BorderStroke(
                        1.5.dp,
                        if (p.useCustomCalories) Color(0xFF3B82F6) else Color(0xFF3F3F46).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateUseCustomCalories(true) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Daily Calorie Target",
                                    color = if (p.useCustomCalories) Color(0xFF60A5FA) else Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Set your own fixed target (e.g. 1500 - 3000 kcal/day)",
                                    color = Color(0xFF71717A),
                                    fontSize = 12.sp
                                )
                            }
                            RadioButton(
                                selected = p.useCustomCalories,
                                onClick = { viewModel.updateUseCustomCalories(true) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF3B82F6),
                                    unselectedColor = Color(0xFF71717A)
                                )
                            )
                        }

                        if (p.useCustomCalories) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = customCaloriesInput,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() } && input.length <= 5) {
                                        customCaloriesInput = input
                                        input.toIntOrNull()?.let { cal ->
                                            if (cal > 0) {
                                                viewModel.updateCustomDailyCalories(cal)
                                            }
                                        }
                                    }
                                },
                                label = { Text("Daily Calorie Goal (kcal)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color(0xFF3F3F46),
                                    focusedContainerColor = Color(0xFF27272A),
                                    unfocusedContainerColor = Color(0xFF27272A)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Quick Targets:", color = Color(0xFF71717A), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1500, 1800, 2000, 2200, 2500).forEach { preset ->
                                    val isCurrent = p.customDailyCalories == preset
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) Color(0xFF3B82F6) else Color(0xFF27272A),
                                        border = BorderStroke(1.dp, if (isCurrent) Color(0xFF60A5FA) else Color(0xFF3F3F46)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                customCaloriesInput = preset.toString()
                                                viewModel.updateCustomDailyCalories(preset)
                                            }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "$preset",
                                                color = if (isCurrent) Color.White else Color(0xFFA1A1AA),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
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

        Spacer(modifier = Modifier.height(20.dp))

        // 4. APP PREFERENCES
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "PREFERENCES & ALERTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF71717A),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Theme (Sophisticated Dark)", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("High contrast AMOLED dark styling", color = Color(0xFF71717A), fontSize = 12.sp)
                    }
                    Switch(
                        checked = p.useDarkTheme,
                        onCheckedChange = { viewModel.updateUseDarkTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3B82F6)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFF27272A), modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Audible Fasting Alerts", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Sound chime upon goal completion", color = Color(0xFF71717A), fontSize = 12.sp)
                    }
                    Switch(
                        checked = p.soundsEnabled,
                        onCheckedChange = { viewModel.updateSoundsEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3B82F6)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. GITHUB RELEASES & APP UPDATE CHECKER (Connected to https://github.com/PASSK3YS/Chomp-Clock/releases)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GITHUB RELEASES & UPDATES",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF71717A),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(
                            text = "PASSK3YS/Chomp-Clock",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Checks directly against GitHub repository releases for new versions and APK downloads.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Check Button
                Button(
                    onClick = { viewModel.checkForUpdates() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF27272A),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFF3F3F46))
                ) {
                    if (updateCheckState is UpdateCheckState.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF60A5FA),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Checking GitHub releases...", fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF60A5FA))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check for Updates", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Status Results
                when (val state = updateCheckState) {
                    is UpdateCheckState.UpdateAvailable -> {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F291E),
                            border = BorderStroke(1.dp, Color(0xFF059669)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "NEW UPDATE AVAILABLE 🎉",
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF059669)
                                    ) {
                                        Text(
                                            text = state.latestVersion,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.releaseName,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.releaseNotes.take(200) + if (state.releaseNotes.length > 200) "..." else "",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val url = state.downloadUrl ?: state.htmlUrl
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Download APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.htmlUrl))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFF059669)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("View Release", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    is UpdateCheckState.UpToDate -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF064E3B).copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "You're on the latest version (${state.currentVersion})",
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Latest verified GitHub release: ${state.releaseName}",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    is UpdateCheckState.NoReleasesFound -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Connected to PASSK3YS/Chomp-Clock",
                                    color = Color(0xFF60A5FA),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "No releases published yet on GitHub. When you tag a release (e.g. v1.1.0), GitHub Actions will automatically generate the .APK file.",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Open Releases Page →",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.repoUrl))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    }

                    is UpdateCheckState.Error -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF27272A),
                            border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Unable to check GitHub Releases: ${state.errorMessage}",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Visit GitHub Releases Manually →",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.repoUrl))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    }

                    UpdateCheckState.Checking -> {
                        // Progress indicator shown in button
                    }

                    UpdateCheckState.Idle -> {
                        // Idle state
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Release notes collapsible
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWhatsNewDialog = !showWhatsNewDialog }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("What's new in this build", color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Icon(
                        imageVector = if (showWhatsNewDialog) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = showWhatsNewDialog) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(Color(0xFF27272A).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text("• UK Supermarket Food Database (Tesco, Sainsbury's, ASDA, M&S, Morrisons, Aldi, Lidl)", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                        Text("• UK Barcode scanning with instant product nutrition autofill", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                        Text("• GitHub Releases Integration with automated APK build workflow", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                        Text("• Stone & Pounds (st & lbs), Pounds, and KG unit switcher", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                        Text("• Custom fasting range menu with hour and minute pickers", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                        Text("• Persistent 5-category daily meal board (Breakfast, Lunch, Dinner, Snacks, Drinks)", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. DANGER ZONE (With Confirmation Prompt)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "DANGER ZONE",
                    color = Color(0xFFFCA5A5),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Permanently clear your local database and reset fasting/meal statistics.",
                    color = Color(0xFFFCA5A5).copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete All Device Data", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Chomp Clock - v1.0.0",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFA1A1AA),
            textAlign = TextAlign.Center
        )
        Text(
            text = "GitHub PASSK3YS/Chomp-Clock",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF71717A),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
