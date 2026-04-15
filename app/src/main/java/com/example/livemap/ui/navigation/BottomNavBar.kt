package com.example.livemap.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

// -------------------------------------------------------
// Data class that holds the info for each tab in the
// bottom bar: which route to navigate to, what icon
// to show, and what label to display.
// -------------------------------------------------------
data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun BottomNavBar(
    currentRoute: String?,           // Which tab is currently active
    onItemSelected: (String) -> Unit // Callback when user taps a tab
) {
    // Define the 3 tabs for our skeleton
    val items = listOf(
        BottomNavItem(
            route = NavRoutes.Map.route,
            icon = Icons.Default.Place,
            label = "Map"
        ),
        BottomNavItem(
            route = NavRoutes.Search.route,
            icon = Icons.Default.Search,
            label = "Search"
        ),
        BottomNavItem(
            route = NavRoutes.Profile.route,
            icon = Icons.Default.Person,
            label = "Profile"
        )
    )

    // NavigationBar is the Material 3 component for bottom nav
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onItemSelected(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}