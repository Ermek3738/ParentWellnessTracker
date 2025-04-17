package com.ermek.parentwellness.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ermek.parentwellness.data.model.User
import com.ermek.parentwellness.data.repository.AuthRepository
import com.ermek.parentwellness.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {
    private val TAG = "ProfileViewModel"

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadCurrentUser()
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d(TAG, "Loading current user data")
                // Get the current user directly from AuthRepository
                val user = authRepository.getCurrentUser()

                if (user != null) {
                    Log.d(TAG, "User loaded successfully: ${user.fullName}")
                    _currentUser.value = user
                } else {
                    Log.w(TAG, "User not signed in or profile not found")
                    _error.value = "User not signed in or profile not found"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user data", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUserProfile(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d(TAG, "Updating user profile: ${user.fullName}")
                // Use the repository to update user profile
                val result = profileRepository.saveUserProfile(user)

                if (result.isSuccess) {
                    // Update current user state with new data
                    _currentUser.value = result.getOrNull()
                    Log.d(TAG, "User profile updated successfully")
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Failed to update profile", exception)
                    _error.value = exception?.message ?: "Failed to update profile"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user profile", e)
                _error.value = e.message ?: "Failed to update profile"
            } finally {
                _isLoading.value = false
            }
        }
    }
}