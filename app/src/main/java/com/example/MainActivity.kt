package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.UserPreferences
import com.example.data.repository.WeightUnit
import com.example.ui.MainScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val userPrefs by settingsViewModel.userPrefs.collectAsState()
            val activePrefs = userPrefs ?: UserPreferences(
                username = "User",
                heightCm = 170f,
                gender = "Male",
                weightUnit = WeightUnit.KG,
                useImperial = false,
                useDarkTheme = true,
                soundsEnabled = true,
                avatarId = "icon:🔥"
            )

            MyApplicationTheme(
                darkTheme = activePrefs.useDarkTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(activePrefs)
                }
            }
        }
    }
}

