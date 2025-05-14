package com.ermek.parentwellness.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ermek.parentwellness.notifications.NotificationPreferences
import com.ermek.parentwellness.notifications.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationSettingsViewModel : ViewModel() {
    private val repository = NotificationRepository()

    private val _preferencesState = MutableStateFlow<NotificationSettingsState>(NotificationSettingsState.Loading)
    val preferencesState: StateFlow<NotificationSettingsState> = _preferencesState.asStateFlow()

    // Current preferences being edited
    private var currentPreferences = NotificationPreferences()

    // Load notification preferences
    fun loadNotificationPreferences() {
        viewModelScope.launch {
            _preferencesState.value = NotificationSettingsState.Loading

            try {
                val result = repository.getNotificationPreferences()

                if (result.isSuccess) {
                    val preferences = result.getOrNull() ?: NotificationPreferences()
                    currentPreferences = preferences
                    _preferencesState.value = NotificationSettingsState.Success(preferences)
                } else {
                    _preferencesState.value = NotificationSettingsState.Error(
                        result.exceptionOrNull()?.message ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                _preferencesState.value = NotificationSettingsState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Update individual settings
    fun updateHeartRateAlerts(enabled: Boolean) {
        currentPreferences = currentPreferences.copy(heartRateAlerts = enabled)
        _preferencesState.value = NotificationSettingsState.Success(currentPreferences)
    }

    fun updateBloodPressureAlerts(enabled: Boolean) {
        currentPreferences = currentPreferences.copy(bloodPressureAlerts = enabled)
        _preferencesState.value = NotificationSettingsState.Success(currentPreferences)
    }

    fun updateBloodSugarAlerts(enabled: Boolean) {
        currentPreferences = currentPreferences.copy(bloodSugarAlerts = enabled)
        _preferencesState.value = NotificationSettingsState.Success(currentPreferences)
    }

    fun updateWeeklyReports(enabled: Boolean) {
        currentPreferences = currentPreferences.copy(weeklyReports = enabled)
        _preferencesState.value = NotificationSettingsState.Success(currentPreferences)
    }

    fun updateCaregiverAlerts(enabled: Boolean) {
        currentPreferences = currentPreferences.copy(caregiverAlerts = enabled)
        _preferencesState.value = NotificationSettingsState.Success(currentPreferences)
    }

    // Save all preferences
    fun savePreferences() {
        viewModelScope.launch {
            try {
                repository.updateNotificationPreferences(currentPreferences)
                // Show success message if needed
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

// States for the notification settings screen
sealed class NotificationSettingsState {
    object Loading : NotificationSettingsState()
    data class Success(val preferences: NotificationPreferences) : NotificationSettingsState()
    data class Error(val message: String) : NotificationSettingsState()
}