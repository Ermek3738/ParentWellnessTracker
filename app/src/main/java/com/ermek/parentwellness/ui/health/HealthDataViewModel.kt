package com.ermek.parentwellness.ui.health

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ermek.parentwellness.ui.health.TimeRange
import com.ermek.parentwellness.data.repository.HealthDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class HealthDataViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HealthDataRepository(application.applicationContext)

    // Heart Rate
    private val _heartRateData = MutableStateFlow<List<com.ermek.parentwellness.data.repository.HeartRateData>>(emptyList())
    val heartRateData: StateFlow<List<com.ermek.parentwellness.data.repository.HeartRateData>> = _heartRateData.asStateFlow()

    // Blood Pressure
    private val _bloodPressureData = MutableStateFlow<List<com.ermek.parentwellness.data.repository.BloodPressureData>>(emptyList())
    val bloodPressureData: StateFlow<List<com.ermek.parentwellness.data.repository.BloodPressureData>> = _bloodPressureData.asStateFlow()

    // Blood Sugar
    private val _bloodSugarData = MutableStateFlow<List<com.ermek.parentwellness.data.repository.BloodSugarData>>(emptyList())
    val bloodSugarData: StateFlow<List<com.ermek.parentwellness.data.repository.BloodSugarData>> = _bloodSugarData.asStateFlow()

    // Steps
    private val _stepsData = MutableStateFlow<List<com.ermek.parentwellness.data.repository.StepsData>>(emptyList())
    val stepsData: StateFlow<List<com.ermek.parentwellness.data.repository.StepsData>> = _stepsData.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Initialize data sync
        repository.initializeSync()

        // Load all health data
        loadAllHealthData()
    }

    private fun loadAllHealthData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Fetch remote data first
                repository.fetchAndSyncRemoteData()

                // Then collect local data
                viewModelScope.launch {
                    repository.getHeartRateReadings().collect {
                        _heartRateData.value = it
                    }
                }

                viewModelScope.launch {
                    repository.getBloodPressureReadings().collect {
                        _bloodPressureData.value = it
                    }
                }

                viewModelScope.launch {
                    repository.getBloodSugarReadings().collect {
                        _bloodSugarData.value = it
                    }
                }

                viewModelScope.launch {
                    repository.getStepsReadings().collect {
                        _stepsData.value = it
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Load data by time range (day, week, month, year)
    fun loadDataByTimeRange(timeRange: TimeRange) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val calendar = Calendar.getInstance()
                val endTime = calendar.timeInMillis

                calendar.apply {
                    when (timeRange) {
                        TimeRange.DAY -> add(Calendar.DAY_OF_YEAR, -1)  // Add case for DAY
                        TimeRange.WEEK -> add(Calendar.DAY_OF_YEAR, -7)
                        TimeRange.MONTH -> add(Calendar.MONTH, -1)
                        TimeRange.YEAR -> add(Calendar.YEAR, -1)
                    }
                }

                val startTime = calendar.timeInMillis

                // Rest of your code remains unchanged
                viewModelScope.launch {
                    repository.getHeartRateReadingsByTimeRange(startTime, endTime).collect {
                        _heartRateData.value = it
                    }
                }

                // Other data collections remain the same...
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add new health data entries
    fun addHeartRateReading(heartRate: Int, isResting: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.addHeartRateReading(heartRate, isResting)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add heart rate reading"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addBloodPressureReading(systolic: Int, diastolic: Int, pulse: Int? = null, situation: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.addBloodPressureReading(systolic, diastolic, pulse, situation)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add blood pressure reading"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addBloodSugarReading(value: Int, situation: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.addBloodSugarReading(value, situation)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add blood sugar reading"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addStepsReading(steps: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.addStepsReading(steps)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add steps reading"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Refresh data
    fun refreshData() {
        loadAllHealthData()
    }
}

