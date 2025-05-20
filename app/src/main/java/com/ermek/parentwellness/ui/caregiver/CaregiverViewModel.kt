package com.ermek.parentwellness.ui.caregiver

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

class CaregiverViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {
    private val tag = "CaregiverViewModel"

    private val _caregivers = MutableStateFlow<List<User>>(emptyList())
    val caregivers: StateFlow<List<User>> = _caregivers.asStateFlow()

    private val _parents = MutableStateFlow<List<User>>(emptyList())
    val parents: StateFlow<List<User>> = _parents.asStateFlow()

    private val _selectedParent = MutableStateFlow<User?>(null)
    val selectedParent: StateFlow<User?> = _selectedParent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Parent health data
    private val _parentHeartRate = MutableStateFlow<Int?>(null)
    val parentHeartRate: StateFlow<Int?> = _parentHeartRate.asStateFlow()

    private val _parentBloodPressure = MutableStateFlow<String?>(null)
    val parentBloodPressure: StateFlow<String?> = _parentBloodPressure.asStateFlow()

    private val _parentBloodSugar = MutableStateFlow<Int?>(null)
    val parentBloodSugar: StateFlow<Int?> = _parentBloodSugar.asStateFlow()

    private val _parentSteps = MutableStateFlow<Int?>(null)
    val parentSteps: StateFlow<Int?> = _parentSteps.asStateFlow()

