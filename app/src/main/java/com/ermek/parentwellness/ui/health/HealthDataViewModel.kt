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
import com.ermek.parentwellness.data.generators.SimulatedDataGenerator
import com.ermek.parentwellness.data.model.HealthData
import com.ermek.parentwellness.data.repository.HealthRepository

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
                        TimeRange.DAY -> add(Calendar.DAY_OF_YEAR, -1)
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

    // Implement the missing load methods
    private fun loadHeartRateData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.getHeartRateReadings().collect {
                    _heartRateData.value = it
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load heart rate data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadBloodPressureData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.getBloodPressureReadings().collect {
                    _bloodPressureData.value = it
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load blood pressure data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadBloodSugarData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.getBloodSugarReadings().collect {
                    _bloodSugarData.value = it
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load blood sugar data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadStepsData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.getStepsReadings().collect {
                    _stepsData.value = it
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load steps data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Fixed generateSimulatedData function
    fun generateSimulatedData(
        metricType: String,
        profile: SimulatedDataGenerator.Companion.UserProfile,
        period: SimulatedDataGenerator.Companion.TimePeriod,
        includeAnomalies: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Create repository without Application parameter
                val healthRepository = HealthRepository()
                val result = healthRepository.generateSimulatedData(
                    metricType = metricType,
                    profile = profile,
                    period = period,
                    includeAnomalies = includeAnomalies
                )

                if (result.isSuccess) {
                    // Refresh the data to show the new simulated values
                    when (metricType) {
                        HealthData.TYPE_HEART_RATE -> loadHeartRateData()
                        HealthData.TYPE_BLOOD_PRESSURE -> loadBloodPressureData()
                        HealthData.TYPE_BLOOD_SUGAR -> loadBloodSugarData()
                        HealthData.TYPE_STEPS -> loadStepsData()
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to generate data"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Convenience methods for specific metric types
    fun generateHeartRateData(
        profile: SimulatedDataGenerator.Companion.UserProfile = SimulatedDataGenerator.Companion.UserProfile.HEALTHY,
        period: SimulatedDataGenerator.Companion.TimePeriod = SimulatedDataGenerator.Companion.TimePeriod.MONTH,
        includeAnomalies: Boolean = true
    ) {
        generateSimulatedData(
            metricType = HealthData.TYPE_HEART_RATE,
            profile = profile,
            period = period,
            includeAnomalies = includeAnomalies
        )
    }

    fun generateBloodPressureData(
        profile: SimulatedDataGenerator.Companion.UserProfile = SimulatedDataGenerator.Companion.UserProfile.HEALTHY,
        period: SimulatedDataGenerator.Companion.TimePeriod = SimulatedDataGenerator.Companion.TimePeriod.MONTH,
        includeAnomalies: Boolean = true
    ) {
        generateSimulatedData(
            metricType = HealthData.TYPE_BLOOD_PRESSURE,
            profile = profile,
            period = period,
            includeAnomalies = includeAnomalies
        )
    }

    fun generateBloodSugarData(
        profile: SimulatedDataGenerator.Companion.UserProfile = SimulatedDataGenerator.Companion.UserProfile.HEALTHY,
        period: SimulatedDataGenerator.Companion.TimePeriod = SimulatedDataGenerator.Companion.TimePeriod.MONTH,
        includeAnomalies: Boolean = true
    ) {
        generateSimulatedData(
            metricType = HealthData.TYPE_BLOOD_SUGAR,
            profile = profile,
            period = period,
            includeAnomalies = includeAnomalies
        )
    }

    fun generateStepsData(
        profile: SimulatedDataGenerator.Companion.UserProfile = SimulatedDataGenerator.Companion.UserProfile.HEALTHY,
        period: SimulatedDataGenerator.Companion.TimePeriod = SimulatedDataGenerator.Companion.TimePeriod.MONTH,
        includeAnomalies: Boolean = true
    ) {
        generateSimulatedData(
            metricType = HealthData.TYPE_STEPS,
            profile = profile,
            period = period,
            includeAnomalies = includeAnomalies
        )
    }
}