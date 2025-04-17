package com.ermek.parentwellness.data.repository

import android.content.Context
import android.util.Log
import com.ermek.parentwellness.data.model.WatchData
import com.ermek.parentwellness.data.samsung.SamsungHealthConnector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Repository for Samsung Galaxy Watch data
 */
class WatchRepository(private val context: Context) {
    companion object {
        private const val TAG = "WatchRepository"

        // Cache timeout (1 hour)
        private val CACHE_TIMEOUT_MS = TimeUnit.HOURS.toMillis(1)
    }

    // Samsung Health connector
    private val samsungHealthConnector = SamsungHealthConnector(context)

    // Latest watch data
    private val _watchData = MutableStateFlow(WatchData.empty())
    val watchData: StateFlow<WatchData> = _watchData.asStateFlow()

    // Connection status
    val connectionStatus = samsungHealthConnector.connectionStatus

    // Error messages
    val errorMessages = samsungHealthConnector.errorMessage

    // Last refresh timestamp
    private var lastRefreshTime: Long = 0

    /**
     * Initialize Samsung Health connection
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Initializing Samsung Health connector")
        samsungHealthConnector.initialize()
    }

    /**
     * Request Samsung Health permissions
     * @return Result of permission request
     */
    suspend fun requestPermissions(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "Requesting Samsung Health permissions")
        return@withContext samsungHealthConnector.requestPermissions()
    }

    /**
     * Refresh watch data from Samsung Health
     * @param forceRefresh Whether to force refresh ignoring cache
     * @return Success status of the refresh operation
     */
    suspend fun refreshWatchData(forceRefresh: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if we need to refresh based on cache timeout
            val currentTime = System.currentTimeMillis()
            if (!forceRefresh && currentTime - lastRefreshTime < CACHE_TIMEOUT_MS) {
                Log.d(TAG, "Using cached watch data (age: ${(currentTime - lastRefreshTime) / 1000} seconds)")
                return@withContext true
            }

            // Get health data reader
            val healthDataReader = samsungHealthConnector.getHealthDataReader()
            if (healthDataReader == null) {
                Log.e(TAG, "Failed to get health data reader")
                return@withContext false
            }

            // Define time range (last 24 hours)
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.DAYS.toMillis(1)

            Log.d(TAG, "Fetching watch data from $startTime to $endTime")
            val newWatchData = healthDataReader.getWatchData(startTime, endTime)

            // Update watch data
            _watchData.value = newWatchData
            lastRefreshTime = currentTime

            Log.d(TAG, "Watch data refreshed successfully: $newWatchData")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing watch data: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Clean up resources when repository is no longer needed
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up resources")
        samsungHealthConnector.disconnect()
    }
}