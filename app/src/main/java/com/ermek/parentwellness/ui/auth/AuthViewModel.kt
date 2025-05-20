package com.ermek.parentwellness.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ermek.parentwellness.data.model.User
import com.ermek.parentwellness.data.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.SecurityException

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {
    private val TAG = "AuthViewModel"

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            // Set loading state temporarily
            _authState.value = AuthState.Loading

            try {
                repository.debugAuthInfo() // Add this debug method to AuthRepository

                if (repository.isUserLoggedIn()) {
                    val user = repository.getCurrentUser()
                    _authState.value = when {
                        user == null -> AuthState.Error("Failed to retrieve user profile.")
                        // Always go directly to authenticated state, bypassing setup completely
                        else -> AuthState.Authenticated(user)
                    }
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking current user", e)
                _authState.value = AuthState.Error("Error checking authentication state: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = repository.signIn(email, password)
                if (result.isSuccess) {
                    val user = repository.getCurrentUser()
                    _authState.value = if (user == null) {
                        AuthState.Error("Login successful but failed to retrieve user data.")
                    } else {
                        // Skip setup check and directly authenticate
                        AuthState.Authenticated(user)
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    val errorMessage = getAuthErrorMessage(exception)
                    Log.e(TAG, "Sign in failed: $errorMessage", exception)
                    _authState.value = AuthState.Error(errorMessage)
                }
            } catch (e: Exception) {
                val errorMessage = getAuthErrorMessage(e)
                Log.e(TAG, "Sign in failed with exception: $errorMessage", e)
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    fun signUp(email: String, password: String, name: String, isParent: Boolean) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = repository.signUp(email, password, name, isParent)
                if (result.isSuccess) {
                    val user = repository.getCurrentUser()
                    if (user != null) {
                        // Auto-populate required fields rather than going through setup flow
                        val updatedUser = user.copy(
                            fullName = name,
                            gender = "Prefer not to say",  // Default gender
                            birthDate = "01-01-2000"       // Default birth date
                        )

                        // Update the user profile with these default values
                        try {
                            repository.updateProfile(updatedUser)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating profile with default values", e)
                            // Continue anyway, as this is not critical
                        }

                        // Directly go to authenticated state, bypassing setup
                        _authState.value = AuthState.Authenticated(updatedUser)
                    } else {
                        _authState.value = AuthState.Error("Failed to create user profile")
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    val errorMessage = getAuthErrorMessage(exception)
                    Log.e(TAG, "Sign up failed: $errorMessage", exception)
                    _authState.value = AuthState.Error(errorMessage)
                }
            } catch (e: Exception) {
                val errorMessage = getAuthErrorMessage(e)
                Log.e(TAG, "Sign up failed with exception: $errorMessage", e)
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    fun signOut() {
        try {
            repository.signOut()
            _authState.value = AuthState.Unauthenticated
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out", e)
            _authState.value = AuthState.Error("Failed to sign out: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    fun retryAuthentication() {
        checkCurrentUser()
    }

    private fun getAuthErrorMessage(exception: Throwable?): String {
        return when (exception) {
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
            is FirebaseAuthInvalidUserException -> "User does not exist or has been disabled."
            is FirebaseAuthUserCollisionException -> "An account already exists with this email."
            is FirebaseNetworkException -> "Network error. Please check your connection."
            is SecurityException -> {
                if (exception.message?.contains("Unknown calling package") == true) {
                    "Firebase authentication configuration error. Please contact support."
                } else {
                    "Security error: ${exception.localizedMessage ?: "Unknown"}"
                }
            }
            else -> exception?.localizedMessage ?: "Authentication failed. Please try again."
        }
    }
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class NeedsSetup(val user: User) : AuthState() // Keeping this for backward compatibility
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}