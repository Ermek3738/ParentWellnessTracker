package com.ermek.parentwellness.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ermek.parentwellness.data.model.User
import com.ermek.parentwellness.data.repository.AuthRepository
import com.ermek.parentwellness.data.samsung.SamsungHealthSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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