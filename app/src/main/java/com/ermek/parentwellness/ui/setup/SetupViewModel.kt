package com.ermek.parentwellness.ui.setup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ermek.parentwellness.data.model.User
import com.ermek.parentwellness.data.repository.AuthRepository
import com.ermek.parentwellness.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SetupViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {
    private val TAG = "SetupViewModel"

    private val _setupState = MutableStateFlow<SetupState>(SetupState.Initial)
    val setupState: StateFlow<SetupState> = _setupState

    // Current profile being built
    private val _user = MutableStateFlow(User())
    val user: StateFlow<User> = _user

    // Current step in the setup process
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep

    // Total steps in the setup process
    private val _totalSteps = MutableStateFlow(3) // Name, Gender, Birthday
    val totalSteps: StateFlow<Int> = _totalSteps

    init {
        // Initialize with authenticated user data
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    Log.d(TAG, "Initializing setup with user: ${currentUser.email}")
                    _user.value = currentUser
                } else {
                    // If we couldn't get the user from authRepository, try to create a basic one
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    auth.currentUser?.let { firebaseUser ->
                        _user.value = User(
                            id = firebaseUser.uid,
                            email = firebaseUser.email ?: ""
                        )
                        Log.d(TAG, "Created basic user for setup: ${_user.value.email}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing user for setup", e)
            }
        }
    }

    // Update user profile field
    fun updateProfile(update: (User) -> User) {
        val updatedUser = update(_user.value)
        Log.d(TAG, "Updating profile: ${updatedUser.fullName}, ${updatedUser.gender}, ${updatedUser.birthDate}")
        _user.value = updatedUser
    }

    // Move to next step
    fun nextStep() {
        if (_currentStep.value < _totalSteps.value) {
            _currentStep.value++
        }
    }

    // Move to previous step
    fun previousStep() {
        if (_currentStep.value > 1) {
            _currentStep.value--
        }
    }

    // Complete the setup process
    fun completeSetup() {
        _setupState.value = SetupState.Loading
        viewModelScope.launch {
            try {
                Log.d(TAG, "Completing setup with user data: ${_user.value.fullName}, ${_user.value.email}")

                // Ensure we have all the data we need
                if (_user.value.fullName.isEmpty()) {
                    Log.w(TAG, "User name is empty, using email as fallback")
                    _user.value = _user.value.copy(
                        fullName = _user.value.email.substringBefore('@')
                    )
                }

                val result = profileRepository.saveUserProfile(_user.value)
                result.onSuccess {
                    Log.d(TAG, "Setup completed successfully. User data saved: ${it.fullName}")
                    _setupState.value = SetupState.Completed
                }.onFailure {
                    Log.e(TAG, "Failed to complete setup", it)
                    _setupState.value = SetupState.Error(it.message ?: "Failed to save profile")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during setup completion", e)
                _setupState.value = SetupState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }
}

sealed class SetupState {
    data object Initial : SetupState()
    data object Loading : SetupState()
    data object Completed : SetupState()
    data class Error(val message: String) : SetupState()
}