    fun clearError() {
        _error.value = null
    }
    fun loadCaregiversForCurrentUser() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _error.value = "User not logged in"
                    return@launch
                }

                // Ensure user is a parent
                if (!currentUser.isParent) {
                    _error.value = "You are not in parent mode. Please switch to parent mode."
                    return@launch
                }

                // Get caregivers for this parent
                val result = profileRepository.getCaregiversForParent(currentUser.id)

                if (result.isSuccess) {
                    _caregivers.value = result.getOrNull() ?: emptyList()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Error loading caregivers"
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading caregivers", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add a caregiver by email (for parents)
    fun addCaregiverByEmail(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _error.value = "User not logged in"
                    _isLoading.value = false
                    return@launch
                }

                // Check if the current user is a parent
                if (!currentUser.isParent) {
                    _error.value = "Only parents can add caregivers"
                    _isLoading.value = false
                    return@launch
                }

                Log.d(tag, "Finding user with email: $email to add as caregiver")

                // Find user by email
                val userResult = profileRepository.findUserByEmail(email)

                if (userResult.isFailure) {
                    val exception = userResult.exceptionOrNull()
                    Log.e(tag, "Error finding user: ${exception?.message}")
                    _error.value = exception?.message ?: "Error finding user"
                    _isLoading.value = false
                    return@launch
                }

                val caregiver = userResult.getOrNull()
                if (caregiver == null) {
                    Log.e(tag, "No user found with this email address: $email")
                    _error.value = "No user found with this email address"
                    _isLoading.value = false
                    return@launch
                }

                // Check if this person is already a caregiver
                if (currentUser.caregiverIds.contains(caregiver.id)) {
                    Log.e(tag, "This user is already your caregiver: ${caregiver.id}")
                    _error.value = "This user is already your caregiver"
                    _isLoading.value = false
                    return@launch
                }

                Log.d(tag, "Linking caregiver ${caregiver.id} to parent ${currentUser.id}")

                // Link the caregiver to the parent
                val result = profileRepository.linkCaregiverToParent(currentUser.id, caregiver.id)

                if (result.isSuccess) {
                    Log.d(tag, "Successfully linked caregiver to parent")
                    // Reload caregivers list
                    loadCaregiversForCurrentUser()
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(tag, "Error adding caregiver: ${exception?.message}")
                    _error.value = exception?.message ?: "Error adding caregiver"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(tag, "Exception adding caregiver", e)
                _error.value = e.message ?: "Unknown error occurred"
                _isLoading.value = false
            }
        }
    }

    // Unlink a caregiver (for parents)
    fun unlinkCaregiver(caregiverId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _error.value = "User not logged in"
                    return@launch
                }

                // Unlink the caregiver from the parent
                val result = profileRepository.unlinkCaregiverFromParent(currentUser.id, caregiverId)

                if (result.isSuccess) {
                    // Reload caregivers list
                    loadCaregiversForCurrentUser()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Error removing caregiver"
                }
            } catch (e: Exception) {
                Log.e(tag, "Error unlinking caregiver", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Load parents for the current user (assuming they're a caregiver)
    fun loadParentsForCurrentUser() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _error.value = "User not logged in"
                    return@launch
                }

                // Ensure user is a caregiver (not a parent)
                if (currentUser.isParent && currentUser.parentIds.isEmpty()) {
                    _error.value = "You are in parent mode. Please switch to caregiver mode."
                    return@launch
                }

                // Get parents for this caregiver
                val result = profileRepository.getParentsForCaregiver(currentUser.id)

                if (result.isSuccess) {
                    val parentsList = result.getOrNull() ?: emptyList()
                    _parents.value = parentsList

                    // Auto-select the first parent if available
                    if (parentsList.isNotEmpty() && _selectedParent.value == null) {
                        _selectedParent.value = parentsList.first()
                        loadParentHealthData(parentsList.first().id)
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Error loading parents"
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading parents", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add a parent by email (for caregivers)
    fun addParentByEmail(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _error.value = "User not logged in"
                    _isLoading.value = false
                    return@launch
                }

                // Check if the current user is a caregiver (not a parent)
                if (currentUser.isParent) {
                    _error.value = "Only caregivers can add parents"
                    _isLoading.value = false
                    return@launch
                }

                Log.d(tag, "Finding user with email: $email to add as parent")

                // Find user by email
                val userResult = profileRepository.findUserByEmail(email)

                if (userResult.isFailure) {
                    val exception = userResult.exceptionOrNull()
                    Log.e(tag, "Error finding user: ${exception?.message}")
                    _error.value = exception?.message ?: "Error finding user"
                    _isLoading.value = false
                    return@launch
                }

                val parent = userResult.getOrNull()
                if (parent == null) {
                    Log.e(tag, "No user found with this email address: $email")
                    _error.value = "No user found with this email address"
                    _isLoading.value = false
                    return@launch
                }

                // Check if user is a parent
                if (!parent.isParent) {
                    Log.e(tag, "This user is not registered as a parent: ${parent.id}")
                    _error.value = "This user is not registered as a parent"
                    _isLoading.value = false
                    return@launch
                }

                // Check if user is already added as a parent
                if (currentUser.parentIds.contains(parent.id)) {
                    Log.e(tag, "This parent is already added to your list: ${parent.id}")
                    _error.value = "This parent is already added to your list"
                    _isLoading.value = false
                    return@launch
                }

                Log.d(tag, "Linking caregiver ${currentUser.id} to parent ${parent.id}")

                // Link the parent to the caregiver
                val result = profileRepository.linkCaregiverToParent(parent.id, currentUser.id)

                if (result.isSuccess) {
                    Log.d(tag, "Successfully linked caregiver to parent")
                    // Reload parents list
                    loadParentsForCurrentUser()
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(tag, "Error adding parent: ${exception?.message}")
                    _error.value = exception?.message ?: "Error adding parent"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(tag, "Exception adding parent", e)
                _error.value = e.message ?: "Unknown error occurred"
                _isLoading.value = false
            }
        }
    }

    // Unlink a parent (for caregivers)
    fun unlinkParent(parentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _error.value = "User not logged in"
                    return@launch
                }

                // Unlink the parent from the caregiver
                val result = profileRepository.unlinkCaregiverFromParent(parentId, currentUser.id)

                if (result.isSuccess) {
                    // Reload parents list
                    loadParentsForCurrentUser()

                    // Reset selected parent if it was the one removed
                    if (_selectedParent.value?.id == parentId) {
                        _selectedParent.value = _parents.value.firstOrNull()
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Error removing parent"
                }
            } catch (e: Exception) {
                Log.e(tag, "Error unlinking parent", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Select a parent to view (for caregivers)
    fun selectParent(parent: User) {
        _selectedParent.value = parent

        // Load the selected parent's health data
        loadParentHealthData(parent.id)
    }

    // Load health data for the selected parent
    private fun loadParentHealthData(parentId: String) {
        // In a real app, you would fetch the actual health data from a database or API
        // For this example, we'll just simulate data with randomly generated values
        viewModelScope.launch {
            try {
                // These would normally be retrieved from Firestore or a health data API
                _parentHeartRate.value = (60..100).random()
                _parentBloodPressure.value = "${(110..140).random()}/${(70..90).random()}"
                _parentBloodSugar.value = (80..120).random()
                _parentSteps.value = (2000..10000).random()
            } catch (e: Exception) {
                Log.e(tag, "Error loading parent health data", e)
            }
        }
    }

    // Refresh the health data for the selected parent
    fun refreshParentHealthData() {
        _selectedParent.value?.let { parent ->
            loadParentHealthData(parent.id)
        }
    }
}