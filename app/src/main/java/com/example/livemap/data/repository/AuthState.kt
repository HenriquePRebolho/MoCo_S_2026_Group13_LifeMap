package com.example.livemap.data.repository

// Represents the current authentication state of the app.
// Sealed class so the compiler enforces exhaustive when() handling everywhere we react to auth state.
sealed class AuthState {
    // Loading state - no checks in the backend yet
    data object Loading : AuthState()
    // User is signed in | carry the UID
    data class Authenticated(val uid: String) : AuthState()
    // No user is signed in | show login/register screen
    data object Unauthenticated : AuthState()
}