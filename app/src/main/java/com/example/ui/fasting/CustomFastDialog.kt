package com.example.ui.fasting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AppTheme

@Composable
fun CustomFastDialog(
    onDismiss: () -> Unit,
    onStartFast: (durationMillis: Long) -> Unit
) {
    var hoursInput by remember { mutableStateOf("16") }
    var minutesInput by remember { mutableStateOf("0") }

    val presetDurations = listOf(12, 14, 16, 18, 20, 24, 36, 48, 72)

    val currentHours = hoursInput.toIntOrNull() ?: 0
    val currentMinutes = minutesInput.toIntOrNull() ?: 0
    val totalHours = currentHours + (currentMinutes / 60f)

    val stagePreview = when {
        totalHours < 4f -> "Digestion Phase • Insulin dropping"
        totalHours < 12f -> "Fat Burning Zone • Glycogen depleted"
        totalHours < 16f -> "Ketosis Starting • Fat oxidation accelerates"
        totalHours < 24f -> "Deep Ketosis & Cellular Repair"
        else -> "Autophagy & Deep Fasting State"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AppTheme.colors.surface,
            border = BorderStroke(1.dp, AppTheme.colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = AppTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CUSTOM FAST DURATION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = AppTheme.colors.textMuted
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AppTheme.colors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick preset pills
                Text(
                    text = "Quick Presets",
                    color = AppTheme.colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetDurations) { presetH ->
                        val isSelected = currentHours == presetH && currentMinutes == 0
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) AppTheme.colors.primary.copy(alpha = 0.25f) else AppTheme.colors.surfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) AppTheme.colors.primary else AppTheme.colors.border
                            ),
                            modifier = Modifier.clickable {
                                hoursInput = presetH.toString()
                                minutesInput = "0"
                            }
                        ) {
                            Text(
                                text = "${presetH}h",
                                color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Custom Hours & Minutes inputs with stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hours field
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hours",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(AppTheme.colors.surfaceElevated, RoundedCornerShape(12.dp))
                                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                                .padding(horizontal = 6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val h = (hoursInput.toIntOrNull() ?: 1) - 1
                                    if (h >= 1) hoursInput = h.toString()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = AppTheme.colors.textPrimary, modifier = Modifier.size(16.dp))
                            }
                            OutlinedTextField(
                                value = hoursInput,
                                onValueChange = { if (it.length <= 3 && it.all { char -> char.isDigit() }) hoursInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.textPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = AppTheme.colors.textPrimary,
                                    unfocusedTextColor = AppTheme.colors.textPrimary
                                ),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    val h = (hoursInput.toIntOrNull() ?: 0) + 1
                                    if (h <= 168) hoursInput = h.toString()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = AppTheme.colors.textPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Minutes field
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Minutes",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(AppTheme.colors.surfaceElevated, RoundedCornerShape(12.dp))
                                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                                .padding(horizontal = 6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val m = (minutesInput.toIntOrNull() ?: 0) - 15
                                    minutesInput = if (m < 0) "0" else m.toString()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = AppTheme.colors.textPrimary, modifier = Modifier.size(16.dp))
                            }
                            OutlinedTextField(
                                value = minutesInput,
                                onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) minutesInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.textPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = AppTheme.colors.textPrimary,
                                    unfocusedTextColor = AppTheme.colors.textPrimary
                                ),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    val m = (minutesInput.toIntOrNull() ?: 0) + 15
                                    minutesInput = if (m >= 60) "45" else m.toString()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = AppTheme.colors.textPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stage Info Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, AppTheme.colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "METABOLIC TARGET",
                            color = AppTheme.colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stagePreview,
                            color = AppTheme.colors.success,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val h = hoursInput.toLongOrNull() ?: 0L
                        val m = minutesInput.toLongOrNull() ?: 0L
                        val totalMillis = ((h * 3600) + (m * 60)) * 1000
                        if (totalMillis > 0) {
                            onStartFast(totalMillis)
                            onDismiss()
                        }
                    },
                    enabled = (currentHours > 0 || currentMinutes > 0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Start ${if (currentHours > 0) "${currentHours}h " else ""}${if (currentMinutes > 0) "${currentMinutes}m " else ""}Fast",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
