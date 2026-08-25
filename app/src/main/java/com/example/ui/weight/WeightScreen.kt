package com.example.ui.weight

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeightScreen(
    viewModel: WeightViewModel = viewModel(),
    username: String,
    heightCm: Float,
    gender: String,
    useImperial: Boolean
) {
    val entries by viewModel.weightEntries.collectAsState()
    
    var weightInput by remember { mutableStateOf("") }
    var waistInput by remember { mutableStateOf("") }

    val currentHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHourOfDay) {
        in 0..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    val latestWeight = entries.firstOrNull()?.weightKg ?: 70f
    val bmi = viewModel.calculateBmi(latestWeight, heightCm)
    val dailyCalories = viewModel.calculateDailyCalories(latestWeight, heightCm, 30, gender)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.UK)
        Text(dateFormat.format(Date()), color = androidx.compose.ui.graphics.Color(0xFFA1A1AA), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        Text(text = "$greeting, $username", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White)
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18181B)),
            border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF27272A)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("CURRENT METRICS", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF71717A), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                val weightStr = if (useImperial) "${String.format(Locale.getDefault(), "%.1f", latestWeight * 2.20462)} lbs" else "${String.format(Locale.getDefault(), "%.1f", latestWeight)} kg"
                Text("Weight: $weightStr", color = androidx.compose.ui.graphics.Color.White)
                Text("BMI: ${String.format(Locale.getDefault(), "%.1f", bmi)}", color = androidx.compose.ui.graphics.Color.White)
                Text("Daily Target: $dailyCalories kcal", color = androidx.compose.ui.graphics.Color(0xFF34D399), fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("LOG NEW WEIGHT", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF71717A), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it },
                label = { Text(if (useImperial) "Weight (lbs)" else "Weight (kg)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color(0xFF18181B),
                    focusedContainerColor = androidx.compose.ui.graphics.Color(0xFF18181B),
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFF27272A),
                    focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF3B82F6)
                )
            )
            OutlinedTextField(
                value = waistInput,
                onValueChange = { waistInput = it },
                label = { Text(if (useImperial) "Waist (in)" else "Waist (cm)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color(0xFF18181B),
                    focusedContainerColor = androidx.compose.ui.graphics.Color(0xFF18181B),
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFF27272A),
                    focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF3B82F6)
                )
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val w = weightInput.toFloatOrNull() ?: 0f
                val waist = waistInput.toFloatOrNull()
                val wKg = if (useImperial) w / 2.20462f else w
                val waistCm = if (waist != null && useImperial) waist * 2.54f else waist
                viewModel.addWeightEntry(wKg, waistCm)
                weightInput = ""
                waistInput = ""
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFF4F4F5), contentColor = androidx.compose.ui.graphics.Color.Black)
        ) {
            Text("Save Log", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("PAST LOGS", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF71717A), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            items(entries) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18181B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF27272A)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val dateString = dateFormat.format(Date(entry.date))
                        val wStr = if (useImperial) "${String.format(Locale.getDefault(), "%.1f", entry.weightKg * 2.20462)} lbs" else "${String.format(Locale.getDefault(), "%.1f", entry.weightKg)} kg"
                        Text(dateString, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                        Text(wStr, color = androidx.compose.ui.graphics.Color(0xFF60A5FA))
                    }
                }
            }
        }
    }
}
