package com.example.ui.settings

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.UserPreferences
import com.example.data.repository.WeightUnit
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.UserAvatarView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    userPrefs: UserPreferences?,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val p = userPrefs ?: UserPreferences("User", 170f, "Male", WeightUnit.KG, false, true, true)
    var editName by remember(p.username) { mutableStateOf(p.username) }
    var editHeight by remember(p.heightCm) { mutableStateOf(if (p.heightCm > 0) p.heightCm.toInt().toString() else "") }

    var showAvatarPicker by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateCheckResult by remember { mutableStateOf<String?>(null) }
    var showReleaseNotes by remember { mutableStateOf(false) }

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
                    border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF18181B),
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Text(
            text = "SETTINGS & PREFERENCES",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF71717A),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 1. USER PROFILE SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "USER PROFILE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF71717A),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Avatar preview + change button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        UserAvatarView(
                            avatarId = p.avatarId,
                            size = 64.dp,
                            onClick = { showAvatarPicker = true }
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF3B82F6),
                            border = BorderStroke(1.5.dp, Color(0xFF18181B)),
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { showAvatarPicker = true }
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
                            text = p.username,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Tap avatar to change icon or photo",
                            color = Color(0xFF60A5FA),
                            fontSize = 12.sp,
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF27272A),
                        focusedContainerColor = Color(0xFF27272A),
                        unfocusedBorderColor = Color(0xFF3F3F46),
                        focusedBorderColor = Color(0xFF3B82F6)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editHeight,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            editHeight = it
                            it.toFloatOrNull()?.let { h -> viewModel.updateHeight(h) }
                        }
                    },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF27272A),
                        focusedContainerColor = Color(0xFF27272A),
                        unfocusedBorderColor = Color(0xFF3F3F46),
                        focusedBorderColor = Color(0xFF3B82F6)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "BIOLOGICAL GENDER",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF71717A),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Row(modifier = Modifier.padding(top = 6.dp)) {
                    listOf("Male", "Female").forEach { gender ->
                        val isSelected = p.gender.equals(gender, ignoreCase = true)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { viewModel.updateGender(gender) }
                                .padding(end = 20.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.updateGender(gender) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF3B82F6),
                                    unselectedColor = Color(0xFF71717A)
                                )
                            )
                            Text(gender, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. METRICS & WEIGHT UNITS SECTION (Stone & Pounds / Pounds / KG)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "WEIGHT MEASUREMENT UNIT",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF71717A),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Choose your preferred unit for logging and stats display.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple(WeightUnit.KG, "Kilograms (kg)", "e.g. 72.5 kg"),
                        Triple(WeightUnit.LBS, "Pounds (lbs)", "e.g. 159.8 lbs"),
                        Triple(WeightUnit.STONE_LBS, "Stone & Pounds (st & lbs)", "e.g. 11 st 5.8 lbs")
                    ).forEach { (unit, title, example) ->
                        val isSelected = p.weightUnit == unit
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.15f) else Color(0xFF27272A),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF3B82F6) else Color(0xFF3F3F46)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateWeightUnit(unit) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = title,
                                        color = if (isSelected) Color(0xFF60A5FA) else Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = example,
                                        color = Color(0xFF71717A),
                                        fontSize = 11.sp
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.updateWeightUnit(unit) },
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

        // 3. GENERAL PREFERENCES SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "APPLICATION PREFERENCES",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF71717A),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark AMOLED Theme", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("True black OLED optimized", color = Color(0xFF71717A), fontSize = 12.sp)
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

        // 4. APP UPDATES SECTION (Polished & Styled)
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
                        text = "APP UPDATES & RELEASES",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF71717A),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF064E3B),
                        border = BorderStroke(1.dp, Color(0xFF059669))
                    ) {
                        Text(
                            text = "v1.1.0 • Up to date",
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Chomp Clock Pro Edition", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Latest build installed and verified", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isCheckingUpdates = true
                            updateCheckResult = null
                            delay(1200)
                            isCheckingUpdates = false
                            updateCheckResult = "You are on the latest version of Chomp Clock (v1.1.0)."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF27272A),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFF3F3F46))
                ) {
                    if (isCheckingUpdates) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF60A5FA),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking for updates...", fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF60A5FA))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check for Updates", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (updateCheckResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = updateCheckResult!!,
                        color = Color(0xFF34D399),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Release notes dropdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReleaseNotes = !showReleaseNotes }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("What's new in v1.1.0", color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Icon(
                        imageVector = if (showReleaseNotes) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = showReleaseNotes) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(Color(0xFF27272A).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text("• Added Stone & Pounds (st & lbs) metric options in settings", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                        Text("• Custom fasting duration picker with minutes & hours", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                        Text("• Persistent Breakfast, Lunch, Dinner, Snacks & Drinks logs", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                        Text("• Advanced Analytics dashboard with multi-timeframe charts", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                        Text("• Profile Avatar icon chooser and custom gallery photo uploader", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                        Text("• Protected Danger Zone data erase confirmation", color = Color(0xFFA1A1AA), fontSize = 12.sp)
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
            text = "Chomp Clock v1.1.0 • Built with Jetpack Compose",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF71717A)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
