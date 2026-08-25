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
import com.example.ui.MainScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedEdgeToEdge()
        
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val userPrefs by settingsViewModel.userPrefs.collectAsState()

            MyApplicationTheme(
                darkTheme = userPrefs?.useDarkTheme ?: true
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (userPrefs != null) {
                        MainScreen(userPrefs!!)
                    }
                }
            }
        }
    }
    
    // We create a helper function for Edge to Edge manually if needed or just rely on ComponentActivity.
    private fun savedEdgeToEdge() {
        enableEdgeToEdge()
    }
}
