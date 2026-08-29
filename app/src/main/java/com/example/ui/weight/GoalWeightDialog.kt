package com.example.ui.weight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.WeightUnit
import com.example.ui.theme.AppTheme
import com.example.util.WeightUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalWeightDialog(
    currentGoalWeightKg: Float?,
    weightUnit: WeightUnit,
    onDismiss: () -> Unit,
    onSave: (Float?) -> Unit
) {
    var weightInput by remember { 
        mutableStateOf(
            if (currentGoalWeightKg != null && weightUnit != WeightUnit.STONE_LBS) {
                WeightUtils.formatWeight(currentGoalWeightKg, weightUnit).replace(Regex("[^0-9.]"), "").trim()
            } else ""
        )
    }
    
    var stoneInput by remember {
        mutableStateOf(
            if (currentGoalWeightKg != null && weightUnit == WeightUnit.STONE_LBS) {
                val totalLbs = (currentGoalWeightKg * 2.20462f).toInt()
                val st = totalLbs / 14
                st.toString()
            } else ""
        )
    }
    
    var lbsInput by remember {
         mutableStateOf(
            if (currentGoalWeightKg != null && weightUnit == WeightUnit.STONE_LBS) {
                val totalLbs = (currentGoalWeightKg * 2.20462f).toInt()
                val lbs = totalLbs % 14
                lbs.toString()
            } else ""
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = AppTheme.colors.background,
            border = BorderStroke(1.dp, AppTheme.colors.border)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set Goal Weight",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "What is your target weight?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))

                if (weightUnit == WeightUnit.STONE_LBS) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = stoneInput,
                            onValueChange = { stoneInput = it },
                            label = { Text("Stone") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colors.primary,
                                unfocusedBorderColor = AppTheme.colors.border
                            )
                        )
                        OutlinedTextField(
                            value = lbsInput,
                            onValueChange = { lbsInput = it },
                            label = { Text("Lbs") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colors.primary,
                                unfocusedBorderColor = AppTheme.colors.border
                            )
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (${weightUnit.shortName})") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.colors.primary,
                            unfocusedBorderColor = AppTheme.colors.border
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            onSave(null) // clear goal weight
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.danger),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                    ) {
                        Text("Clear", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val parsedKg = if (weightUnit == WeightUnit.STONE_LBS) {
                                WeightUtils.parseToKg(stoneInput, lbsInput, WeightUnit.STONE_LBS)
                            } else {
                                WeightUtils.parseToKg(weightInput, "", weightUnit)
                            }
                            onSave(parsedKg)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = AppTheme.colors.textSecondary)
                }
            }
        }
    }
}
