package com.ermek.parentwellness.data.samsung

import android.util.Log
import com.samsung.android.sdk.healthdata.HealthDataResolver
import com.ermek.parentwellness.data.model.HealthData
import com.ermek.parentwellness.data.model.WatchData
import com.samsung.android.sdk.healthdata.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Class for reading different health data types from Samsung Health
 */
class HealthDataReader(private val healthDataStore: HealthDataStore) {
    companion object {
        private const val TAG = "HealthDataReader"
    }

    /**
     * Read steps data for a specified time range
     */
    suspend fun readStepsData(startTime: Long, endTime: Long): HealthData.Steps = withContext(Dispatchers.IO) {
        try {
            // Create filter for time range
            val filter = HealthDataResolver.Filter.and(
                HealthDataResolver.Filter.greaterThanEquals("start_time", startTime),
                HealthDataResolver.Filter.lessThanEquals("end_time", endTime)
            )

            // Create resolver
            val resolver = HealthDataResolver(healthDataStore, null)

            // Build read request for step count
            val request = HealthDataResolver.ReadRequest.Builder()
                .setDataType("com.samsung.health.step_count")
                .setProperties(arrayOf("count", "start_time", "end_time"))
                .setFilter(filter)
                .build()

            // Read data through the internal method
            val result = readDataInternalRaw(resolver, request)

            // Process steps data
            var totalSteps = 0
            var startTimeResult = Long.MAX_VALUE
            var endTimeResult = Long.MIN_VALUE

            for (data in result) {
                val steps = data.getInt("count")
                val start = data.getLong("start_time")
                val end = data.getLong("end_time")

                totalSteps += steps
                startTimeResult = minOf(startTimeResult, start)
                endTimeResult = maxOf(endTimeResult, end)
            }

            // If no data was found, use the requested time range
            if (startTimeResult == Long.MAX_VALUE) {
                startTimeResult = startTime
                endTimeResult = endTime
            }

            HealthData.Steps(totalSteps, startTimeResult, endTimeResult)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading steps data: ${e.message}")
            HealthData.Steps(0, startTime, endTime)
        }
    }

    /**
     * Read heart rate data for a specified time range
     */
    suspend fun readHeartRateData(startTime: Long, endTime: Long): HealthData.HeartRate = withContext(Dispatchers.IO) {
        try {
            // Create filter for time range
            val filter = HealthDataResolver.Filter.and(
                HealthDataResolver.Filter.greaterThanEquals("start_time", startTime),
                HealthDataResolver.Filter.lessThanEquals("end_time", endTime)
            )

            // Create resolver
            val resolver = HealthDataResolver(healthDataStore, null)

            // Build read request for heart rate
            val request = HealthDataResolver.ReadRequest.Builder()
                .setDataType("com.samsung.health.heart_rate")
                .setProperties(arrayOf("heart_rate", "start_time"))
                .setFilter(filter)
                .build()

            // Read data through the internal method
            val result = readDataInternalRaw(resolver, request)

            // Process heart rate data
            val readings = mutableListOf<HealthData.HeartRateReading>()
            var timestamp = endTime  // Default to end time if no readings

            for (data in result) {
                val heartRate = data.getInt("heart_rate")
                val time = data.getLong("start_time")

                timestamp = time  // Update timestamp with most recent reading
                readings.add(HealthData.HeartRateReading(heartRate, time))
            }

            // Calculate average heart rate
            val average = if (readings.isNotEmpty()) {
                readings.sumOf { it.value } / readings.size
            } else {
                0
            }

            HealthData.HeartRate(average, timestamp, readings)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading heart rate data: ${e.message}")
            HealthData.HeartRate(0, startTime, emptyList())
        }
    }

    /**
     * Read blood pressure data for a specified time range
     */
    suspend fun readBloodPressureData(startTime: Long, endTime: Long): HealthData.BloodPressure = withContext(Dispatchers.IO) {
        try {
            // Create filter for time range
            val filter = HealthDataResolver.Filter.and(
                HealthDataResolver.Filter.greaterThanEquals("start_time", startTime),
                HealthDataResolver.Filter.lessThanEquals("start_time", endTime)
            )

            // Create resolver
            val resolver = HealthDataResolver(healthDataStore, null)

            // Build read request for blood pressure
            val request = HealthDataResolver.ReadRequest.Builder()
                .setDataType("com.samsung.health.blood_pressure")
                .setProperties(arrayOf("systolic", "diastolic", "start_time"))
                .setFilter(filter)
                .build()

            // Read data through the internal method
            val result = readDataInternalRaw(resolver, request)

            // Process blood pressure data
            val readings = mutableListOf<HealthData.BloodPressureReading>()
            var timestamp = endTime  // Default to end time if no readings

            for (data in result) {
                val systolic = data.getFloat("systolic")
                val diastolic = data.getFloat("diastolic")
                val time = data.getLong("start_time")

                timestamp = time  // Update timestamp with most recent reading
                readings.add(HealthData.BloodPressureReading(systolic, diastolic, time))
            }

            // Calculate average blood pressure
            val avgSystolic = if (readings.isNotEmpty()) {
                readings.sumOf { it.systolic.toDouble() }.toFloat() / readings.size
            } else {
                0f
            }

            val avgDiastolic = if (readings.isNotEmpty()) {
                readings.sumOf { it.diastolic.toDouble() }.toFloat() / readings.size
            } else {
                0f
            }

            HealthData.BloodPressure(avgSystolic, avgDiastolic, timestamp, readings)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading blood pressure data: ${e.message}")
            HealthData.BloodPressure(0f, 0f, startTime, emptyList())
        }
    }

