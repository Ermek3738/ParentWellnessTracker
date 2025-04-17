package com.ermek.parentwellness.data.model

abstract class HealthData {
    open var id: String = ""
    open var userId: String = ""
    open var timestamp: Long = System.currentTimeMillis()

    data class Steps(
        val count: Int,
        val startTime: Long,
        val endTime: Long,
        val dataTimestamp: Long = System.currentTimeMillis()
    ) : HealthData() {
        override var id: String = ""
        override var userId: String = ""
        override var timestamp: Long = dataTimestamp
    }

    data class HeartRate(
        val average: Int,
        val dataTimestamp: Long,
        val readings: List<HeartRateReading>
    ) : HealthData() {
        override var id: String = ""
        override var userId: String = ""
        override var timestamp: Long = dataTimestamp
    }

    data class HeartRateReading(
        val value: Int,
        val timestamp: Long
    )

    data class BloodPressure(
        val systolic: Float,
        val diastolic: Float,
        val dataTimestamp: Long,
        val readings: List<BloodPressureReading>
    ) : HealthData() {
        override var id: String = ""
        override var userId: String = ""
        override var timestamp: Long = dataTimestamp
    }

    data class BloodPressureReading(
        val systolic: Float,
        val diastolic: Float,
        val timestamp: Long
    )

    data class BloodGlucose(
        val average: Float,
        val dataTimestamp: Long,
        val readings: List<BloodGlucoseReading>
    ) : HealthData() {
        override var id: String = ""
        override var userId: String = ""
        override var timestamp: Long = dataTimestamp
    }

    data class BloodGlucoseReading(
        val glucose: Float,
        val timestamp: Long,
        val mealTime: String
    )
}