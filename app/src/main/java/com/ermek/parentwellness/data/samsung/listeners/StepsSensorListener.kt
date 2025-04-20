package com.ermek.parentwellness.data.samsung.listeners

import android.util.Log
import androidx.annotation.NonNull
import com.ermek.parentwellness.data.samsung.models.StepsData
import com.ermek.parentwellness.data.samsung.utils.DataPointUtils
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.data.DataPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Listener for steps sensor data
 */
class StepsSensorListener {
    private val TAG = "StepsSensorListener"

    // Tracker for steps
    private var stepsTracker: HealthTracker? = null

    // Current step count data
    private val _stepsData = MutableStateFlow<StepsData?>(null)
    val stepsData: StateFlow<StepsData?> = _stepsData.asStateFlow()

    // Daily step count history (by date)
    private val dailyStepsMap = mutableMapOf<String, StepsData>()
    val dailyStepsHistory: Map<String, StepsData> get() = dailyStepsMap.toMap()

    // Hourly step counts for today
    private val _hourlySteps = MutableStateFlow<Map<Int, Int>>(mapOf())
    val hourlySteps: StateFlow<Map<Int, Int>> = _hourlySteps.asStateFlow()

    // Last activity timestamp to detect inactivity
    private var lastActivityTimestamp = System.currentTimeMillis()

    // Inactivity alert
    private val _inactivityAlert = MutableStateFlow<InactivityAlert?>(null)
    val inactivityAlert: StateFlow<InactivityAlert?> = _inactivityAlert.asStateFlow()

    // Daily step goal
    private var dailyStepGoal = StepsData.MEDIUM_ACTIVITY_GOAL

    /**
     * Initialize the steps tracker
     */
    fun initialize(tracker: HealthTracker) {
        stepsTracker = tracker

        // Set up event listener for steps data
        tracker.setEventListener(object : HealthTracker.TrackerEventListener {
            override fun onDataReceived(@NonNull dataPoints: List<DataPoint>) {
                Log.d(TAG, "Steps data received: ${dataPoints.size} data points")
                processStepsData(dataPoints)
            }

            override fun onFlushCompleted() {
                Log.d(TAG, "Steps data flush completed")
            }

            override fun onError(error: HealthTracker.TrackerError) {
                Log.e(TAG, "Steps tracker error: $error")
            }
        })

        Log.d(TAG, "Steps tracker initialized")
    }

    /**
     * Process steps data points from the sensor
     */
    private fun processStepsData(dataPoints: List<DataPoint>) {
        if (dataPoints.isEmpty()) return

        // Process each data point
        dataPoints.forEach { dataPoint ->
            try {
                // Extract steps value using our utility class
                val stepsValue = DataPointUtils.extractStepCount(dataPoint)
                val timestamp = DataPointUtils.extractTimestamp(dataPoint)

                // Skip invalid step values
                if (stepsValue < 0) {
                    Log.w(TAG, "Invalid steps value: $stepsValue, skipping")
                    return@forEach
                }

                // Create steps data object
                val stepsData = StepsData(
                    stepCount = stepsValue,
                    timestamp = timestamp,
                    dailyGoal = dailyStepGoal
                )

                Log.d(TAG, "Processed steps data: $stepsValue steps, timestamp: $timestamp")

                // Update the state flow with the latest data
                _stepsData.value = stepsData

                // Update daily history
                updateDailyHistory(stepsData)

                // Update hourly breakdown
                updateHourlySteps(stepsData)

                // Check for inactivity
                checkForInactivity(stepsData)

            } catch (e: Exception) {
                Log.e(TAG, "Error processing steps data point: ${e.message}", e)
            }
        }
    }

    /**
     * Update the daily step history
     */
    private fun updateDailyHistory(stepsData: StepsData) {
        val dateKey = getDateKey(stepsData.timestamp)
        dailyStepsMap[dateKey] = stepsData
        Log.d(TAG, "Updated daily history for $dateKey: ${stepsData.stepCount} steps")
    }

    /**
     * Update hourly steps breakdown for today
     */
    private fun updateHourlySteps(stepsData: StepsData) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = stepsData.timestamp
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // Only update if it's today's data
        if (isToday(stepsData.timestamp)) {
            val currentHourlySteps = _hourlySteps.value.toMutableMap()
            currentHourlySteps[hour] = stepsData.stepCount
            _hourlySteps.value = currentHourlySteps
            Log.d(TAG, "Updated hourly steps for hour $hour: ${stepsData.stepCount} steps")
        }
    }

    /**
     * Check for prolonged inactivity
     */
    private fun checkForInactivity(stepsData: StepsData) {
        val currentTime = System.currentTimeMillis()

        // If steps haven't increased significantly in the inactivity threshold period
        if (stepsData.stepCount < StepsData.VERY_LOW_ACTIVITY_THRESHOLD) {
            // Check if we've been inactive for too long
            if (currentTime - lastActivityTimestamp > INACTIVITY_THRESHOLD_MS) {
                val inactiveDurationMinutes = (currentTime - lastActivityTimestamp) / (60 * 1000)
                _inactivityAlert.value = InactivityAlert(
                    timestamp = currentTime,
                    inactiveDurationMinutes = inactiveDurationMinutes
                )
                Log.w(TAG, "Inactivity detected: $inactiveDurationMinutes minutes")
            }
        } else {
            // Update last activity timestamp and clear any alerts
            lastActivityTimestamp = currentTime
            _inactivityAlert.value = null
        }
    }

    /**
     * Set daily step goal
     */
    fun setDailyStepGoal(goal: Int) {
        dailyStepGoal = goal

        // Update current steps data with new goal
        _stepsData.value?.let {
            _stepsData.value = it.copy(dailyGoal = goal)
        }

        Log.d(TAG, "Daily step goal updated: $goal steps")
    }

    /**
     * Request a manual refresh of steps data
     */
    fun refreshStepsData() {
        Log.d(TAG, "Manually refreshing steps data")
        stepsTracker?.flush()
    }

    /**
     * Get date key for storing daily step counts
     */
    private fun getDateKey(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    /**
     * Check if timestamp is from today
     */
    private fun isToday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()

        calendar.timeInMillis = timestamp

        return (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH))
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up steps tracker resources")
        stepsTracker?.unsetEventListener()
        stepsTracker = null
    }

    companion object {
        // 2 hours of inactivity triggers an alert
        private const val INACTIVITY_THRESHOLD_MS = 2 * 60 * 60 * 1000L
    }

    /**
     * Data class for inactivity alerts
     */
    data class InactivityAlert(
        val timestamp: Long,
        val inactiveDurationMinutes: Long
    )
}