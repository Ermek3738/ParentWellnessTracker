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
                        // Add more specific checks for setup completion
                        user.fullName.isBlank() || user.gender.isBlank() || user.birthDate.isBlank() ->
                            AuthState.NeedsSetup(user)
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
                    _authState.value = when {
                        user == null -> AuthState.Error("Login successful but failed to retrieve user data.")
                        user.fullName.isBlank() || user.gender.isBlank() || user.birthDate.isBlank() ->
                            AuthState.NeedsSetup(user)
                        else -> AuthState.Authenticated(user)
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
                    val userId = repository.getCurrentUserId() ?: ""
                    val setupUser = user ?: User(
                        id = userId,
                        email = email,
                        fullName = name,
                        isParent = isParent,
                        caregiverIds = emptyList(),
                        parentIds = emptyList()
                    )
                    _authState.value = AuthState.NeedsSetup(setupUser)
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
    data class NeedsSetup(val user: User) : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}