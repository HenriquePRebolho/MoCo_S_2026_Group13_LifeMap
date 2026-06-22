package com.example.livemap.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.KeyboardOptions
import com.example.livemap.data.repository.FormState

/**
 * Login screen - logs into an existing account.
 *
 * State sources:
 *   - Local: email, password (held in remember-mutableStateOf).
 *   - From AuthViewModel: formState (Submitting/Error/Success).
 *
 * Side effects:
 *   - When formState becomes Success, the parent navigation (MainActivity) will swap to the main app because authState turns Authenticated.
 *   - When the screen is left, the screen goes back to Idle state.
 */
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
    onNavigateToForgotPassword: () -> Unit,
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    // Local form state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Reset the form when the screen is first composed.
    LaunchedEffect(Unit) {
        viewModel.resetForm()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "LifeMap",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Sign in to your account",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            // Disable input while a request is in flight.
            enabled = formState !is FormState.Submitting,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = formState !is FormState.Submitting,
            modifier = Modifier.fillMaxWidth()
        )

        // Show error inline if the last attempt failed.
        if (formState is FormState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (formState as FormState.Error).message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(email, password) },
            enabled = formState !is FormState.Submitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (formState is FormState.Submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Sign in")
            }
        }

        TextButton(
            onClick = onNavigateToForgotPassword
        ) {
            Text("Forgot your password?")
        }

        Spacer(modifier = Modifier.height(16.dp))



        TextButton(onClick = onNavigateToRegister) {
            Text("Don't have an account? Sign up")
        }
    }
}