    /**
     * Read blood glucose data for a specified time range
     */
    suspend fun readBloodGlucoseData(startTime: Long, endTime: Long): HealthData.BloodGlucose = withContext(Dispatchers.IO) {
        try {
            // Create filter for time range
            val filter = HealthDataResolver.Filter.and(
                HealthDataResolver.Filter.greaterThanEquals("start_time", startTime),
                HealthDataResolver.Filter.lessThanEquals("start_time", endTime)
            )

            // Create resolver
            val resolver = HealthDataResolver(healthDataStore, null)

            // Build read request for blood glucose
            val request = HealthDataResolver.ReadRequest.Builder()
                .setDataType("com.samsung.health.blood_glucose")
                .setProperties(arrayOf("glucose", "start_time", "meal"))
                .setFilter(filter)
                .build()

            // Read data through the internal method
            val result = readDataInternalRaw(resolver, request)

            // Process blood glucose data
            val readings = mutableListOf<HealthData.BloodGlucoseReading>()
            var timestamp = endTime  // Default to end time if no readings

            for (data in result) {
                val glucose = data.getFloat("glucose")
                val time = data.getLong("start_time")
                val meal = data.getString("meal") ?: "unknown"

                timestamp = time  // Update timestamp with most recent reading
                readings.add(HealthData.BloodGlucoseReading(glucose, time, meal))
            }

            // Calculate average blood glucose
            val avgGlucose = if (readings.isNotEmpty()) {
                readings.sumOf { it.glucose.toDouble() }.toFloat() / readings.size
            } else {
                0f
            }

            HealthData.BloodGlucose(avgGlucose, timestamp, readings)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading blood glucose data: ${e.message}")
            HealthData.BloodGlucose(0f, startTime, emptyList())
        }
    }

    /**
     * Generic method to read raw data from Samsung Health
     * To avoid conflicts, return List of generic Map instead of HealthData
     */
    // In your HealthDataReader class, modify the data reading methods:
    private suspend fun readDataInternalRaw(
        resolver: HealthDataResolver,
        request: HealthDataResolver.ReadRequest
    ): List<com.samsung.android.sdk.healthdata.HealthData> = withContext(Dispatchers.Main) {
        // Use Main dispatcher which has a looper
        suspendCancellableCoroutine { continuation ->
            try {
                val resultList = mutableListOf<com.samsung.android.sdk.healthdata.HealthData>()

                val resultListener = object : HealthResultHolder.ResultListener<HealthDataResolver.ReadResult> {
                    override fun onResult(result: HealthDataResolver.ReadResult) {
                        try {
                            val iterator = result.iterator()
                            while (iterator.hasNext()) {
                                resultList.add(iterator.next())
                            }

                            continuation.resume(resultList)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing data: ${e.message}")
                            continuation.resumeWithException(e)
                        } finally {
                            result.close()
                        }
                    }
                }

                resolver.read(request).setResultListener(resultListener)
            } catch (e: Exception) {
                Log.e(TAG, "Error in data resolver: ${e.message}")
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * Get a combined watch data object with all health data
     */
    suspend fun getWatchData(startTime: Long, endTime: Long): WatchData = withContext(Dispatchers.IO) {
        try {
            // Read all health data
            val steps = readStepsData(startTime, endTime)
            val heartRate = readHeartRateData(startTime, endTime)
            val bloodPressure = readBloodPressureData(startTime, endTime)
            val bloodGlucose = readBloodGlucoseData(startTime, endTime)

            // Create the WatchData object
            WatchData(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                deviceId = "SamsungWatch4",
                steps = steps,
                heartRate = heartRate,
                bloodPressure = bloodPressure,
                bloodGlucose = bloodGlucose
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting watch data: ${e.message}")
            WatchData.empty()
        }
    }
}