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
import com.example.ui.weight.WeightScreen

@Composable
fun MainScreen(
    userPrefs: UserPreferences
) {
    val navController = rememberNavController()
    
    Scaffold(
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
        color = Color(0xFF0A0A0A),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFF18181B))
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = Color(0xFFA1A1AA)
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            items.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
                    selected = currentRoute == item.route,
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
                        selectedIconColor = Color(0xFF60A5FA),
                        selectedTextColor = Color(0xFF60A5FA),
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color(0xFF71717A),
                        unselectedTextColor = Color(0xFF71717A)
                    )
                )
            }
        }
    }
}

data class NavigationItem(val title: String, val route: String, val icon: ImageVector)
