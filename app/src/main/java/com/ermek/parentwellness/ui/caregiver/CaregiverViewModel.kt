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
    private val TAG = "CaregiverViewModel"

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

    // Load caregivers for the current user (assuming they're a parent)
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
                    _error.value = "Only parents can have caregivers"
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
                Log.e(TAG, "Error loading caregivers", e)
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
                if (currentUser.isParent) {
                    _error.value = "Only caregivers can have parents"
                    return@launch
                }

                // Get parents for this caregiver
                val result = profileRepository.getParentsForCaregiver(currentUser.id)

                if (result.isSuccess) {
                    _parents.value = result.getOrNull() ?: emptyList()

                    // Auto-select the first parent if available
                    if (_parents.value.isNotEmpty() && _selectedParent.value == null) {
                        _selectedParent.value = _parents.value.first()
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Error loading parents"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading parents", e)
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
                    return@launch
                }

                // Find user by email
                val userResult = profileRepository.findUserByEmail(email)

                if (userResult.isFailure) {
                    _error.value = userResult.exceptionOrNull()?.message ?: "Error finding user"
                    return@launch
                }

                val caregiver = userResult.getOrNull()
                if (caregiver == null) {
                    _error.value = "No user found with this email address"
                    return@launch
                }

                // Check if user is already a caregiver
                if (_caregivers.value.any { it.id == caregiver.id }) {
                    _error.value = "This user is already your caregiver"
                    return@launch
                }

                // Link the caregiver to the parent
                val result = profileRepository.linkCaregiverToParent(currentUser.id, caregiver.id)

                if (result.isSuccess) {
                    // Reload caregivers list
                    loadCaregiversForCurrentUser()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Error adding caregiver"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding caregiver", e)
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
                    return@launch
                }

                // Find user by email
                val userResult = profileRepository.findUserByEmail(email)

                if (userResult.isFailure) {
                    _error.value = userResult.exceptionOrNull()?.message ?: "Error finding user"
                    return@launch
                }

                val parent = userResult.getOrNull()
                if (parent == null) {
                    _error.value = "No user found with this email address"
                    return@launch
                }

                // Check if user is a parent
                if (!parent.isParent) {
                    _error.value = "This user is not registered as a parent"
                    return@launch
                }

                // Check if user is already added as a parent
                if (_parents.value.any { it.id == parent.id }) {
                    _error.value = "This parent is already added to your list"
                    return@launch
                }

                // Link the parent to the caregiver
                val result = profileRepository.linkCaregiverToParent(parent.id, currentUser.id)

                if (result.isSuccess) {
                    // Reload parents list
                    loadParentsForCurrentUser()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Error adding parent"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding parent", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
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
                Log.e(TAG, "Error unlinking caregiver", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
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
                Log.e(TAG, "Error unlinking parent", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Select a parent to view (for caregivers)
    fun selectParent(parent: User) {
        _selectedParent.value = parent
    }
}