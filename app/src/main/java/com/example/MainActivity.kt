package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.ThemeMode
import com.example.data.repository.UserPreferences
import com.example.data.repository.WeightUnit
import com.example.ui.MainScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.util.WeightReminderManager

class MainActivity : ComponentActivity() {

    private val navTargetState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WeightReminderManager.createNotificationChannel(this)
        navTargetState.value = intent?.getStringExtra(WeightReminderManager.EXTRA_NAV_TARGET)
        
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val userPrefs by settingsViewModel.userPrefs.collectAsState()
            val activePrefs = userPrefs ?: UserPreferences(
                username = "User",
                heightCm = 170f,
                gender = "Male",
                weightUnit = WeightUnit.KG,
                useImperial = false,
                themeMode = ThemeMode.DARK,
                useDarkTheme = true,
                soundsEnabled = true,
                avatarId = "icon:🔥"
            )

            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (activePrefs.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemDark
            }

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                dynamicColor = activePrefs.dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        userPrefs = activePrefs,
                        initialNavTarget = navTargetState.value,
                        onNavTargetConsumed = { navTargetState.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val target = intent.getStringExtra(WeightReminderManager.EXTRA_NAV_TARGET)
        if (target != null) {
            navTargetState.value = target
        }
    }
}

