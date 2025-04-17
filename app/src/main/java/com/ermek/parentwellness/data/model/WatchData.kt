package com.ermek.parentwellness.data.model

/**
 * Combined data model for all watch health data
 * @param id Unique identifier for this data snapshot
 * @param timestamp Timestamp when this data was collected in milliseconds
 * @param deviceId Device identifier (e.g., watch model or UUID)
 * @param steps Steps data
 * @param heartRate Heart rate data
 * @param bloodPressure Blood pressure data
 * @param bloodGlucose Blood glucose data
 */
data class WatchData(
    val id: String,
    val timestamp: Long,
    val deviceId: String,
    val steps: HealthData.Steps,
    val heartRate: HealthData.HeartRate,
    val bloodPressure: HealthData.BloodPressure,
    val bloodGlucose: HealthData.BloodGlucose
) {
    /**
     * Helper function to determine if this data is empty/invalid
     */
    fun isEmpty(): Boolean {
        return steps.count == 0 &&
                heartRate.average == 0 &&
                bloodPressure.systolic == 0f &&
                bloodGlucose.average == 0f
    }

    /**
     * Helper function to check data freshness
     * @param maxAgeMs Maximum allowed age in milliseconds
     * @return True if data is fresh, false otherwise
     */
    fun isFresh(maxAgeMs: Long): Boolean {
        val now = System.currentTimeMillis()
        return now - timestamp <= maxAgeMs
    }

    companion object {
        /**
         * Create an empty WatchData object
         */
        fun empty(): WatchData {
            return WatchData(
                id = "",
                timestamp = 0L,
                deviceId = "",
                steps = HealthData.Steps(0, 0L, 0L),
                heartRate = HealthData.HeartRate(0, 0L, emptyList()),
                bloodPressure = HealthData.BloodPressure(0f, 0f, 0L, emptyList()),
                bloodGlucose = HealthData.BloodGlucose(0f, 0L, emptyList())
            )
        }
    }
}