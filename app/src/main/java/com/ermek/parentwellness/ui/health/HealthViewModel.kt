package com.ermek.parentwellness.ui.health

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ermek.parentwellness.data.model.HealthData
import com.ermek.parentwellness.data.repository.HealthRepository
import com.ermek.parentwellness.ui.components.HealthDataEntry
import com.ermek.parentwellness.ui.components.MetricType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class HealthViewModel(
    private val repository: HealthRepository = HealthRepository()
) : ViewModel() {
    private val TAG = "HealthViewModel"

    // State flows for each metric type
    private val _heartRateData = MutableStateFlow<List<HealthData>>(emptyList())
    val heartRateData: StateFlow<List<HealthData>> = _heartRateData.asStateFlow()

    private val _bloodPressureData = MutableStateFlow<List<HealthData>>(emptyList())
    val bloodPressureData: StateFlow<List<HealthData>> = _bloodPressureData.asStateFlow()

    private val _bloodSugarData = MutableStateFlow<List<HealthData>>(emptyList())
    val bloodSugarData: StateFlow<List<HealthData>> = _bloodSugarData.asStateFlow()

    private val _stepsData = MutableStateFlow<List<HealthData>>(emptyList())
    val stepsData: StateFlow<List<HealthData>> = _stepsData.asStateFlow()

    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Selected date range for filtering
    private val _startDate = MutableStateFlow<Long>(getStartOfWeek())
    val startDate: StateFlow<Long> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long>(System.currentTimeMillis())
    val endDate: StateFlow<Long> = _endDate.asStateFlow()

    // Initialize by loading data
    init {
        loadAllHealthData()
    }

    /**
     * Load all types of health data
     */
    fun loadAllHealthData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            loadHealthData(HealthData.TYPE_HEART_RATE)
            loadHealthData(HealthData.TYPE_BLOOD_PRESSURE)
            loadHealthData(HealthData.TYPE_BLOOD_SUGAR)
            loadHealthData(HealthData.TYPE_STEPS)

            _isLoading.value = false
        }
    }

    /**
     * Load health data for a specific metric type
     */
    fun loadHealthData(metricType: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading $metricType data")

                val result = repository.getHealthDataByType(metricType)

                if (result.isSuccess) {
                    val data = result.getOrNull() ?: emptyList()

                    // Update the appropriate state flow
                    when (metricType) {
                        HealthData.TYPE_HEART_RATE -> _heartRateData.value = data
                        HealthData.TYPE_BLOOD_PRESSURE -> _bloodPressureData.value = data
                        HealthData.TYPE_BLOOD_SUGAR -> _bloodSugarData.value = data
                        HealthData.TYPE_STEPS -> _stepsData.value = data
                    }

                    Log.d(TAG, "Loaded ${data.size} $metricType records")
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Error loading $metricType data: ${exception?.message}")
                    _error.value = exception?.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading $metricType data", e)
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    /**
     * Load health data for the selected date range
     */
    fun loadHealthDataByDateRange(metricType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d(
                    TAG,
                    "Loading $metricType data for date range: ${_startDate.value} - ${_endDate.value}"
                )

                val result = repository.getHealthDataByDateRange(
                    metricType,
                    _startDate.value,
                    _endDate.value
                )

                if (result.isSuccess) {
                    val data = result.getOrNull() ?: emptyList()

                    // Update the appropriate state flow
                    when (metricType) {
                        HealthData.TYPE_HEART_RATE -> _heartRateData.value = data
                        HealthData.TYPE_BLOOD_PRESSURE -> _bloodPressureData.value = data
                        HealthData.TYPE_BLOOD_SUGAR -> _bloodSugarData.value = data
                        HealthData.TYPE_STEPS -> _stepsData.value = data
                    }

                    Log.d(TAG, "Loaded ${data.size} $metricType records for date range")
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(
                        TAG,
                        "Error loading $metricType data for date range: ${exception?.message}"
                    )
                    _error.value = exception?.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading $metricType data for date range", e)
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Save a health data entry
     */
    fun saveHealthData(entry: HealthDataEntry) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d(TAG, "Saving health data entry: ${entry.metricType}")

                val result = repository.saveHealthData(entry)

                if (result.isSuccess) {
                    Log.d(TAG, "Health data saved successfully")

                    // Reload the appropriate data type
                    when (entry.metricType) {
                        MetricType.HEART_RATE -> loadHealthData(HealthData.TYPE_HEART_RATE)
                        MetricType.BLOOD_PRESSURE -> loadHealthData(HealthData.TYPE_BLOOD_PRESSURE)
                        MetricType.BLOOD_SUGAR -> loadHealthData(HealthData.TYPE_BLOOD_SUGAR)
                        MetricType.STEPS -> loadHealthData(HealthData.TYPE_STEPS)
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Error saving health data: ${exception?.message}")
                    _error.value = exception?.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception saving health data", e)
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a health data entry
     */
    fun deleteHealthData(id: String, metricType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d(TAG, "Deleting health data: $id")

                val result = repository.deleteHealthData(id)

                if (result.isSuccess) {
                    Log.d(TAG, "Health data deleted successfully")

                    // Reload the appropriate data type
                    loadHealthData(metricType)
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Error deleting health data: ${exception?.message}")
                    _error.value = exception?.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting health data", e)
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Generate simulated data for testing
     */
    fun generateSimulatedData(metricType: String, count: Int = 10) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d(TAG, "Generating simulated $metricType data")

                val result = repository.generateSimulatedData(metricType, count)

                if (result.isSuccess) {
                    Log.d(TAG, "Simulated data generated successfully")

                    // Reload the appropriate data type
                    loadHealthData(metricType)
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Error generating simulated data: ${exception?.message}")
                    _error.value = exception?.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception generating simulated data", e)
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Set date range for filtering
     */
    fun setDateRange(start: Long, end: Long) {
        _startDate.value = start
        _endDate.value = end
    }

    /**
     * Set date range to current week
     */
    fun setCurrentWeek() {
        _startDate.value = getStartOfWeek()
        _endDate.value = System.currentTimeMillis()
    }

    /**
     * Set date range to current month
     */
    fun setCurrentMonth() {
        _startDate.value = getStartOfMonth()
        _endDate.value = System.currentTimeMillis()
    }

    /**
     * Set date range to last 3 months
     */
    fun setLast3Months() {
        _startDate.value = getStartOfMonth(monthsAgo = 3)
        _endDate.value = System.currentTimeMillis()
    }

    /**
     * Set date range to current year
     */
    fun setCurrentYear() {
        _startDate.value = getStartOfYear()
        _endDate.value = System.currentTimeMillis()
    }

    /**
     * Helper to get start of current week
     */
    private fun getStartOfWeek(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Helper to get start of current month
     */
    private fun getStartOfMonth(monthsAgo: Int = 0): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -monthsAgo)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Helper to get start of current year
     */
    private fun getStartOfYear(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}