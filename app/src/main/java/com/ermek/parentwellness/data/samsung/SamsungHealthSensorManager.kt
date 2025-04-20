package com.ermek.parentwellness.data.samsung

import android.content.Context
import android.util.Log
import com.ermek.parentwellness.data.samsung.listeners.HeartRateSensorListener
import com.ermek.parentwellness.data.samsung.listeners.StepsSensorListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.os.Build

class SamsungHealthSensorManager(private val context: Context) {
    private val tag = "SamsungHealthSensorManager"

    // Connection manager
    private val connectionManager = SensorConnectionManager(context)

    // Sensor listeners
    private val heartRateListener = HeartRateSensorListener()
    private val stepsListener = StepsSensorListener()

    // Trackers
    private var heartRateTracker: HealthTracker? = null
    private var stepsTracker: HealthTracker? = null

    // Available sensor types
    private val _availableSensors = MutableStateFlow<List<HealthTrackerType>>(emptyList())
    val availableSensors: StateFlow<List<HealthTrackerType>> = _availableSensors.asStateFlow()

    fun initialize() {
        Log.d(tag, "SDK Initialization Started")
        Log.d(tag, "Device Info: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.d(tag, "Android Version: ${Build.VERSION.RELEASE}")

        connectionManager.initialize()

        connectionManager.connectionState.let { state ->
            if (state.value is SensorConnectionManager.ConnectionState.Connected) {
                discoverAvailableSensors()
            }
        }
    }

    private fun discoverAvailableSensors() {
        try {
            val trackingService = connectionManager.getHealthTrackingService() ?: return
            val capability = trackingService.trackingCapability

            val sdkVersion = capability.version
            Log.d(tag, "Samsung Health Sensor SDK Version: $sdkVersion")

            val supportedTypes = capability.supportHealthTrackerTypes
            Log.d(tag, "Available sensors: $supportedTypes")
            _availableSensors.value = supportedTypes

            val hasHeartRateSensor = supportedTypes.contains(HealthTrackerType.HEART_RATE)
            val stepsTrackerType = findStepsTrackerType(supportedTypes)

            if (hasHeartRateSensor) {
                initializeHeartRateTracker()
            } else {
                Log.w(tag, "Heart rate sensor not available on this device")
            }

            if (stepsTrackerType != null) {
                initializeStepsTracker(stepsTrackerType)
            } else {
                Log.w(tag, "Steps sensor not available on this device")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error discovering sensors", e)
        }
    }

    private fun findStepsTrackerType(supportedTypes: List<HealthTrackerType>): HealthTrackerType? {
        supportedTypes.forEach { type ->
            Log.d(tag, "Available tracker type: ${type.name}")
        }

        return supportedTypes.find {
            it.name.contains("STEP", ignoreCase = true) ||
                    it.name.contains("PEDOMETER", ignoreCase = true) ||
                    it == HealthTrackerType.ACCELEROMETER
        }
    }

    private fun initializeHeartRateTracker() {
        try {
            val trackingService = connectionManager.getHealthTrackingService() ?: return

            heartRateTracker = trackingService.getHealthTracker(HealthTrackerType.HEART_RATE)
            Log.d(tag, "Heart rate tracker class: ${heartRateTracker?.javaClass?.name}")

            heartRateTracker?.let { tracker ->
                heartRateListener.initialize(tracker)
                Log.d(tag, "Heart rate tracker initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error initializing heart rate tracker", e)
        }
    }

    private fun initializeStepsTracker(trackerType: HealthTrackerType) {
        try {
            val trackingService = connectionManager.getHealthTrackingService() ?: return

            Log.d(tag, "Initializing steps tracker with type: ${trackerType.name}")

            stepsTracker = trackingService.getHealthTracker(trackerType)
            Log.d(tag, "Steps tracker class: ${stepsTracker?.javaClass?.name}")

            stepsTracker?.let { tracker ->
                stepsListener.initialize(tracker)
                Log.d(tag, "Steps tracker initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error initializing steps tracker", e)
        }
    }

    fun getHeartRateListener(): HeartRateSensorListener = heartRateListener
    fun getStepsListener(): StepsSensorListener = stepsListener
    fun getConnectionManager(): SensorConnectionManager = connectionManager

    fun refreshAllSensorData() {
        Log.d(tag, "Requesting refresh of all sensor data")
        heartRateListener.refreshHeartRateData()
        stepsListener.refreshStepsData()
    }

    fun isSensorAvailable(type: HealthTrackerType): Boolean =
        availableSensors.value.contains(type)

    fun getSdkVersion(): String = try {
        connectionManager.getHealthTrackingService()
            ?.trackingCapability?.version ?: "Unknown"
    } catch (e: Exception) {
        Log.e(tag, "Error getting SDK version", e)
        "Error"
    }

    fun cleanup() {
        Log.d(tag, "Cleaning up Samsung Health Sensor Manager resources")
        heartRateListener.cleanup()
        stepsListener.cleanup()
        connectionManager.disconnectFromService()
    }
}