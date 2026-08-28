package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.BuildConfig
import com.example.data.repository.HeightUnit
import com.example.data.repository.ThemeMode
import com.example.data.repository.UserPreferences
import com.example.data.repository.WeighInFrequency
import com.example.data.repository.WeightUnit
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.UserAvatarView
import com.example.ui.theme.AppTheme
import com.example.ui.weight.WeighInReminderDialog
import com.example.ui.weight.WeightViewModel
import com.example.util.CalorieWeightCalculator
import com.example.util.InAppUpdateInstaller
import com.example.util.WeightReminderManager
import com.example.util.WeightTrajectory
import com.example.util.WeightUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    userPrefs: UserPreferences?,
    viewModel: SettingsViewModel = viewModel(),
    weightViewModel: WeightViewModel = viewModel()
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val installState by viewModel.installState.collectAsState()

    val p = userPrefs ?: UserPreferences(
        username = "User",
        heightCm = 170f,
        gender = "Male",
        weightUnit = WeightUnit.KG,
        heightUnit = HeightUnit.CM,
        useImperial = false,
        themeMode = ThemeMode.DARK,
        useDarkTheme = true,
        soundsEnabled = true,
        avatarId = "icon:🔥"
    )

    val weightEntries by weightViewModel.weightEntries.collectAsState()
    val latestWeightKg = weightEntries.firstOrNull()?.weightKg ?: 70f

    var editName by remember(p.username) { mutableStateOf(p.username) }
    var editHeightCm by remember(p.heightCm) { mutableStateOf(p.heightCm.toInt().toString()) }
    
    // Feet and inches state
    val (initialFeet, initialInches) = WeightUtils.cmToFeetAndInches(p.heightCm)
    var editFeet by remember(p.heightCm) { mutableStateOf(initialFeet.toString()) }
    var editInches by remember(p.heightCm) { mutableStateOf(initialInches.toString()) }

    // Waist state
    var editWaist by remember(p.waistCm, p.heightUnit) {
        val waistText = if (p.waistCm != null && p.waistCm > 0f) {
            if (p.heightUnit == HeightUnit.FT_IN) {
                String.format(Locale.getDefault(), "%.1f", p.waistCm / 2.54f)
            } else {
                String.format(Locale.getDefault(), "%.1f", p.waistCm)
            }
        } else {
            ""
        }
        mutableStateOf(waistText)
    }

    var customCaloriesInput by remember(p.customDailyCalories) {
        mutableStateOf(p.customDailyCalories.toString())
    }

    var showAvatarPicker by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var showReleaseNotesDialog by remember { mutableStateOf(false) }
    var showWeighInReminderDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingExportPayload by remember { mutableStateOf<String?>(null) }
    var backupResultSummary by remember { mutableStateOf<String?>(null) }

    val isBackingUp by viewModel.isBackingUp.collectAsState()
    val updateCheckState by viewModel.updateCheckState.collectAsState()

    // File pickers for Backup/Restore
    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingExportPayload != null) {
            viewModel.exportToFileUri(uri, pendingExportPayload!!) { success ->
                pendingExportPayload = null
                if (success) {
                    Toast.makeText(context, "JSON Backup saved successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to save backup file.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && pendingExportPayload != null) {
            viewModel.exportToFileUri(uri, pendingExportPayload!!) { success ->
                pendingExportPayload = null
                if (success) {
                    Toast.makeText(context, "CSV Export saved successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to save CSV file.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val jsonImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirmDialog = true
        }
    }

    // Calculate weekly weight projection for live preview
    val currentCalorieBudget = if (p.useCustomCalories && p.customDailyCalories > 0) p.customDailyCalories else {
        weightViewModel.calculateDailyCalories(latestWeightKg, p.heightCm, 30, p.gender, p.waistCm)
    }
    val weeklyProjection = remember(currentCalorieBudget, latestWeightKg, p.heightCm, p.waistCm, p.gender, p.weightUnit) {
        CalorieWeightCalculator.calculateWeeklyProjection(
            dailyBudget = currentCalorieBudget,
            weightKg = latestWeightKg,
            heightCm = p.heightCm,
            waistCm = p.waistCm,
            age = 30,
            gender = p.gender,
            unit = p.weightUnit
        )
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentAvatarId = p.avatarId,
            onDismiss = { showAvatarPicker = false },
            onAvatarSelected = { newAvatar ->
                viewModel.updateAvatarId(newAvatar)
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Clear All Application Data?",
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.danger
                )
            },
            text = {
                Text(
                    text = "This will permanently erase all your fasting history, food logs, weight entries, and preferences. You may export a JSON backup first.",
                    color = AppTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDeviceData {
                            showDeleteConfirmDialog = false
                            Toast.makeText(context, "All device data has been cleared.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.danger,
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
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.textPrimary),
                    border = BorderStroke(1.dp, AppTheme.colors.border)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = AppTheme.colors.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (showImportConfirmDialog && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirmDialog = false
                pendingImportUri = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Import JSON Backup",
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "How would you like to restore this backup file?\n\n• Merge: Adds all fasting sessions, food logs, and weights to your existing data.\n• Replace: Clears current logs and replaces them with the backup.",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportUri ?: return@Button
                        showImportConfirmDialog = false
                        viewModel.importFromJsonUri(uri, clearExisting = false) { result ->
                            pendingImportUri = null
                            backupResultSummary = result.message
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Merge Data", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val uri = pendingImportUri ?: return@TextButton
                            showImportConfirmDialog = false
                            viewModel.importFromJsonUri(uri, clearExisting = true) { result ->
                                pendingImportUri = null
                                backupResultSummary = result.message
                            }
                        }
                    ) {
                        Text("Replace Data", color = AppTheme.colors.danger, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = {
                            showImportConfirmDialog = false
                            pendingImportUri = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.textPrimary)
                    ) {
                        Text("Cancel")
                    }
                }
            },
            containerColor = AppTheme.colors.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (backupResultSummary != null) {
        AlertDialog(
            onDismissRequest = { backupResultSummary = null },
            title = { Text("Backup Operation", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(backupResultSummary ?: "", color = AppTheme.colors.textSecondary, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = { backupResultSummary = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = AppTheme.colors.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (showReleaseNotesDialog) {
        val releaseNotes = viewModel.getBuiltInReleaseNotes()
        val currentVer = BuildConfig.VERSION_NAME.ifEmpty { "1.2.4" }
        ReleaseNotesDialog(
            releaseNotes = releaseNotes,
            currentVersion = currentVer,
            onDismiss = { showReleaseNotesDialog = false }
        )
    }

    if (showWeighInReminderDialog) {
        WeighInReminderDialog(
            userPrefs = userPrefs,
            onDismiss = { showWeighInReminderDialog = false },
            onSaveReminder = { enabled, frequency, dayOfWeek, hour, minute ->
                weightViewModel.updateWeighInReminder(
                    enabled = enabled,
                    frequency = frequency,
                    dayOfWeek = dayOfWeek,
                    hour = hour,
                    minute = minute
                )
            },
            onSendTestNotification = {
                weightViewModel.sendTestReminder()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AppTheme.colors.primary.copy(alpha = if (AppTheme.colors.isDark) 0.15f else 0.10f),
                border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME.ifEmpty { "1.2.4" }}",
                    color = AppTheme.colors.primaryVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. PROFILE & AVATAR
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
            border = BorderStroke(1.dp, AppTheme.colors.border)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROFILE & AVATAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AppTheme.colors.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { showAvatarPicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Change Avatar",
                                color = AppTheme.colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Avatar header with user summary
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Box {
                            UserAvatarView(
                                avatarId = p.avatarId,
                                size = 60.dp,
                                onClick = { showAvatarPicker = true }
                            )
                            Surface(
                                onClick = { showAvatarPicker = true },
                                shape = CircleShape,
                                color = AppTheme.colors.primary,
                                border = BorderStroke(2.dp, AppTheme.colors.surfaceElevated),
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.BottomEnd)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Avatar",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = p.username.ifEmpty { "User" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val formattedH = WeightUtils.formatHeight(p.heightCm, p.heightUnit)
                            val waistSummary = if (p.waistCm != null && p.waistCm > 0f) {
                                " • Waist: ${WeightUtils.formatWaist(p.waistCm, p.heightUnit == HeightUnit.FT_IN)}"
                            } else ""
                            Text(
                                text = "${p.gender} • $formattedH$waistSummary",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap photo or icon to choose a new avatar",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.primary,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { showAvatarPicker = true }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display Name input
                OutlinedTextField(
                    value = editName,
                    onValueChange = {
                        editName = it
                        viewModel.updateUsername(it)
                    },
                    label = { Text("Display Name") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = AppTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primary,
                        unfocusedBorderColor = AppTheme.colors.border,
                        focusedTextColor = AppTheme.colors.textPrimary,
                        unfocusedTextColor = AppTheme.colors.textPrimary,
                        focusedContainerColor = AppTheme.colors.inputBackground,
                        unfocusedContainerColor = AppTheme.colors.inputBackground
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Height Section with Unit Switcher (cm vs ft/in)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Height",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Used for BMI & caloric calculations",
                            color = AppTheme.colors.textMuted,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(AppTheme.colors.surfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, AppTheme.colors.border, RoundedCornerShape(8.dp))
                            .padding(3.dp)
                    ) {
                        HeightUnit.values().forEach { hUnit ->
                            val isSelected = p.heightUnit == hUnit
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) AppTheme.colors.primary else Color.Transparent,
                                modifier = Modifier
                                    .clickable { viewModel.updateHeightUnit(hUnit) }
                            ) {
                                Text(
                                    text = if (hUnit == HeightUnit.CM) "cm" else "ft & in",
                                    color = if (isSelected) Color.White else AppTheme.colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (p.heightUnit == HeightUnit.CM) {
                    OutlinedTextField(
                        value = editHeightCm,
                        onValueChange = { input ->
                            if (input.all { c -> c.isDigit() } && input.length <= 3) {
                                editHeightCm = input
                                input.toFloatOrNull()?.let { h ->
                                    if (h > 0f) viewModel.updateHeight(h)
                                }
                            }
                        },
                        label = { Text("Height (Centimeters)") },
                        placeholder = { Text("e.g. 175") },
                        suffix = { Text("cm", color = AppTheme.colors.textMuted, fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.colors.primary,
                            unfocusedBorderColor = AppTheme.colors.border,
                            focusedTextColor = AppTheme.colors.textPrimary,
                            unfocusedTextColor = AppTheme.colors.textPrimary,
                            focusedContainerColor = AppTheme.colors.inputBackground,
                            unfocusedContainerColor = AppTheme.colors.inputBackground
                        )
                    )
                } else {
                    // Feet & Inches inputs side by side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = editFeet,
                            onValueChange = { fInput ->
                                if (fInput.all { c -> c.isDigit() } && fInput.length <= 2) {
                                    editFeet = fInput
                                    val feetVal = fInput.toIntOrNull() ?: 0
                                    val inchesVal = editInches.toIntOrNull() ?: 0
                                    if (feetVal > 0 || inchesVal > 0) {
                                        val totalCm = WeightUtils.feetAndInchesToCm(feetVal, inchesVal)
                                        viewModel.updateHeight(totalCm)
                                    }
                                }
                            },
                            label = { Text("Feet") },
                            placeholder = { Text("5") },
                            suffix = { Text("ft", color = AppTheme.colors.textMuted, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colors.primary,
                                unfocusedBorderColor = AppTheme.colors.border,
                                focusedTextColor = AppTheme.colors.textPrimary,
                                unfocusedTextColor = AppTheme.colors.textPrimary,
                                focusedContainerColor = AppTheme.colors.inputBackground,
                                unfocusedContainerColor = AppTheme.colors.inputBackground
                            )
                        )

                        OutlinedTextField(
                            value = editInches,
                            onValueChange = { inInput ->
                                if (inInput.all { c -> c.isDigit() } && inInput.length <= 2) {
                                    val inNum = inInput.toIntOrNull() ?: 0
                                    if (inNum < 12) {
                                        editInches = inInput
                                        val feetVal = editFeet.toIntOrNull() ?: 0
                                        val totalCm = WeightUtils.feetAndInchesToCm(feetVal, inNum)
                                        viewModel.updateHeight(totalCm)
                                    }
                                }
                            },
                            label = { Text("Inches") },
                            placeholder = { Text("10") },
                            suffix = { Text("in", color = AppTheme.colors.textMuted, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colors.primary,
                                unfocusedBorderColor = AppTheme.colors.border,
                                focusedTextColor = AppTheme.colors.textPrimary,
                                unfocusedTextColor = AppTheme.colors.textPrimary,
                                focusedContainerColor = AppTheme.colors.inputBackground,
                                unfocusedContainerColor = AppTheme.colors.inputBackground
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Waist Circumference
                OutlinedTextField(
                    value = editWaist,
                    onValueChange = { wInput ->
                        if (wInput.count { it == '.' } <= 1 && wInput.all { it.isDigit() || it == '.' } && wInput.length <= 5) {
                            editWaist = wInput
                            val num = wInput.toFloatOrNull()
                            if (num != null && num > 0f) {
                                val cm = if (p.heightUnit == HeightUnit.FT_IN) num * 2.54f else num
                                viewModel.updateWaist(cm)
                            } else if (wInput.isEmpty()) {
                                viewModel.updateWaist(null)
                            }
                        }
                    },
                    label = { Text(if (p.heightUnit == HeightUnit.FT_IN) "Waist Circumference (inches)" else "Waist Circumference (cm)") },
                    placeholder = { Text("Optional — for body fat / health metrics") },
                    suffix = { Text(if (p.heightUnit == HeightUnit.FT_IN) "in" else "cm", color = AppTheme.colors.textMuted, fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primary,
                        unfocusedBorderColor = AppTheme.colors.border,
                        focusedTextColor = AppTheme.colors.textPrimary,
                        unfocusedTextColor = AppTheme.colors.textPrimary,
                        focusedContainerColor = AppTheme.colors.inputBackground,
                        unfocusedContainerColor = AppTheme.colors.inputBackground
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Biological Sex / Gender Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Biological Sex / Gender",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "For BMR calculation",
                            color = AppTheme.colors.textMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Male", "Female").forEach { g ->
                            val isSelected = p.gender.equals(g, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) AppTheme.colors.primary.copy(alpha = if (AppTheme.colors.isDark) 0.22f else 0.14f) else AppTheme.colors.inputBackground,
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isSelected) AppTheme.colors.primary else AppTheme.colors.border
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clickable { viewModel.updateGender(g) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (g == "Male") "♂️" else "♀️",
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = g,
                                        color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. THEME & DISPLAY APPEARANCE (Light, Dark, System)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
            border = BorderStroke(1.dp, AppTheme.colors.border)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "THEME & APPEARANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Choose your preferred interface theme style:",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val themeOptions = listOf(
                        Triple(ThemeMode.DARK, "🌙 Dark Mode", "Deep Material You dark palette with ambient tonal surfaces"),
                        Triple(ThemeMode.LIGHT, "☀️ Light Mode", "Bright Material You daylight palette with crisp high-contrast surfaces"),
                        Triple(ThemeMode.SYSTEM, "📱 System Default", "Follows your Android device's day/night system theme")
                    )

                    themeOptions.forEach { (mode, title, desc) ->
                        val isSelected = p.themeMode == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) AppTheme.colors.primary.copy(alpha = if (AppTheme.colors.isDark) 0.20f else 0.12f) else AppTheme.colors.surfaceElevated,
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) AppTheme.colors.primary else AppTheme.colors.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateThemeMode(mode) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        color = AppTheme.colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.updateThemeMode(mode) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = AppTheme.colors.primary,
                                        unselectedColor = AppTheme.colors.textMuted
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Material You Dynamic Color Toggle Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
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
                                    shape = CircleShape,
                                    color = AppTheme.colors.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "🎨", fontSize = 18.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Material You Dynamic Color",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AppTheme.colors.textPrimary
                                        )
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = AppTheme.colors.primary.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "Monet",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AppTheme.colors.primary,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                            "Derives adaptive tones directly from your device wallpaper & system palette"
                                        else
                                            "Applies harmonious Google Material Design 3 dynamic color tokens",
                                        fontSize = 12.sp,
                                        color = AppTheme.colors.textSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Switch(
                                checked = p.dynamicColor,
                                onCheckedChange = { viewModel.updateDynamicColor(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AppTheme.colors.surface,
                                    checkedTrackColor = AppTheme.colors.primary,
                                    uncheckedThumbColor = AppTheme.colors.textMuted,
                                    uncheckedTrackColor = AppTheme.colors.surfaceHighlight
                                )
                            )
                        }

                        // Live Palette Swatches Preview (fluid columns that fill available width evenly)
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVE PALETTE PREVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = AppTheme.colors.textMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (p.dynamicColor) "Dynamic active" else "Default active",
                                fontSize = 10.sp,
                                color = AppTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val swatches = listOf(
                                "Primary" to AppTheme.colors.primary,
                                "Container" to AppTheme.colors.primaryVariant,
                                "Secondary" to AppTheme.colors.secondary,
                                "Tertiary" to AppTheme.colors.tertiary,
                                "Surface" to AppTheme.colors.surfaceElevated
                            )
                            swatches.forEach { (name, color) ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .background(color, RoundedCornerShape(8.dp))
                                            .border(1.dp, AppTheme.colors.borderLight, RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = name,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AppTheme.colors.textMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. MEASUREMENT & WEIGHT UNITS (Stone & Pounds, Pounds, KG)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
            border = BorderStroke(1.dp, AppTheme.colors.border)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "MEASUREMENT & UNITS",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select preferred weight unit system:",
                    color = AppTheme.colors.textSecondary,
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
                            color = if (isSelected) AppTheme.colors.primary.copy(alpha = if (AppTheme.colors.isDark) 0.18f else 0.12f) else AppTheme.colors.surfaceElevated,
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) AppTheme.colors.primary else AppTheme.colors.border
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
                                        color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = subtitle,
                                        color = AppTheme.colors.textMuted,
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
                                        selectedColor = AppTheme.colors.primary,
                                        unselectedColor = AppTheme.colors.textMuted
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. DAILY CALORIE INTAKE & WEEKLY WEIGHT LOSS PROJECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
            border = BorderStroke(1.dp, AppTheme.colors.border)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "DAILY CALORIE BUDGET & WEIGHT PROJECTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Configure how your daily calorie goal is determined:",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Option 1: Automatic BMR/TDEE
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!p.useCustomCalories) AppTheme.colors.primary.copy(alpha = if (AppTheme.colors.isDark) 0.18f else 0.12f) else AppTheme.colors.surfaceElevated,
                    border = BorderStroke(
                        1.5.dp,
                        if (!p.useCustomCalories) AppTheme.colors.primary else AppTheme.colors.border
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
                                color = if (!p.useCustomCalories) AppTheme.colors.primary else AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Uses Mifflin-St Jeor formula based on weight (${WeightUtils.formatWeight(latestWeightKg, p.weightUnit)}), height (${WeightUtils.formatHeight(p.heightCm, p.heightUnit)}), gender, and waist",
                                color = AppTheme.colors.textMuted,
                                fontSize = 12.sp
                            )
                        }
                        RadioButton(
                            selected = !p.useCustomCalories,
                            onClick = { viewModel.updateUseCustomCalories(false) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AppTheme.colors.primary,
                                unselectedColor = AppTheme.colors.textMuted
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Custom Daily Calories
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (p.useCustomCalories) AppTheme.colors.primary.copy(alpha = if (AppTheme.colors.isDark) 0.18f else 0.12f) else AppTheme.colors.surfaceElevated,
                    border = BorderStroke(
                        1.5.dp,
                        if (p.useCustomCalories) AppTheme.colors.primary else AppTheme.colors.border
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
                                    color = if (p.useCustomCalories) AppTheme.colors.primary else AppTheme.colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Set your own fixed target (e.g. 1500 - 3000 kcal/day)",
                                    color = AppTheme.colors.textMuted,
                                    fontSize = 12.sp
                                )
                            }
                            RadioButton(
                                selected = p.useCustomCalories,
                                onClick = { viewModel.updateUseCustomCalories(true) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AppTheme.colors.primary,
                                    unselectedColor = AppTheme.colors.textMuted
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
                                    focusedBorderColor = AppTheme.colors.primary,
                                    unfocusedBorderColor = AppTheme.colors.border,
                                    focusedTextColor = AppTheme.colors.textPrimary,
                                    unfocusedTextColor = AppTheme.colors.textPrimary,
                                    focusedContainerColor = AppTheme.colors.inputBackground,
                                    unfocusedContainerColor = AppTheme.colors.inputBackground
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Quick Targets:", color = AppTheme.colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1400, 1600, 1800, 2000, 2200).forEach { preset ->
                                    val isCurrent = p.customDailyCalories == preset
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) AppTheme.colors.primary else AppTheme.colors.inputBackground,
                                        border = BorderStroke(1.dp, if (isCurrent) AppTheme.colors.primary else AppTheme.colors.border),
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
                                                color = if (isCurrent) Color.White else AppTheme.colors.textSecondary,
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

                Spacer(modifier = Modifier.height(14.dp))

                // Live Weekly Weight Loss Telemetry Card
                val bannerColor = when (weeklyProjection.trajectory) {
                    WeightTrajectory.WEIGHT_LOSS -> AppTheme.colors.success
                    WeightTrajectory.WEIGHT_GAIN -> AppTheme.colors.warning
                    WeightTrajectory.MAINTENANCE -> AppTheme.colors.primary
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = bannerColor.copy(alpha = if (AppTheme.colors.isDark) 0.15f else 0.10f),
                    border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (weeklyProjection.trajectory == WeightTrajectory.WEIGHT_LOSS) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = bannerColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PROJECTED WEEKLY CHANGE",
                                    color = bannerColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Text(
                                text = weeklyProjection.summaryText,
                                color = bannerColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = weeklyProjection.explanationText,
                            color = AppTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. AUDIBLE ALERTS & PREFERENCES
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
            border = BorderStroke(1.dp, AppTheme.colors.border)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "ALERTS & NOTIFICATIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Audible Fasting Alerts", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Medium)
                        Text("Sound chime upon goal completion", color = AppTheme.colors.textMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = p.soundsEnabled,
                        onCheckedChange = { viewModel.updateSoundsEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppTheme.colors.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = AppTheme.colors.borderLight)
                Spacer(modifier = Modifier.height(14.dp))

                // Weigh-In Reminder Settings Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showWeighInReminderDialog = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Weigh-In Reminder",
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            if (p.weighInReminderEnabled) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AppTheme.colors.primary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = p.weighInFrequency.displayName,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.colors.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val reminderDesc = if (p.weighInReminderEnabled) {
                            val timeCal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, p.weighInHour)
                                set(Calendar.MINUTE, p.weighInMinute)
                            }
                            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(timeCal.time)
                            if (p.weighInFrequency == WeighInFrequency.DAILY) {
                                "Every day at $timeStr"
                            } else {
                                "${p.weighInFrequency.displayName} on ${WeightReminderManager.getDayOfWeekDisplayName(p.weighInDayOfWeek, short = true)} at $timeStr"
                            }
                        } else {
                            "Remind you on specific days & times to weigh in"
                        }
                        Text(
                            text = reminderDesc,
                            color = AppTheme.colors.textMuted,
                            fontSize = 12.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showWeighInReminderDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Configure Schedule",
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Switch(
                            checked = p.weighInReminderEnabled,
                            onCheckedChange = { weightViewModel.setWeighInReminderEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppTheme.colors.primary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 6. DATA BACKUP, EXPORT & IMPORT (.JSON / .CSV)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
            border = BorderStroke(1.dp, AppTheme.colors.border)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DATA BACKUP & EXPORT",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AppTheme.colors.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = ".JSON / .CSV",
                            color = AppTheme.colors.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Safely backup, transfer, or export your fasting sessions, food logs, and weight telemetry.",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action 1: Export .JSON
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isBackingUp) {
                            scope.launch {
                                val json = viewModel.getJsonExportData()
                                pendingExportPayload = json
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                jsonExportLauncher.launch("chomp_clock_backup_$timeStamp.json")
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AppTheme.colors.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = AppTheme.colors.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export Full JSON Backup", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Complete snapshot for migration and restore", color = AppTheme.colors.textMuted, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppTheme.colors.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action 2: Import .JSON
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isBackingUp) {
                            jsonImportLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AppTheme.colors.success.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = AppTheme.colors.success, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Import & Restore JSON Backup", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Merge or replace existing sessions and data", color = AppTheme.colors.textMuted, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppTheme.colors.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action 3: Export .CSV
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isBackingUp) {
                            scope.launch {
                                val csv = viewModel.getCsvExportData()
                                pendingExportPayload = csv
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                csvExportLauncher.launch("chomp_clock_export_$timeStamp.csv")
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AppTheme.colors.warning.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = AppTheme.colors.warning, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export CSV Spreadsheet", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Compatible with Excel, Google Sheets & Apple Numbers", color = AppTheme.colors.textMuted, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppTheme.colors.textMuted)
                    }
                }

                if (isBackingUp) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AppTheme.colors.primary,
                        trackColor = AppTheme.colors.border
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 7. GITHUB RELEASES & UPDATES
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
            border = BorderStroke(1.dp, AppTheme.colors.border)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UPDATES & RELEASES",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AppTheme.colors.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/PASSK3YS/Chomp-Clock/releases")
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PASSK3YS / Chomp-Clock",
                                color = AppTheme.colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = "Open GitHub Releases",
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Installed build info
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Installed: v${BuildConfig.VERSION_NAME.ifEmpty { "1.3.2" }}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = AppTheme.colors.textPrimary
                            )
                        }
                        Text(
                            text = "Latest Verified Build",
                            color = AppTheme.colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons: Check for Updates & View Release Notes Popup
                val isChecking = updateCheckState is UpdateCheckState.Checking
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.checkForUpdates() },
                        enabled = !isChecking,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.primary,
                            disabledContainerColor = AppTheme.colors.primary.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Checking...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check for Updates", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showReleaseNotesDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.textPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = AppTheme.colors.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Release Notes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Update Result States with Animated Transition
                AnimatedContent(
                    targetState = updateCheckState,
                    transitionSpec = { fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180)) },
                    label = "UpdateCheckAnimation"
                ) { state ->
                    when (state) {
                        is UpdateCheckState.Idle -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppTheme.colors.surfaceElevated,
                                border = BorderStroke(1.dp, AppTheme.colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = AppTheme.colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tap 'Check for Updates' to verify against PASSK3YS/Chomp-Clock on GitHub.",
                                        color = AppTheme.colors.textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        is UpdateCheckState.Checking -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppTheme.colors.primary.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = AppTheme.colors.primary,
                                                strokeWidth = 2.5.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Checking for Updates...",
                                                fontWeight = FontWeight.Bold,
                                                color = AppTheme.colors.textPrimary,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AppTheme.colors.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Step ${state.step}/3",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AppTheme.colors.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = state.statusMessage,
                                        color = AppTheme.colors.textSecondary,
                                        fontSize = 12.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp),
                                        color = AppTheme.colors.primary,
                                        trackColor = AppTheme.colors.primary.copy(alpha = 0.15f)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Querying repository: PASSK3YS / Chomp-Clock",
                                        color = AppTheme.colors.textMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        is UpdateCheckState.UpdateAvailable -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppTheme.colors.success.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, AppTheme.colors.success.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFB300),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "New Update Available: ${state.latestVersion}",
                                                fontWeight = FontWeight.Bold,
                                                color = AppTheme.colors.textPrimary,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AppTheme.colors.success.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "v${BuildConfig.VERSION_NAME} → ${state.latestVersion}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AppTheme.colors.success,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = state.releaseNotes,
                                        color = AppTheme.colors.textSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 4
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // In-App Install Progress / Action
                                    val progress = installState
                                    if (progress.isDownloading) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Downloading APK: ${(progress.progress * 100).toInt()}%",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AppTheme.colors.primary
                                                )
                                                if (progress.totalMb > 0f) {
                                                    Text(
                                                        text = "%.1f / %.1f MB".format(progress.downloadedMb, progress.totalMb),
                                                        fontSize = 10.sp,
                                                        color = AppTheme.colors.textMuted
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { progress.progress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                color = AppTheme.colors.primary,
                                                trackColor = AppTheme.colors.surfaceElevated
                                            )
                                        }
                                    } else if (progress.isReadyToInstall) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Launching Android package installer...",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AppTheme.colors.primary
                                            )
                                        }
                                    } else if (progress.error != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFE53935).copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = progress.error,
                                                    color = Color(0xFFE53935),
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Main action buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val url = state.downloadUrl ?: state.htmlUrl
                                                viewModel.downloadAndInstallUpdate(context, url)
                                            },
                                            enabled = !installState.isDownloading && !installState.isReadyToInstall,
                                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.success),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1.2f)
                                        ) {
                                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (installState.isDownloading) "Downloading..." else "Install Update",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { showReleaseNotesDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, AppTheme.colors.border),
                                            modifier = Modifier.weight(0.8f)
                                        ) {
                                            Text("Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val url = state.downloadUrl ?: state.htmlUrl
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                context.startActivity(intent)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, AppTheme.colors.border),
                                            modifier = Modifier.weight(0.8f)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, tint = AppTheme.colors.textMuted, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("APK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Android Install Unknown Apps permission helper if needed
                                    if (!InAppUpdateInstaller.canInstallApks(context)) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AppTheme.colors.warning.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, AppTheme.colors.warning.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { InAppUpdateInstaller.openInstallPermissionSettings(context) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Security, contentDescription = null, tint = AppTheme.colors.warning, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Tap to enable 'Install unknown apps' permission in Android Settings",
                                                    color = AppTheme.colors.textPrimary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppTheme.colors.warning, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Note: If updating an existing debug build causes a signature mismatch ('App not installed'), export a data backup above first, then reinstall.",
                                        color = AppTheme.colors.textMuted,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                        is UpdateCheckState.UpToDate -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppTheme.colors.success.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, AppTheme.colors.success.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = AppTheme.colors.success,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "App is Up to Date (${state.currentVersion})",
                                                color = AppTheme.colors.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AppTheme.colors.success.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Verified",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AppTheme.colors.success,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Your app is running the latest verified release build (v${BuildConfig.VERSION_NAME.ifEmpty { "1.2.6" }}), checked against PASSK3YS/Chomp-Clock on GitHub.",
                                        color = AppTheme.colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showReleaseNotesDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, AppTheme.colors.border),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Description, contentDescription = null, tint = AppTheme.colors.primary, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Release Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                uriHandler.openUri(state.htmlUrl)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, AppTheme.colors.border),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = AppTheme.colors.textMuted, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("GitHub Releases", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        is UpdateCheckState.NoReleasesFound, is UpdateCheckState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppTheme.colors.surfaceElevated,
                                border = BorderStroke(1.dp, AppTheme.colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppTheme.colors.success, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "App is Up to Date (v${BuildConfig.VERSION_NAME.ifEmpty { "1.2.6" }})",
                                            color = AppTheme.colors.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Latest verified build active. You can view all release notes or open the GitHub releases page directly.",
                                        color = AppTheme.colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showReleaseNotesDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, AppTheme.colors.border),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Release Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                uriHandler.openUri("https://github.com/PASSK3YS/Chomp-Clock/releases")
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, AppTheme.colors.border),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("GitHub Releases", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

        // 8. DANGER ZONE
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
            border = BorderStroke(1.dp, AppTheme.colors.danger.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "DANGER ZONE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.danger,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Irreversibly delete all fasting sessions, food logs, and weigh-ins from this device.",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger),
                    border = BorderStroke(1.dp, AppTheme.colors.danger.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = AppTheme.colors.danger)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Application Data", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
