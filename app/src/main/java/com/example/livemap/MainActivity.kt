package com.example.livemap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.livemap.ui.navigation.BottomNavBar
import com.example.livemap.ui.navigation.NavRoutes
import com.example.livemap.ui.screens.MapScreen
import com.example.livemap.ui.screens.ProfileScreen
import com.example.livemap.ui.screens.SearchScreen
import com.example.livemap.ui.theme.LiveMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiveMapTheme {
                LifeMapApp()
            }
        }
    }
}

@Composable
fun LifeMapApp() {
    // navController keeps track of which screen we're on
    // and handles the back stack (like browser history)
    val navController = rememberNavController()

    // Observe the current route so the bottom bar
    // knows which tab to highlight
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Scaffold provides the standard Material 3 layout:
    // top bar (optional), bottom bar, and content area
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onItemSelected = { route ->
                    navController.navigate(route) {
                        // Pop up to the start destination to avoid
                        // building up a huge stack of screens
                        popUpTo(NavRoutes.Map.route) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same screen
                        launchSingleTop = true
                        // Restore state when re-selecting a tab
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        // NavHost is the container that swaps screens
        // based on the current route
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Map.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Each composable() call registers a screen
            // for a specific route string
            composable(NavRoutes.Map.route) { MapScreen() }
            composable(NavRoutes.Search.route) { SearchScreen() }
            composable(NavRoutes.Profile.route) { ProfileScreen() }
        }
    }
}