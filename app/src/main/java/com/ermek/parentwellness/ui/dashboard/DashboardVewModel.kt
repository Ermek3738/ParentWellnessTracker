package com.ermek.parentwellness.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ermek.parentwellness.data.model.HealthData
import com.ermek.parentwellness.data.model.User
import com.ermek.parentwellness.data.repository.AuthRepository
import com.ermek.parentwellness.data.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val healthRepository: HealthRepository = HealthRepository()
) : ViewModel() {

    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val dashboardState: StateFlow<DashboardState> = _dashboardState

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _healthData = MutableStateFlow<List<HealthData>>(emptyList())
    val healthData: StateFlow<List<HealthData>> = _healthData

    init {
        loadUserData()
        loadHealthData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    _user.value = currentUser
                } else {
                    _dashboardState.value = DashboardState.Error("Failed to load user data")
                }
            } catch (e: Exception) {
                _dashboardState.value = DashboardState.Error(e.message ?: "An error occurred")
            }
        }
    }

    private fun loadHealthData() {
        viewModelScope.launch {
            _dashboardState.value = DashboardState.Loading
            healthRepository.getLatestHealthData()
                .onSuccess { data ->
                    _healthData.value = data
                    _dashboardState.value = DashboardState.Success
                }
                .onFailure {
                    _dashboardState.value = DashboardState.Error(it.message ?: "Failed to load health data")
                }
        }
    }

    fun refreshData() {
        loadHealthData()
    }

    fun signOut() {
        authRepository.signOut()
    }
}

sealed class DashboardState {
    data object Loading : DashboardState()
    data object Success : DashboardState()
    data class Error(val message: String) : DashboardState()
}