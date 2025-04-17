package com.ermek.parentwellness.ui.watch

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.ermek.parentwellness.data.model.WatchData
import com.ermek.parentwellness.data.repository.WatchRepository
import com.ermek.parentwellness.data.samsung.SamsungHealthConnectionStatus
import com.ermek.parentwellness.data.worker.HealthSyncWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.content.Intent
import kotlinx.coroutines.delay
/**
 * UI state for watch screen
 */
data class WatchUiState(
    val isLoading: Boolean = false,
    val watchData: WatchData = WatchData.empty(),
    val connectionStatus: String = "Disconnected",
    val error: String? = null,
    val permissionsGranted: Boolean = false,
    val diagnosticInfo: String? = null
)

/**
 * ViewModel for Watch screen
 */
class WatchViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "WatchViewModel"

        // WorkManager sync constants
        private const val SYNC_WORK_NAME = "health_sync_work"
        private const val SYNC_INTERVAL_MINUTES = 30L // 30 minutes
    }

    // Watch repository
    private val watchRepository = WatchRepository(application)

    // UI state
    private val _uiState = MutableStateFlow(WatchUiState(isLoading = true))
    val uiState: StateFlow<WatchUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "Initializing WatchViewModel")
        initializeSamsungHealth()
        observeConnectionStatus()
        observeWatchData()
        observeErrorMessages()
    }

    /**
     * Initialize Samsung Health connection
     */
    private fun initializeSamsungHealth() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Initializing Samsung Health connection")
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        diagnosticInfo = "Initializing Samsung Health connection..."
                    )
                }

                watchRepository.initialize()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Samsung Health: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to initialize Samsung Health: ${e.message}",
                        diagnosticInfo = "Exception: ${e.message}\n${e.stackTraceToString()}"
                    )
                }
            }
        }
    }

    /**
     * Observe connection status changes
     */
    private fun observeConnectionStatus() {
        viewModelScope.launch {
            watchRepository.connectionStatus.collect { status ->
                Log.d(TAG, "Connection status changed: $status")

                val statusText = when (status) {
                    SamsungHealthConnectionStatus.DISCONNECTED -> "Disconnected"
                    SamsungHealthConnectionStatus.CONNECTING -> "Connecting..."
                    SamsungHealthConnectionStatus.CONNECTED -> "Connected"
                    SamsungHealthConnectionStatus.CONNECTION_FAILED -> "Connection Failed"
                    SamsungHealthConnectionStatus.PERMISSIONS_REQUIRED -> "Permissions Required"
                }

                _uiState.update {
                    it.copy(
                        connectionStatus = statusText,
                        diagnosticInfo = "Connection status: $statusText"
                    )
                }

                // Update permissions status based on connection status
                if (status == SamsungHealthConnectionStatus.CONNECTED) {
                    _uiState.update { it.copy(permissionsGranted = true) }
                    checkPermissionsAndRefresh()
                } else if (status == SamsungHealthConnectionStatus.PERMISSIONS_REQUIRED) {
                    _uiState.update { it.copy(permissionsGranted = false) }
                }

                // Schedule periodic sync when connected and permissions granted
                if (status == SamsungHealthConnectionStatus.CONNECTED &&
                    uiState.value.permissionsGranted) {
                    schedulePeriodicSync()
                }
            }
        }
    }

    /**
     * Observe error messages from the repository
     */
    private fun observeErrorMessages() {
        viewModelScope.launch {
            watchRepository.errorMessages.collect { errorMsg ->
                if (errorMsg != null) {
                    Log.e(TAG, "Error from repository: $errorMsg")
                    _uiState.update {
                        it.copy(
                            error = errorMsg,
                            diagnosticInfo = "Error message: $errorMsg"
                        )
                    }
                }
            }
        }
    }

    /**
     * Observe watch data changes
     */
    private fun observeWatchData() {
        viewModelScope.launch {
            watchRepository.watchData.collect { data ->
                Log.d(TAG, "Watch data updated: $data")
                _uiState.update {
                    it.copy(
                        watchData = data,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Request Samsung Health permissions
     */
    fun requestPermissions() {
        viewModelScope.launch {
            Log.d(TAG, "Requesting Samsung Health permissions")
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    diagnosticInfo = "Requesting permissions..."
                )
            }

            try {
                val granted = watchRepository.requestPermissions()
                Log.d(TAG, "Permission request result: $granted")

                _uiState.update {
                    it.copy(
                        permissionsGranted = granted,
                        isLoading = false,
                        error = if (!granted) "Permission denied by user" else null,
                        diagnosticInfo = "Permission request result: $granted"
                    )
                }

                if (granted) {
                    refreshWatchData()
                    schedulePeriodicSync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting permissions: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to request permissions: ${e.message}",
                        diagnosticInfo = "Exception during permission request: ${e.message}\n${e.stackTraceToString()}"
                    )
                }
            }
        }
    }

    /**
     * Check permissions and refresh watch data if granted
     */
    private fun checkPermissionsAndRefresh() {
        viewModelScope.launch {
            Log.d(TAG, "Checking permissions and refreshing data")

            when (watchRepository.connectionStatus.value) {
                SamsungHealthConnectionStatus.PERMISSIONS_REQUIRED -> {
                    Log.d(TAG, "Permissions required")
                    _uiState.update {
                        it.copy(
                            permissionsGranted = false,
                            diagnosticInfo = "Permissions check result: required"
                        )
                    }
                }
                SamsungHealthConnectionStatus.CONNECTED -> {
                    Log.d(TAG, "Connected and permissions granted")
                    _uiState.update {
                        it.copy(
                            permissionsGranted = true,
                            diagnosticInfo = "Permissions check result: granted"
                        )
                    }
                    refreshWatchData()
                }
                else -> {
                    Log.d(TAG, "Connection status not ready for data: ${watchRepository.connectionStatus.value}")
                    // Do nothing for other states
                }
            }
        }
    }

    /**
     * Refresh watch data from Samsung Health
     * @param forceRefresh Whether to force a refresh ignoring cache
     */
    fun refreshWatchData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            Log.d(TAG, "Refreshing watch data, force=$forceRefresh")
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    diagnosticInfo = "Refreshing watch data..."
                )
            }

            try {
                val success = watchRepository.refreshWatchData(forceRefresh)
                Log.d(TAG, "Data refresh result: $success")

                if (!success) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to refresh watch data",
                            diagnosticInfo = "Data refresh failed"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            diagnosticInfo = "Data refresh successful"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing watch data: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error refreshing data: ${e.message}",
                        diagnosticInfo = "Exception during data refresh: ${e.message}\n${e.stackTraceToString()}"
                    )
                }
            }
        }
    }

    /**
     * Schedule periodic background sync
     */
    private fun schedulePeriodicSync() {
        Log.d(TAG, "Scheduling periodic health sync")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(
            SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(getApplication())
            .enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

        Log.d(TAG, "Periodic health sync scheduled")
    }


    fun launchSamsungHealth() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Attempting to launch Samsung Health app...")
                val intent = getApplication<Application>().packageManager
                    .getLaunchIntentForPackage("com.sec.android.app.shealth")

                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    getApplication<Application>().startActivity(intent)
                    Log.d(TAG, "Samsung Health app launched successfully")

                    // Wait a moment before requesting permissions
                    delay(2000) // Wait 2 seconds
                    Log.d(TAG, "Requesting permissions after Samsung Health launch")
                    requestPermissions()
                } else {
                    Log.e(TAG, "Could not create intent for Samsung Health")
                    _uiState.update {
                        it.copy(
                            error = "Could not launch Samsung Health app",
                            diagnosticInfo = "Failed to create intent for Samsung Health. Please install Samsung Health from Galaxy Store."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error launching Samsung Health: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Failed to launch Samsung Health app: ${e.message}",
                        diagnosticInfo = "Exception when launching Samsung Health: ${e.message}"
                    )
                }
            }
        }
    }


    /**
     * Stop background sync
     */
    fun stopBackgroundSync() {
        Log.d(TAG, "Stopping background sync")

        WorkManager.getInstance(getApplication())
            .cancelUniqueWork(SYNC_WORK_NAME)

        Log.d(TAG, "Periodic health sync canceled")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel being cleared, cleaning up resources")
        watchRepository.cleanup()
    }
}