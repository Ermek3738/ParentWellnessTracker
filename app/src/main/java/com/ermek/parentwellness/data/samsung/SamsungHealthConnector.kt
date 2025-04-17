package com.ermek.parentwellness.data.samsung

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.samsung.android.sdk.healthdata.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Connection status for Samsung Health SDK
 */
enum class SamsungHealthConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    CONNECTION_FAILED,
    PERMISSIONS_REQUIRED
}

/**
 * Main connector class for Samsung Health SDK integration
 */
class SamsungHealthConnector(private val context: Context) {
    companion object {
        private const val TAG = "SamsungHealthConnector"
        private const val SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth"

        // Samsung Health data types we're interested in
        val HEALTH_DATA_TYPES = setOf(
            HealthConstants.StepCount.HEALTH_DATA_TYPE,
            HealthConstants.HeartRate.HEALTH_DATA_TYPE,
            HealthConstants.BloodPressure.HEALTH_DATA_TYPE,
            HealthConstants.BloodGlucose.HEALTH_DATA_TYPE
        )
    }

    // Connection client for Samsung Health
    private lateinit var healthDataStore: HealthDataStore

    // State flow for connection status
    private val _connectionStatus = MutableStateFlow(SamsungHealthConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<SamsungHealthConnectionStatus> = _connectionStatus.asStateFlow()

    // Error message for detailed error reporting
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Permission Manager for handling health data permissions
    private lateinit var permissionManager: HealthPermissionManager

    /**
     * Check if Samsung Health app is installed
     */
    private fun isSamsungHealthInstalled(): Boolean {
        try {
            val packageInfo = context.packageManager.getPackageInfo(SAMSUNG_HEALTH_PACKAGE, 0)
            Log.d(TAG, "Samsung Health found: $SAMSUNG_HEALTH_PACKAGE, version: ${packageInfo.versionName}")
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Samsung Health app ($SAMSUNG_HEALTH_PACKAGE) is not installed", e)
            return false
        }
    }

    /**
     * Initialize the Samsung Health connection
     */
    fun initialize() {
        try {
            Log.d(TAG, "=== Samsung Health initialization starting ===")

            // First check if Samsung Health is installed
            if (!isSamsungHealthInstalled()) {
                Log.e(TAG, "Cannot initialize: Samsung Health not found")
                _connectionStatus.value = SamsungHealthConnectionStatus.CONNECTION_FAILED
                return
            }

            // Create a new instance of HealthDataStore
            Log.d(TAG, "Creating HealthDataStore instance...")
            healthDataStore = HealthDataStore(context, connectionListener)
            Log.d(TAG, "HealthDataStore created successfully")

            // Initialize permission manager
            Log.d(TAG, "Initializing permission manager...")
            permissionManager = HealthPermissionManager(healthDataStore)
            Log.d(TAG, "Permission manager initialized")

            // Connect to the store
            _connectionStatus.value = SamsungHealthConnectionStatus.CONNECTING
            Log.d(TAG, "Calling healthDataStore.connectService()...")
            healthDataStore.connectService()
            Log.d(TAG, "connectService() call completed")

        } catch (exception: Exception) {
            Log.e(TAG, "Error initializing Samsung Health: ${exception.message}", exception)
            _connectionStatus.value = SamsungHealthConnectionStatus.CONNECTION_FAILED
            _errorMessage.value = "Failed to initialize: ${exception.message}"
        }
    }

    /**
     * Listener for connection events with Samsung Health
     */
    private val connectionListener = object : HealthDataStore.ConnectionListener {
        override fun onConnected() {
            Log.d(TAG, "Connected to Samsung Health")
            _connectionStatus.value = SamsungHealthConnectionStatus.CONNECTED
            _errorMessage.value = null

            // Check permissions after connection
            CoroutineScope(Dispatchers.Default).launch {
                val hasPermissions = permissionManager.checkPermissions(HEALTH_DATA_TYPES)
                if (!hasPermissions) {
                    Log.d(TAG, "Permissions required for Samsung Health data")
                    _connectionStatus.value = SamsungHealthConnectionStatus.PERMISSIONS_REQUIRED
                    _errorMessage.value = "Health permissions required"
                } else {
                    Log.d(TAG, "All permissions granted for Samsung Health data")
                }
            }
        }

        override fun onConnectionFailed(error: HealthConnectionErrorResult) {
            val errorCode = error.errorCode
            val errorMsg = when (errorCode) {
                HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED ->
                    "Samsung Health platform not installed"
                HealthConnectionErrorResult.OLD_VERSION_PLATFORM ->
                    "Old version of Samsung Health platform"
                HealthConnectionErrorResult.PLATFORM_DISABLED ->
                    "Samsung Health platform disabled"
                HealthConnectionErrorResult.USER_AGREEMENT_NEEDED ->
                    "User agreement needed for Samsung Health"
                else -> "Unknown error: $errorCode"
            }

            Log.e(TAG, "Failed to connect to Samsung Health: $errorMsg")
            _connectionStatus.value = SamsungHealthConnectionStatus.CONNECTION_FAILED
            _errorMessage.value = errorMsg
        }

        override fun onDisconnected() {
            Log.d(TAG, "Disconnected from Samsung Health")
            _connectionStatus.value = SamsungHealthConnectionStatus.DISCONNECTED
            _errorMessage.value = "Disconnected from Samsung Health"
        }
    }

    /**
     * Request necessary permissions for Samsung Health data
     * @return Result of permission request
     */
    suspend fun requestPermissions(): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            if (!this::healthDataStore.isInitialized) {
                Log.e(TAG, "Health data store not initialized")
                _errorMessage.value = "Health data store not initialized"
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            if (_connectionStatus.value != SamsungHealthConnectionStatus.PERMISSIONS_REQUIRED &&
                _connectionStatus.value != SamsungHealthConnectionStatus.CONNECTED) {
                Log.e(TAG, "Cannot request permissions in current state: ${_connectionStatus.value}")
                _errorMessage.value = "Cannot request permissions in current state"
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            // Convert to main thread operation
            permissionManager.requestPermissions(HEALTH_DATA_TYPES) { success ->
                // This will be called on the main thread
                if (success) {
                    Log.d(TAG, "Permissions granted successfully")
                    _connectionStatus.value = SamsungHealthConnectionStatus.CONNECTED
                    _errorMessage.value = null
                    continuation.resume(true)
                } else {
                    Log.e(TAG, "Permission request denied by user")
                    _errorMessage.value = "Permission request denied by user"
                    continuation.resume(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting permissions: ${e.message}", e)
            _errorMessage.value = "Error requesting permissions: ${e.message}"
            continuation.resumeWithException(e)
        }
    }

    /**
     * Get the health data reader for accessing Samsung Health data
     */
    fun getHealthDataReader(): HealthDataReader? {
        return if (this::healthDataStore.isInitialized &&
            _connectionStatus.value == SamsungHealthConnectionStatus.CONNECTED) {
            HealthDataReader(healthDataStore)
        } else {
            Log.e(TAG, "Cannot get health data reader, connection status: ${_connectionStatus.value}")
            _errorMessage.value = "Cannot access health data, connection status: ${_connectionStatus.value}"
            null
        }
    }

    /**
     * Disconnect from Samsung Health when no longer needed
     */
    fun disconnect() {
        if (this::healthDataStore.isInitialized) {
            healthDataStore.disconnectService()
            Log.d(TAG, "Disconnected from Samsung Health service")
        }
    }

}