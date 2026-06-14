package com.example.livemap.ui.auth

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Hosts the Login and Register screens with navigation between them.
 *
 * MainActivity decides whether to show this or the main app NavHost (NavHost is only displayed when the user is NOT authenticated).
 */
@Composable
fun AuthNavHost() {
    val navController = rememberNavController()

    // Single shared AuthViewModel for both Login and Register.
    val authViewModel: AuthViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate("register") {
                        // Don't pile up Login → Register → Login → Register - replaces instead.
                        popUpTo("login") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                viewModel = authViewModel
            )
        }
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack(
                        route = "login",
                        inclusive = false
                    )
                },
                viewModel = authViewModel
            )
        }
    }
}