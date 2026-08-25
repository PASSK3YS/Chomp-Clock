package com.example.ui.food

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BarcodeScannerScreen
import java.util.Calendar

@Composable
fun FoodScreen(
    viewModel: FoodViewModel = viewModel(),
    username: String
) {
    val entries by viewModel.foodEntries.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    
    val currentHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHourOfDay) {
        in 0..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    if (showScanner) {
        BarcodeScannerScreen(onBarcodeScanned = { barcode ->
            viewModel.scanBarcode(barcode, "Snack") // Default to snack for scanned items for simplicity
            showScanner = false
        })
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        val dateFormat = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.UK)
        Text(dateFormat.format(java.util.Date()), color = androidx.compose.ui.graphics.Color(0xFFA1A1AA), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        Text(text = "$greeting, $username", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White)
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("FOOD LOG", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF71717A), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row {
                IconButton(onClick = { showScanner = true }) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = androidx.compose.ui.graphics.Color.White)
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Food", tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack", "Drink")
            mealTypes.forEach { meal ->
                val mealEntries = entries.filter { it.mealType.equals(meal, ignoreCase = true) }
                if (mealEntries.isNotEmpty()) {
                    item {
                        Text(meal.uppercase(), style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFFA1A1AA), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    }
                    items(mealEntries) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18181B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF27272A)),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(entry.name, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                                    Text(entry.servingSize, style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color(0xFFA1A1AA))
                                }
                                Text("${entry.calories} kcal", color = androidx.compose.ui.graphics.Color(0xFF34D399), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var foodName by remember { mutableStateOf("") }
        var servingSize by remember { mutableStateOf("") }
        var calories by remember { mutableStateOf("") }
        var mealType by remember { mutableStateOf("Breakfast") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Food") },
            text = {
                Column {
                    OutlinedTextField(value = foodName, onValueChange = { foodName = it }, label = { Text("Food Name") })
                    OutlinedTextField(value = servingSize, onValueChange = { servingSize = it }, label = { Text("Serving Size") })
                    OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Calories") })
                    // Basic dropdown/selector substitute for simplicity
                    OutlinedTextField(value = mealType, onValueChange = { mealType = it }, label = { Text("Meal Type (Breakfast/Lunch/Dinner/Snack/Drink)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val calInt = calories.toIntOrNull() ?: 0
                    viewModel.addFoodEntry(foodName, servingSize, calInt, mealType)
                    showAddDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
