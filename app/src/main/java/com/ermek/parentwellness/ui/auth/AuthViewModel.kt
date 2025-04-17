package com.ermek.parentwellness.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ermek.parentwellness.data.model.User
import com.ermek.parentwellness.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            // Set loading state temporarily
            _authState.value = AuthState.Loading

            if (repository.isUserLoggedIn()) {
                try {
                    val user = repository.getCurrentUser()
                    _authState.value = when {
                        user == null -> AuthState.Unauthenticated
                        // Add more specific checks for setup completion
                        user.fullName.isBlank() || user.gender.isBlank() || user.birthDate.isBlank() ->
                            AuthState.NeedsSetup(user)
                        else -> AuthState.Authenticated(user)
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error checking current user", e)
                    _authState.value = AuthState.Unauthenticated
                }
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.signIn(email, password)
                .onSuccess { firebaseUser ->
                    val user = repository.getCurrentUser()
                    _authState.value = when {
                        user == null -> AuthState.Unauthenticated
                        user.fullName.isBlank() || user.gender.isBlank() || user.birthDate.isBlank() ->
                            AuthState.NeedsSetup(user)
                        else -> AuthState.Authenticated(user)
                    }
                }
                .onFailure { error ->
                    Log.e("AuthViewModel", "Sign in failed", error)
                    _authState.value = AuthState.Unauthenticated
                }
        }
    }

    fun signUp(email: String, password: String, name: String, isParent: Boolean) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.signUp(email, password, name, isParent)
                .onSuccess { firebaseUser ->
                    val user = repository.getCurrentUser()
                    _authState.value = AuthState.NeedsSetup(user ?: User(
                        id = firebaseUser.uid,
                        email = email,
                        fullName = name,
                        isParent = isParent
                    ))
                }
                .onFailure { error ->
                    Log.e("AuthViewModel", "Sign up failed", error)
                    _authState.value = AuthState.Unauthenticated
                }
        }
    }

    fun signOut() {
        repository.signOut()
        _authState.value = AuthState.Unauthenticated
    }
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class NeedsSetup(val user: User) : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}