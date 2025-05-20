package com.ermek.parentwellness.ui.dashboard

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ermek.parentwellness.data.model.User
import com.ermek.parentwellness.data.repository.AuthRepository
import com.ermek.parentwellness.data.repository.HealthDataRepository
import com.ermek.parentwellness.data.samsung.SamsungHealthSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val sensorManager = SamsungHealthSensorManager(getApplication())

    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val dashboardState: StateFlow<DashboardState> = _dashboardState

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate

    private val _stepCount = MutableStateFlow<Int?>(null)
    val stepCount: StateFlow<Int?> = _stepCount

    init {
        loadUserData()
        initializeHealthSensors()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                _user.value = currentUser
                _dashboardState.value = DashboardState.Success
            } catch (e: Exception) {
                _dashboardState.value = DashboardState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    private fun initializeHealthSensors() {
        viewModelScope.launch {
            try {
                sensorManager.initialize()

                sensorManager.getHeartRateListener().heartRateData.collect { data ->
                    _heartRate.value = data?.heartRate
                }

                sensorManager.getStepsListener().stepsData.collect { data ->
                    _stepCount.value = data?.stepCount
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun updateDashboardWithSimulatedData() {
        viewModelScope.launch {
            try {
                // Create health data repository
                val healthRepository = HealthDataRepository(getApplication())

                // Get the latest heart rate data
                try {
                    val heartRateList = healthRepository.getHeartRateReadings().first()
                    if (heartRateList.isNotEmpty()) {
                        // Get the most recent heart rate reading
                        val latestHeartRate = heartRateList.maxByOrNull { it.timestamp }
                        latestHeartRate?.let {
                            _heartRate.value = it.heartRate
                            Log.d("DashboardViewModel", "Updated heart rate to ${it.heartRate}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DashboardViewModel", "Error updating heart rate", e)
                }

                // Get the latest steps data
                try {
                    val stepsList = healthRepository.getStepsReadings().first()
                    if (stepsList.isNotEmpty()) {
                        // Get the most recent steps reading
                        val latestSteps = stepsList.maxByOrNull { it.timestamp }
                        latestSteps?.let {
                            _stepCount.value = it.steps
                            Log.d("DashboardViewModel", "Updated steps to ${it.steps}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DashboardViewModel", "Error updating steps", e)
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error updating dashboard with simulated data", e)
            }
        }
    }

    fun isDemoModeEnabled(): Boolean {
        val prefs = getApplication<android.app.Application>().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("demo_mode_enabled", false)
    }

    fun setDemoModeEnabled(enabled: Boolean) {
        getApplication<android.app.Application>().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("demo_mode_enabled", enabled)
            .apply()
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                sensorManager.refreshAllSensorData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    override fun onCleared() {
        sensorManager.cleanup()
        super.onCleared()
    }
}

sealed class DashboardState {
    data object Loading : DashboardState()
    data object Success : DashboardState()
    data class Error(val message: String) : DashboardState()
}