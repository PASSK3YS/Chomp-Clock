package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
                FastingScreen(username = userPrefs.username)
            }
            composable("weight") {
                WeightScreen(
                    username = userPrefs.username,
                    heightCm = userPrefs.heightCm,
                    gender = userPrefs.gender,
                    useImperial = userPrefs.useImperial
                )
            }
            composable("food") {
                FoodScreen(username = userPrefs.username)
            }
            composable("stats") {
                StatsScreen(username = userPrefs.username)
            }
            composable("settings") {
                SettingsScreen()
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

    androidx.compose.material3.Surface(
        color = androidx.compose.ui.graphics.Color(0xFF0A0A0A),
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, androidx.compose.ui.graphics.Color(0xFF18181B)
        )
    ) {
        NavigationBar(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = androidx.compose.ui.graphics.Color(0xFFA1A1AA) // Zinc400
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
                        selectedIconColor = androidx.compose.ui.graphics.Color(0xFF60A5FA), // Blue400
                        selectedTextColor = androidx.compose.ui.graphics.Color(0xFF60A5FA),
                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF71717A), // Zinc500
                        unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF71717A)
                    )
                )
            }
        }
    }
}

data class NavigationItem(val title: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
