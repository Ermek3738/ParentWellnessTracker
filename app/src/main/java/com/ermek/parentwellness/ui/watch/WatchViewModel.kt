package com.ermek.parentwellness.ui.watch

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ermek.parentwellness.data.samsung.SamsungHealthSensorManager
import com.ermek.parentwellness.data.samsung.SensorConnectionManager.ConnectionState
import com.ermek.parentwellness.data.samsung.listeners.HeartRateSensorListener
import com.ermek.parentwellness.data.samsung.models.HeartRateData
import com.ermek.parentwellness.data.samsung.models.StepsData
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for Watch Screen to handle Samsung Health Sensor data
 */
class WatchViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "WatchViewModel"

    // Samsung Health Sensor Manager
    private val sensorManager = SamsungHealthSensorManager(application)

    // Heart rate data state
    private val _heartRateState = MutableStateFlow<HeartRateState>(HeartRateState.Loading)
    val heartRateState: StateFlow<HeartRateState> = _heartRateState.asStateFlow()

    // Steps data state
    private val _stepsState = MutableStateFlow<StepsState>(StepsState.Loading)
    val stepsState: StateFlow<StepsState> = _stepsState.asStateFlow()

    // Connection state
        private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Initialization state
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // Init block to setup the ViewModel
    init {
        initialize()
    }

    /**
     * Initialize Samsung Health Sensor integration
     */
    private fun initialize() {
        viewModelScope.launch {
            try {
                // Initialize sensor manager
                sensorManager.initialize()

                // Observe connection state
                observeConnectionState()

                // Observe heart rate data
                observeHeartRateData()

                // Observe steps data
                observeStepsData()

                _isInitialized.value = true

                Log.d(tag, "Samsung Health Sensor integration initialized")
            } catch (e: Exception) {
                Log.e(tag, "Error initializing Samsung Health integration: ${e.message}")
                _connectionState.value = ConnectionState.Error(e)
            }
        }
    }

    /**
     * Observe connection state from the sensor manager
     */
    private fun observeConnectionState() {
        viewModelScope.launch {
            sensorManager.getConnectionManager().connectionState.collectLatest { state ->
                _connectionState.value = state

                // Update UI states based on connection state
                when (state) {
                    is ConnectionState.Connected -> {
                        Log.d(tag, "Connected to Samsung Health")

                        // Check if required sensors are available
                        checkSensorAvailability()
                    }
                    is ConnectionState.Error -> {
                        Log.e(tag, "Error connecting to Samsung Health: ${state.exception.message}")

                        // Update UI states with errors
                        _heartRateState.value = HeartRateState.Error("Connection error")
                        _stepsState.value = StepsState.Error("Connection error")
                    }
                    is ConnectionState.ResolutionRequired -> {
                        Log.w(tag, "Resolution required: ${state.exception.errorCode}")

                        // We'll handle this in the UI
                    }
                    else -> {
                        // Connecting or disconnected - keep loading state
                    }
                }
            }
        }
    }

    /**
     * Check if the required sensors are available
     */
    private fun checkSensorAvailability() {
        // Check heart rate sensor availability
        if (sensorManager.isSensorAvailable(HealthTrackerType.HEART_RATE)) {
            Log.d(tag, "Heart rate sensor is available")
        } else {
            Log.w(tag, "Heart rate sensor is not available")
            _heartRateState.value = HeartRateState.Error("Heart rate sensor not available")
        }

        // Check steps sensor availability - we use a different approach now
        // since PEDOMETER isn't directly available as a constant
        if (sensorManager.availableSensors.value.any {
                it.name.contains("STEP", ignoreCase = true) ||
                        it.name.contains("PEDOMETER", ignoreCase = true) ||
                        it == HealthTrackerType.ACCELEROMETER
            }) {
            Log.d(tag, "Steps sensor is available")
        } else {
            Log.w(tag, "Steps sensor is not available")
            _stepsState.value = StepsState.Error("Steps sensor not available")
        }
    }

    /**
     * Observe heart rate data from the sensor
     */
    private fun observeHeartRateData() {
        viewModelScope.launch {
            sensorManager.getHeartRateListener().heartRateData.collectLatest { data ->
                if (data != null) {
                    // Format timestamp
                    val date = Date(data.timestamp)
                    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val formattedTime = dateFormat.format(date)

                    // Update state with data
                    _heartRateState.value = HeartRateState.Success(
                        heartRate = data.heartRate,
                        timestamp = formattedTime,
                        isAbnormal = data.isAbnormal
                    )

                    Log.d(tag, "Heart rate data updated: ${data.heartRate} bpm")
                }
            }
        }

        // Also observe alerts
        viewModelScope.launch {
            sensorManager.getHeartRateListener().heartRateAlert.collectLatest { alert ->
                if (alert != null) {
                    // Handle alert in UI
                    when (alert) {
                        is HeartRateSensorListener.HeartRateAlert.HighHeartRate -> {
                            Log.w(tag, "High heart rate alert: ${alert.data.heartRate} bpm")
                            // Update UI with alert
                        }
                        is HeartRateSensorListener.HeartRateAlert.LowHeartRate -> {
                            Log.w(tag, "Low heart rate alert: ${alert.data.heartRate} bpm")
                            // Update UI with alert
                        }
                    }
                }
            }
        }
    }

    /**
     * Observe steps data from the sensor
     */
    private fun observeStepsData() {
        viewModelScope.launch {
            sensorManager.getStepsListener().stepsData.collectLatest { data ->
                if (data != null) {
                    // Format timestamp
                    val date = Date(data.timestamp)
                    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val formattedTime = dateFormat.format(date)

                    // Update state with data
                    _stepsState.value = StepsState.Success(
                        steps = data.stepCount,
                        dailyGoal = data.dailyGoal,
                        progress = data.getProgressPercentage(),
                        timestamp = formattedTime
                    )

                    Log.d(tag, "Steps data updated: ${data.stepCount} steps")
                }
            }
        }

        // Also observe inactivity alerts
        viewModelScope.launch {
            sensorManager.getStepsListener().inactivityAlert.collectLatest { alert ->
                if (alert != null) {
                    Log.w(tag, "Inactivity alert: ${alert.inactiveDurationMinutes} minutes")
                    // Update UI with alert
                }
            }
        }
    }

    /**
     * Manually refresh watch data
     */
    fun refreshWatchData() {
        viewModelScope.launch {
            try {
                Log.d(tag, "Manually refreshing watch data")
                sensorManager.refreshAllSensorData()
            } catch (e: Exception) {
                Log.e(tag, "Error refreshing watch data: ${e.message}")
            }
        }
    }

    /**
     * Resolve Samsung Health connection issues
     * (This should be called from UI with an activity context)
     */
    fun resolveConnectionIssue(activity: android.app.Activity) {
        val currentState = _connectionState.value
        if (currentState is ConnectionState.ResolutionRequired) {
            try {
                currentState.exception.resolve(activity)
            } catch (e: Exception) {
                Log.e(tag, "Error resolving connection issue: ${e.message}")
            }
        }
    }

    /**
     * Clean up resources when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        sensorManager.cleanup()
    }

    /**
     * Heart rate state for UI
     */
    sealed class HeartRateState {
        object Loading : HeartRateState()
        data class Success(
            val heartRate: Int,
            val timestamp: String,
            val isAbnormal: Boolean = false
        ) : HeartRateState()
        data class Error(val message: String) : HeartRateState()
    }

    /**
     * Steps state for UI
     */
    sealed class StepsState {
        object Loading : StepsState()
        data class Success(
            val steps: Int,
            val dailyGoal: Int,
            val progress: Int,
            val timestamp: String
        ) : StepsState()
        data class Error(val message: String) : StepsState()
    }
}