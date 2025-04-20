package com.ermek.parentwellness.data.samsung.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ermek.parentwellness.data.samsung.SamsungHealthSensorManager
import com.ermek.parentwellness.data.samsung.SensorConnectionManager
import com.ermek.parentwellness.data.samsung.listeners.HeartRateSensorListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Background worker for syncing Samsung Health sensor data periodically
 */
class SensorSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val tag = "SensorSyncWorker"

    // Samsung Health Sensor Manager
    private lateinit var sensorManager: SamsungHealthSensorManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(tag, "Starting Samsung Health sensor sync")

        try {
            // Initialize sensor manager
            sensorManager = SamsungHealthSensorManager(applicationContext)
            sensorManager.initialize()

            // Wait for connection to be established (with timeout)
            if (!waitForConnection()) {
                Log.e(tag, "Connection to Samsung Health Sensor SDK failed")
                return@withContext Result.retry()
            }

            // Sync heart rate data
            syncHeartRateData()

            // Sync steps data
            syncStepsData()

            // Check for critical health alerts
            checkForHealthAlerts()

            // Cleanup
            sensorManager.cleanup()

            Log.d(tag, "Samsung Health sensor sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Error during Samsung Health sync: ${e.message}")
            Result.retry()
        }
    }

    /**
     * Wait for connection to be established with timeout
     */
    private suspend fun waitForConnection(): Boolean {
        return try {
            withTimeout(CONNECTION_TIMEOUT_MS) {
                val connectionManager = sensorManager.getConnectionManager()

                while (true) {
                    when (connectionManager.connectionState.value) {
                        is SensorConnectionManager.ConnectionState.Connected -> {
                            return@withTimeout true
                        }

                        is SensorConnectionManager.ConnectionState.Error -> {
                            return@withTimeout false
                        }

                        is SensorConnectionManager.ConnectionState.ResolutionRequired -> {
                            // Cannot resolve in background, will retry
                            return@withTimeout false
                        }

                        else -> {
                            // Keep waiting
                            delay(500)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Timeout waiting for Samsung Health connection: ${e.message}")
            false
        } as Boolean
    }

    /**
     * Sync heart rate data
     */
    private suspend fun syncHeartRateData() {
        try {
            // Request refresh of heart rate data
            sensorManager.getHeartRateListener().refreshHeartRateData()

            // Wait a moment for data to be processed
            delay(2000)

            // Check if we received heart rate data
            val heartRateData = sensorManager.getHeartRateListener().heartRateData.firstOrNull()

            if (heartRateData != null) {
                Log.d(tag, "Heart rate data synced: ${heartRateData.heartRate} bpm")

                // Here you would store the data in your repository/database
                // This is where you'd integrate with your app's data persistence layer
            } else {
                Log.w(tag, "No heart rate data received during sync")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error syncing heart rate data: ${e.message}")
        }
    }

    /**
     * Sync steps data
     */
    private suspend fun syncStepsData() {
        try {
            // Request refresh of steps data
            sensorManager.getStepsListener().refreshStepsData()

            // Wait a moment for data to be processed
            delay(2000)

            // Check if we received steps data
            val stepsData = sensorManager.getStepsListener().stepsData.firstOrNull()

            if (stepsData != null) {
                Log.d(tag, "Steps data synced: ${stepsData.stepCount} steps")

                // Here you would store the data in your repository/database
                // This is where you'd integrate with your app's data persistence layer
            } else {
                Log.w(tag, "No steps data received during sync")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error syncing steps data: ${e.message}")
        }
    }

    /**
     * Check for any critical health alerts that need immediate notification
     */
    private fun checkForHealthAlerts() {
        try {
            // Check for heart rate alerts
            val heartRateAlert = sensorManager.getHeartRateListener().heartRateAlert.value

            if (heartRateAlert != null) {
                // Process heart rate alert based on type
                when (heartRateAlert) {
                    is HeartRateSensorListener.HeartRateAlert.HighHeartRate -> {
                        Log.w(tag, "High heart rate alert: ${heartRateAlert.data.heartRate} bpm")
                        // Here you would send a notification to the user/caregiver
                    }
                    is HeartRateSensorListener.HeartRateAlert.LowHeartRate -> {
                        Log.w(tag, "Low heart rate alert: ${heartRateAlert.data.heartRate} bpm")
                        // Here you would send a notification to the user/caregiver
                    }
                }
            }

            // Check for inactivity alerts
            val inactivityAlert = sensorManager.getStepsListener().inactivityAlert.value

            if (inactivityAlert != null) {
                Log.w(tag, "Inactivity alert: ${inactivityAlert.inactiveDurationMinutes} minutes inactive")
                // Here you would send a notification to the user/caregiver
            }
        } catch (e: Exception) {
            Log.e(tag, "Error checking for health alerts: ${e.message}")
        }
    }

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 30000L // 30 seconds
    }
}