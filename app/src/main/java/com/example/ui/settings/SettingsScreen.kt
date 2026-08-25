package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.UserPreferences

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val prefs by viewModel.userPrefs.collectAsState()
    
    if (prefs == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val p = prefs!!

    var editName by remember { mutableStateOf(p.username) }
    var editHeight by remember { mutableStateOf(p.heightCm.toString()) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("USER PROFILE", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF71717A), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = editName,
            onValueChange = { editName = it; viewModel.updateUsername(it) },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = androidx.compose.ui.graphics.Color(0xFF18181B),
                focusedContainerColor = androidx.compose.ui.graphics.Color(0xFF18181B),
                unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFF27272A),
                focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF3B82F6)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = editHeight,
            onValueChange = { 
                editHeight = it
                it.toFloatOrNull()?.let { h -> viewModel.updateHeight(h) }
            },
            label = { Text("Height (cm)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = androidx.compose.ui.graphics.Color(0xFF18181B),
                focusedContainerColor = androidx.compose.ui.graphics.Color(0xFF18181B),
                unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFF27272A),
                focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF3B82F6)
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("BIOLOGICAL GENDER", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF71717A), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(modifier = Modifier.padding(top = 8.dp)) {
            RadioButton(
                selected = p.gender == "Male", 
                onClick = { viewModel.updateGender("Male") },
                colors = RadioButtonDefaults.colors(selectedColor = androidx.compose.ui.graphics.Color(0xFF3B82F6), unselectedColor = androidx.compose.ui.graphics.Color(0xFF71717A))
            )
            Text("Male", modifier = Modifier.align(Alignment.CenterVertically), color = androidx.compose.ui.graphics.Color.White)
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = p.gender == "Female", 
                onClick = { viewModel.updateGender("Female") },
                colors = RadioButtonDefaults.colors(selectedColor = androidx.compose.ui.graphics.Color(0xFF3B82F6), unselectedColor = androidx.compose.ui.graphics.Color(0xFF71717A))
            )
            Text("Female", modifier = Modifier.align(Alignment.CenterVertically), color = androidx.compose.ui.graphics.Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("PREFERENCES", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF71717A), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18181B)),
            border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF27272A)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Use Imperial Units", color = androidx.compose.ui.graphics.Color.White)
                    Switch(checked = p.useImperial, onCheckedChange = { viewModel.updateUseImperial(it) }, colors = SwitchDefaults.colors(checkedThumbColor = androidx.compose.ui.graphics.Color.White, checkedTrackColor = androidx.compose.ui.graphics.Color(0xFF3B82F6)))
                }
                Divider(color = androidx.compose.ui.graphics.Color(0xFF27272A), modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Dark Theme", color = androidx.compose.ui.graphics.Color.White)
                    Switch(checked = p.useDarkTheme, onCheckedChange = { viewModel.updateUseDarkTheme(it) }, colors = SwitchDefaults.colors(checkedThumbColor = androidx.compose.ui.graphics.Color.White, checkedTrackColor = androidx.compose.ui.graphics.Color(0xFF3B82F6)))
                }
                Divider(color = androidx.compose.ui.graphics.Color(0xFF27272A), modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Sounds", color = androidx.compose.ui.graphics.Color.White)
                    Switch(checked = p.soundsEnabled, onCheckedChange = { viewModel.updateSoundsEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = androidx.compose.ui.graphics.Color.White, checkedTrackColor = androidx.compose.ui.graphics.Color(0xFF3B82F6)))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("APP & DATA", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF71717A), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = { /* TODO Check for updates */ }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18181B), contentColor = androidx.compose.ui.graphics.Color.White)) {
            Text("Check for Updates")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { /* TODO Export JSON */ }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18181B), contentColor = androidx.compose.ui.graphics.Color.White)) {
            Text("Export Data (.json)")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { /* TODO Export CSV */ }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18181B), contentColor = androidx.compose.ui.graphics.Color.White)) {
            Text("Export Data (.csv)")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { /* TODO Import JSON */ }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18181B), contentColor = androidx.compose.ui.graphics.Color.White)) {
            Text("Import Data (.json)")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF450a0a)), 
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF7f1d1d)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("DANGER ZONE", color = androidx.compose.ui.graphics.Color(0xFFfca5a5), fontWeight = FontWeight.Bold, letterSpacing = 1.sp, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.deleteDeviceData() },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFdc2626)),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Delete All Device Data", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Chomp Clock v1.0", modifier = Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color(0xFF71717A))
    }
}
