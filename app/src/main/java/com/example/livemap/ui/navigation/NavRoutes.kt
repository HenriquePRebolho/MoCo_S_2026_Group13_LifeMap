package com.example.livemap.ui.navigation

// Sealed class that defines all the navigation routes in the app.
// Each object represents one screen.
sealed class NavRoutes(val route: String){
    object Map: NavRoutes("map")
    object Search: NavRoutes("search")
    object Profile: NavRoutes("profile")
}