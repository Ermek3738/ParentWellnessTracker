package com.ermek.parentwellness.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ermek.parentwellness.notifications.Alert
import com.ermek.parentwellness.notifications.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlertsViewModel : ViewModel() {
    private val repository = NotificationRepository()

    private val _alertsState = MutableStateFlow<AlertsState>(AlertsState.Loading)
    val alertsState: StateFlow<AlertsState> = _alertsState.asStateFlow()

    // Load alerts from repository
    fun loadAlerts() {
        viewModelScope.launch {
            _alertsState.value = AlertsState.Loading

            try {
                val result = repository.getAlerts()

                if (result.isSuccess) {
                    _alertsState.value = AlertsState.Success(result.getOrNull() ?: emptyList())
                } else {
                    _alertsState.value = AlertsState.Error(
                        result.exceptionOrNull()?.message ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                _alertsState.value = AlertsState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Mark alert as read
    fun markAlertAsRead(alertId: String) {
        viewModelScope.launch {
            try {
                repository.markAlertAsRead(alertId)

                // Update the alerts list
                val currentState = _alertsState.value
                if (currentState is AlertsState.Success) {
                    // Update the alert in the list
                    val updatedAlerts = currentState.alerts.map {
                        if (it.id == alertId) it.copy(read = true) else it
                    }

                    _alertsState.value = AlertsState.Success(updatedAlerts)
                }
            } catch (e: Exception) {
                // Handle error silently, or show a toast
            }
        }
    }
}

// States for the alerts screen
sealed class AlertsState {
    object Loading : AlertsState()
    data class Success(val alerts: List<Alert>) : AlertsState()
    data class Error(val message: String) : AlertsState()
}