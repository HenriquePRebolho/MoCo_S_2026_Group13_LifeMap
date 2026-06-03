package com.example.livemap.data.repository

/**
 * State of the currently active auth form (login or register).
 *
 * Idle       → nothing happening, form is editable
 * Submitting → request in flight, UI should disable button + show spinner
 * Error      → last attempt failed, show errorMessage and let user retry (class not object)
 * Success    → terminal state for the form, UI will navigate away and authState will transition to Authenticated.
 */
sealed class FormState {
    data object Idle : FormState()
    data object Submitting : FormState()
    data class Error(val message: String) : FormState()
    data object Success : FormState()
}