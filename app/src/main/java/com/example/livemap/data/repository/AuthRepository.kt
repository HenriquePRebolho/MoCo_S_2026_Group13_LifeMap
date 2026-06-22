package com.example.livemap.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Handles database connections for authentication (register/login/logout).
 *
 * Responsibilities:
 *   - Sign users up / in / out.
 *   - Expose the currently authenticated user as a Flow that emits whenever
 *     the auth state changes (login, logout, token refresh).
 */

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /**
     * Emits the current FirebaseUser whenever auth state changes:
     * -  `null` when no user is signed in.
     * -   FirebaseUser when someone signs in.
     */
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            // trySend never blocks, if the channel is full or closed it drops the emission.
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)

        // Suspend here until the collector cancels. Then clean up.
        awaitClose { auth.removeAuthStateListener(listener) }
    }


    // Returns the currently signed-in user synchronously, or null.
    fun currentUser(): FirebaseUser? = auth.currentUser

    // Creates a new account with email + password and signs the user in.
    // Returns: Result.success(uid) on success | Result.failure(exception) on failure.
    suspend fun signUp(email: String, password: String): Result<String> = runCatching {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        // After successful sign-up, currentUser is non-null and contains the new uid.
        authResult.user?.uid
            ?: error("Sign-up succeeded but user is null — unexpected state")
    }

    // Signs in an existing user.
    // Returns: Result.success(uid) on success | Result.failure(exception) on failure.
    suspend fun signIn(email: String, password: String): Result<String> = runCatching {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        authResult.user?.uid
            ?: error("Sign-in succeeded but user is null — unexpected state")
    }

    // Signs the current user out.
    fun signOut() {
        auth.signOut()
    }

    /**
     * Permanently deletes the currently authenticated Firebase user.
     *
     * This removes the account from Firebase Authentication,
     * meaning the user will no longer be able to sign in.
     *
     */
    suspend fun deleteCurrentUser(): Result<Unit> = runCatching {
        auth.currentUser?.delete()?.await()
            ?: error("No authenticated user found")
    }

    /**
     * Sends a password reset email to the specified address.
     *
     * Firebase generates the reset link automatically and sends
     * the email using its built-in password recovery flow.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }
}