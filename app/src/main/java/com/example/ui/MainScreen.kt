package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.UserPreferences
import com.example.ui.fasting.FastingScreen
import com.example.ui.food.FoodScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.stats.StatsScreen
import com.example.ui.theme.AppTheme
import com.example.ui.weight.WeightScreen

@Composable
fun MainScreen(
    userPrefs: UserPreferences
) {
    val navController = rememberNavController()
    
    Scaffold(
        containerColor = AppTheme.colors.background,
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "fasting",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("fasting") {
                FastingScreen(userPrefs = userPrefs)
            }
            composable("weight") {
                WeightScreen(userPrefs = userPrefs)
            }
            composable("food") {
                FoodScreen(userPrefs = userPrefs)
            }
            composable("stats") {
                StatsScreen(userPrefs = userPrefs)
            }
            composable("settings") {
                SettingsScreen(userPrefs = userPrefs)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        NavigationItem("Fasting", "fasting", Icons.Default.Timer),
        NavigationItem("Weight", "weight", Icons.Default.MonitorWeight),
        NavigationItem("Food", "food", Icons.Default.Restaurant),
        NavigationItem("Stats", "stats", Icons.Default.BarChart),
        NavigationItem("Settings", "settings", Icons.Default.Settings)
    )

    Surface(
        color = AppTheme.colors.surface,
        shadowElevation = if (AppTheme.colors.isDark) 8.dp else 4.dp,
        border = BorderStroke(1.dp, AppTheme.colors.border)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = AppTheme.colors.textSecondary
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
                    selected = isSelected,
                    onClick = {
                        navController.navigate(item.route) {
                            navController.graph.startDestinationRoute?.let { route ->
                                popUpTo(route) { saveState = true }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppTheme.colors.primary,
                        selectedTextColor = AppTheme.colors.primary,
                        indicatorColor = AppTheme.colors.primary.copy(alpha = if (AppTheme.colors.isDark) 0.18f else 0.12f),
                        unselectedIconColor = AppTheme.colors.textMuted,
                        unselectedTextColor = AppTheme.colors.textMuted
                    )
                )
            }
        }
    }
}

data class NavigationItem(val title: String, val route: String, val icon: ImageVector)
