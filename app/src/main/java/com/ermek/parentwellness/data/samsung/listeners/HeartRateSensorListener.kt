package com.ermek.parentwellness.data.samsung.listeners

import android.util.Log
import androidx.annotation.NonNull
import com.ermek.parentwellness.data.samsung.models.HeartRateData
import com.ermek.parentwellness.data.samsung.utils.DataPointUtils
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.data.DataPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Listener for heart rate sensor data
 */
class HeartRateSensorListener {
    private val TAG = "HeartRateSensorListener"

    // Tracker for heart rate
    private var heartRateTracker: HealthTracker? = null

    // Recent heart rate data
    private val _heartRateData = MutableStateFlow<HeartRateData?>(null)
    val heartRateData: StateFlow<HeartRateData?> = _heartRateData.asStateFlow()

    // Historical heart rate readings (keeping recent history in memory)
    private val heartRateHistoryList = CopyOnWriteArrayList<HeartRateData>()
    val heartRateHistory: List<HeartRateData> get() = heartRateHistoryList.toList()

    // Thresholds for heart rate alerts (can be personalized per user)
    private var lowThreshold = HeartRateData.RESTING_RATE_LOW_THRESHOLD
    private var highThreshold = HeartRateData.RESTING_RATE_HIGH_THRESHOLD

    // Alert state for abnormal heart rate
    private val _heartRateAlert = MutableStateFlow<HeartRateAlert?>(null)
    val heartRateAlert: StateFlow<HeartRateAlert?> = _heartRateAlert.asStateFlow()

    /**
     * Initialize the heart rate tracker
     */
    fun initialize(tracker: HealthTracker) {
        heartRateTracker = tracker

        // Set up event listener for heart rate data
        tracker.setEventListener(object : HealthTracker.TrackerEventListener {
            override fun onDataReceived(@NonNull dataPoints: List<DataPoint>) {
                Log.d(TAG, "Heart rate data received: ${dataPoints.size} data points")
                processHeartRateData(dataPoints)
            }

            override fun onFlushCompleted() {
                Log.d(TAG, "Heart rate data flush completed")
            }

            override fun onError(error: HealthTracker.TrackerError) {
                Log.e(TAG, "Heart rate tracker error: $error")
            }
        })

        Log.d(TAG, "Heart rate tracker initialized")
    }

    /**
     * Process heart rate data points from the sensor
     */
    private fun processHeartRateData(dataPoints: List<DataPoint>) {
        if (dataPoints.isEmpty()) return

        // Process each data point
        dataPoints.forEach { dataPoint ->
            try {
                // Extract heart rate using our utility class
                val heartRateValue = DataPointUtils.extractHeartRate(dataPoint)
                val timestamp = DataPointUtils.extractTimestamp(dataPoint)
                val accuracy = DataPointUtils.extractAccuracy(dataPoint)

                // Skip invalid heart rate values
                if (heartRateValue <= 0) {
                    Log.w(TAG, "Invalid heart rate value: $heartRateValue, skipping")
                    return@forEach
                }

                // Create heart rate data object
                val heartRateData = HeartRateData(
                    heartRate = heartRateValue,
                    timestamp = timestamp,
                    accuracy = accuracy,
                    isResting = isUserResting(),
                    isAbnormal = isHeartRateAbnormal(heartRateValue)
                )

                // Log the processed data
                Log.d(TAG, "Processed heart rate: $heartRateValue bpm, timestamp: $timestamp, accuracy: $accuracy")

                // Update the state flow with the latest data
                _heartRateData.value = heartRateData

                // Add to history (limiting size to avoid memory issues)
                heartRateHistoryList.add(heartRateData)
                if (heartRateHistoryList.size > MAX_HISTORY_SIZE) {
                    heartRateHistoryList.removeAt(0)
                }

                // Check for alerts
                checkForHeartRateAlerts(heartRateData)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing heart rate data point: ${e.message}", e)
            }
        }
    }

    /**
     * Check if the heart rate is abnormal based on thresholds
     */
    private fun isHeartRateAbnormal(heartRate: Int): Boolean {
        return if (isUserResting()) {
            // Check against resting thresholds
            heartRate < lowThreshold || heartRate > highThreshold
        } else {
            // Check against active thresholds
            heartRate > HeartRateData.ACTIVE_RATE_HIGH_THRESHOLD
        }
    }

    /**
     * Check if the user is likely resting based on time and recent activity
     * This is a simplified logic - in a real app, you would use activity recognition
     */
    private fun isUserResting(): Boolean {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        // Simple heuristic: consider night hours as resting time
        return hourOfDay < 7 || hourOfDay > 22
    }

    /**
     * Check for heart rate alerts and update the alert state flow
     */
    private fun checkForHeartRateAlerts(data: HeartRateData) {
        if (data.isAbnormal && data.accuracy >= HeartRateData.ACCURACY_MEDIUM) {
            // Create an alert for abnormal heart rate
            val alertType = when {
                data.heartRate < lowThreshold -> HeartRateAlert.LowHeartRate(data)
                data.heartRate > highThreshold -> HeartRateAlert.HighHeartRate(data)
                else -> return // No alert needed
            }

            _heartRateAlert.value = alertType
            Log.w(TAG, "Heart rate alert: ${data.heartRate} bpm")
        } else {
            // Clear any existing alert if heart rate returns to normal
            if (_heartRateAlert.value != null) {
                _heartRateAlert.value = null
            }
        }
    }

    /**
     * Set custom thresholds for heart rate alerts
     */
    fun setHeartRateThresholds(low: Int, high: Int) {
        lowThreshold = low
        highThreshold = high
        Log.d(TAG, "Heart rate thresholds updated: low=$low, high=$high")
    }

    /**
     * Request a manual refresh of heart rate data
     */
    fun refreshHeartRateData() {
        Log.d(TAG, "Manually refreshing heart rate data")
        heartRateTracker?.flush()
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up heart rate tracker resources")
        heartRateTracker?.unsetEventListener()
        heartRateTracker = null
    }

    companion object {
        private const val MAX_HISTORY_SIZE = 100 // Store the last 100 readings
    }

    /**
     * Sealed class for heart rate alerts
     */
    sealed class HeartRateAlert {
        data class LowHeartRate(val data: HeartRateData) : HeartRateAlert()
        data class HighHeartRate(val data: HeartRateData) : HeartRateAlert()
    }
}