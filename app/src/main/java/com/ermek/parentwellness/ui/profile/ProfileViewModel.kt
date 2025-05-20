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
    private val tag = "ProfileViewModel"  // Fixed: Changed from TAG to tag (lowercase)

    // Current user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Profile updated state
    private val _profileUpdated = MutableStateFlow(false)
    val profileUpdated: StateFlow<Boolean> = _profileUpdated.asStateFlow()

    init {
        loadCurrentUser()
    }

    fun loadCurrentUser(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Reset profile updated state when loading user
            if (forceRefresh) {
                _profileUpdated.value = false
            }

            try {
                Log.d(tag, "Loading current user data${if (forceRefresh) " (forced)" else ""}")
                // Get the current user directly from AuthRepository
                val user = authRepository.getCurrentUser()

                if (user != null) {
                    Log.d(tag, "User loaded successfully: ${user.fullName}")
                    _currentUser.value = user
                } else {
                    Log.w(tag, "User not signed in or profile not found")
                    _error.value = "User not signed in or profile not found"
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading user data", e)
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
            _profileUpdated.value = false // Reset the state before updating

            try {
                Log.d(tag, "Updating user profile: ${user.fullName}")

                // First try with AuthRepository (using updateProfile method we added)
                val authResult = authRepository.updateProfile(user)  // Fixed: Changed from updateUserProfile to updateProfile

                if (authResult.isSuccess) {
                    // Update current user state with new data
                    _currentUser.value = authResult.getOrNull()
                    _profileUpdated.value = true // Mark as updated
                    Log.d(tag, "User profile updated successfully via AuthRepository")
                } else {
                    // Fall back to ProfileRepository
                    val result = profileRepository.saveUserProfile(user)

                    if (result.isSuccess) {
                        // Update current user state with new data
                        _currentUser.value = result.getOrNull()
                        _profileUpdated.value = true // Mark as updated
                        Log.d(tag, "User profile updated successfully via ProfileRepository")
                    } else {
                        val exception = result.exceptionOrNull()
                        Log.e(tag, "Failed to update profile", exception)
                        _error.value = exception?.message ?: "Failed to update profile"
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error updating user profile", e)
                _error.value = e.message ?: "Failed to update profile"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Reset the profile updated state - useful when navigating away or when you need to reset
    fun resetProfileUpdatedState() {
        _profileUpdated.value = false
    }
}