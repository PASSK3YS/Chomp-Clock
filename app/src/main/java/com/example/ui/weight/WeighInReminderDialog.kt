package com.example.ui.weight

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.repository.UserPreferences
import com.example.data.repository.WeighInFrequency
import com.example.ui.components.SlideUpBottomSheetDialog
import com.example.ui.theme.AppTheme
import com.example.util.WeightReminderManager
import java.util.Calendar
import java.util.Locale

@Composable
fun WeighInReminderDialog(
    userPrefs: UserPreferences?,
    onDismiss: () -> Unit,
    onSaveReminder: (enabled: Boolean, frequency: WeighInFrequency, dayOfWeek: Int, hour: Int, minute: Int) -> Unit,
    onSendTestNotification: () -> Unit
) {
    val context = LocalContext.current

    var isEnabled by remember { mutableStateOf(userPrefs?.weighInReminderEnabled ?: true) }
    var selectedFrequency by remember { mutableStateOf(userPrefs?.weighInFrequency ?: WeighInFrequency.WEEKLY) }
    var selectedDayOfWeek by remember { mutableStateOf(userPrefs?.weighInDayOfWeek ?: Calendar.MONDAY) }
    var selectedHour by remember { mutableStateOf(userPrefs?.weighInHour ?: 8) }
    var selectedMinute by remember { mutableStateOf(userPrefs?.weighInMinute ?: 0) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                "Notifications are needed to receive weigh-in reminders.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val daysOfWeek = listOf(
        Calendar.MONDAY to ("Mon" to "Monday"),
        Calendar.TUESDAY to ("Tue" to "Tuesday"),
        Calendar.WEDNESDAY to ("Wed" to "Wednesday"),
        Calendar.THURSDAY to ("Thu" to "Thursday"),
        Calendar.FRIDAY to ("Fri" to "Friday"),
        Calendar.SATURDAY to ("Sat" to "Saturday"),
        Calendar.SUNDAY to ("Sun" to "Sunday")
    )

    val timePresets = listOf(
        7 to 0 to "7:00 AM",
        7 to 30 to "7:30 AM",
        8 to 0 to "8:00 AM",
        8 to 30 to "8:30 AM",
        9 to 0 to "9:00 AM",
        19 to 0 to "7:00 PM"
    )

    val openSystemTimePicker = {
        val dialog = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
            },
            selectedHour,
            selectedMinute,
            false // 12-hour format with AM/PM
        )
        dialog.show()
    }

    SlideUpBottomSheetDialog(
        onDismissRequest = onDismiss,
        maxHeightFraction = 0.92f
    ) { dismissWithAnimation ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Fixed Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = AppTheme.colors.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Weigh-In Reminder",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = "Stay consistent with timely reminders",
                            fontSize = 12.sp,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                IconButton(onClick = { dismissWithAnimation() }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppTheme.colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isEnabled) AppTheme.colors.primary.copy(alpha = 0.10f) else AppTheme.colors.surfaceElevated,
                border = BorderStroke(1.dp, if (isEnabled) AppTheme.colors.primary.copy(alpha = 0.35f) else AppTheme.colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEnabled) "Reminders Enabled" else "Reminders Disabled",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isEnabled) AppTheme.colors.primary else AppTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isEnabled) "You will receive notifications on your chosen schedule" else "Turn on to get notified to step on the scale",
                            fontSize = 12.sp,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            isEnabled = checked
                            if (checked && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppTheme.colors.primary,
                            uncheckedThumbColor = AppTheme.colors.textMuted,
                            uncheckedTrackColor = AppTheme.colors.border
                        )
                    )
                }
            }

            // Notification permission warning if missing
            if (isEnabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppTheme.colors.warning.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AppTheme.colors.warning.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AppTheme.colors.warning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notification permission is required for reminders to sound.",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.warning),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Allow", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isEnabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))

                    // 1. Frequency Selection Section
                    Text(
                        text = "REPEAT FREQUENCY",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeighInFrequency.values().forEach { freq ->
                            val isSelected = selectedFrequency == freq
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) AppTheme.colors.primary.copy(alpha = 0.12f) else AppTheme.colors.surfaceElevated,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) AppTheme.colors.primary else AppTheme.colors.border
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFrequency = freq }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedFrequency = freq },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = AppTheme.colors.primary,
                                                unselectedColor = AppTheme.colors.textMuted
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = freq.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.textPrimary
                                            )
                                            Text(
                                                text = freq.shortDescription,
                                                fontSize = 11.sp,
                                                color = AppTheme.colors.textSecondary
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = AppTheme.colors.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Day of Week Selection (Only for Weekly, Bi-Weekly, Monthly)
                    if (selectedFrequency != WeighInFrequency.DAILY) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DAY OF THE WEEK",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.textMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                            Text(
                                text = WeightReminderManager.getDayOfWeekDisplayName(selectedDayOfWeek, short = false),
                                color = AppTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            daysOfWeek.forEach { (calDay, labels) ->
                                val (shortLabel, fullLabel) = labels
                                val isSelected = selectedDayOfWeek == calDay
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.surfaceElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) AppTheme.colors.primary else AppTheme.colors.border
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable { selectedDayOfWeek = calDay }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = shortLabel,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else AppTheme.colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. Time Selection Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REMINDER TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = "Tap to edit",
                            color = AppTheme.colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Time Display & Picker Trigger
                    val formattedTime = remember(selectedHour, selectedMinute) {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, selectedHour)
                            set(Calendar.MINUTE, selectedMinute)
                        }
                        java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = AppTheme.colors.surfaceElevated,
                        border = BorderStroke(1.dp, AppTheme.colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openSystemTimePicker() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = AppTheme.colors.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = formattedTime,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = AppTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = if (selectedHour in 5..11) "Morning weigh-in (recommended)" else "Custom time",
                                        fontSize = 11.sp,
                                        color = AppTheme.colors.textSecondary
                                    )
                                }
                            }

                            Button(
                                onClick = { openSystemTimePicker() },
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Change",
                                    color = AppTheme.colors.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Time Presets
                    Text(
                        text = "Quick Presets:",
                        fontSize = 11.sp,
                        color = AppTheme.colors.textMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timePresets.take(4).forEach { (preset, label) ->
                            val (h, m) = preset
                            val isPresetSelected = selectedHour == h && selectedMinute == m
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPresetSelected) AppTheme.colors.primary else AppTheme.colors.surfaceElevated,
                                border = BorderStroke(1.dp, if (isPresetSelected) AppTheme.colors.primary else AppTheme.colors.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedHour = h
                                        selectedMinute = m
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isPresetSelected) Color.White else AppTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Next Reminder Preview Banner
                    val nextPreviewText = remember(selectedFrequency, selectedDayOfWeek, selectedHour, selectedMinute) {
                        WeightReminderManager.formatNextReminderPreview(
                            frequency = selectedFrequency,
                            dayOfWeek = selectedDayOfWeek,
                            hour = selectedHour,
                            minute = selectedMinute
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppTheme.colors.success.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AppTheme.colors.success.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = AppTheme.colors.success,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "NEXT SCHEDULED REMINDER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.success,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = nextPreviewText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppTheme.colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Pinned Bottom Action Bar
        Surface(
            color = AppTheme.colors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                HorizontalDivider(color = AppTheme.colors.border.copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onSendTestNotification()
                                Toast.makeText(context, "Test notification sent! Check your notification bar.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.border),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = AppTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Test Now",
                            fontSize = 13.sp,
                            color = AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            onSaveReminder(
                                isEnabled,
                                selectedFrequency,
                                selectedDayOfWeek,
                                selectedHour,
                                selectedMinute
                            )
                            Toast.makeText(
                                context,
                                if (isEnabled) "Weigh-in reminder saved!" else "Reminders turned off",
                                Toast.LENGTH_SHORT
                            ).show()
                            dismissWithAnimation()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save Schedule",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
}
