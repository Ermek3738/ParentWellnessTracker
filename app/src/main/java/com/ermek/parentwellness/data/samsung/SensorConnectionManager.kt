package com.ermek.parentwellness.data.samsung

import android.content.Context
import android.util.Log
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager class for handling connections to Samsung Health Sensor SDK
 */
class SensorConnectionManager(private val context: Context) {

    private val TAG = "SensorConnectionManager"

    // Health tracking service for Samsung Health SDK
    private var healthTrackingService: HealthTrackingService? = null

    // Connection state flow
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Initialize the health tracking service
    fun initialize() {
        // Connection listener for Samsung Health SDK
        val connectionListener = object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.d(TAG, "Samsung Health connection successful")
                _connectionState.value = ConnectionState.Connected
            }

            override fun onConnectionEnded() {
                Log.d(TAG, "Samsung Health connection ended")
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun onConnectionFailed(exception: HealthTrackerException) {
                Log.e(TAG, "Samsung Health connection failed: ${exception.message}, code: ${exception.errorCode}")
                _connectionState.value = ConnectionState.Error(exception)

                // Handle resolution if possible
                if (exception.hasResolution()) {
                    Log.d(TAG, "Resolution available for error")
                    _connectionState.value = ConnectionState.ResolutionRequired(exception)
                }
            }
        }

        // Create the health tracking service
        healthTrackingService = HealthTrackingService(connectionListener, context)

        // Connect to the service
        connectToService()
    }

    // Connect to Samsung Health service
    fun connectToService() {
        _connectionState.value = ConnectionState.Connecting
        try {
            healthTrackingService?.connectService() ?: run {
                _connectionState.value = ConnectionState.Error(
                    HealthTrackerException("HealthTrackingService is null")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Samsung Health: ${e.message}")
            _connectionState.value = ConnectionState.Error(
                e as? HealthTrackerException ?: HealthTrackerException(e.message ?: "Unknown error")
            )
        }
    }

    // Disconnect from Samsung Health service
    fun disconnectFromService() {
        try {
            healthTrackingService?.disconnectService()
            _connectionState.value = ConnectionState.Disconnected
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from Samsung Health: ${e.message}")
        }
    }

    // Get the health tracking service instance
    fun getHealthTrackingService(): HealthTrackingService? {
        return healthTrackingService
    }

    // Sealed class for connection states
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val exception: Exception) : ConnectionState()
        data class ResolutionRequired(val exception: HealthTrackerException) : ConnectionState()
    }
}