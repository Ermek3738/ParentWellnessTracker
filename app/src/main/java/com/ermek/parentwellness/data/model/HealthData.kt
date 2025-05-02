package com.ermek.parentwellness.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Data class representing a health metric measurement
 */
data class HealthData(
    var id: String = "", // Firestore document ID
    val userId: String = "", // User who owns this data

    @get:PropertyName("metricType")
    @set:PropertyName("metricType")
    var metricType: String = "", // HEART_RATE, BLOOD_PRESSURE, BLOOD_SUGAR, STEPS

    @get:PropertyName("primaryValue")
    @set:PropertyName("primaryValue")
    var primaryValue: Double = 0.0, // Main value (heart rate, systolic pressure, blood sugar, steps)

    @get:PropertyName("secondaryValue")
    @set:PropertyName("secondaryValue")
    var secondaryValue: Double? = null, // Secondary value (diastolic blood pressure)

    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Long = 0, // When the measurement was taken

    @get:PropertyName("situation")
    @set:PropertyName("situation")
    var situation: String = "", // Context (resting, after meal, etc.)

    @get:PropertyName("notes")
    @set:PropertyName("notes")
    var notes: String = "", // Additional notes

    @get:PropertyName("source")
    @set:PropertyName("source")
    var source: String = "manual", // Source of data (manual, samsung_health, etc.)

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis() // When the record was created
) {
    companion object {
        // Metric types
        const val TYPE_HEART_RATE = "HEART_RATE"
        const val TYPE_BLOOD_PRESSURE = "BLOOD_PRESSURE"
        const val TYPE_BLOOD_SUGAR = "BLOOD_SUGAR"
        const val TYPE_STEPS = "STEPS"

        // Data sources
        const val SOURCE_MANUAL = "manual"
        const val SOURCE_SAMSUNG_HEALTH = "samsung_health"
        const val SOURCE_SIMULATOR = "simulator"

        // Helper functions
        fun fromHealthDataEntry(
            userId: String,
            entry: com.ermek.parentwellness.ui.components.HealthDataEntry
        ): HealthData {
            val metricTypeString = when (entry.metricType) {
                com.ermek.parentwellness.ui.components.MetricType.HEART_RATE -> TYPE_HEART_RATE
                com.ermek.parentwellness.ui.components.MetricType.BLOOD_PRESSURE -> TYPE_BLOOD_PRESSURE
                com.ermek.parentwellness.ui.components.MetricType.BLOOD_SUGAR -> TYPE_BLOOD_SUGAR
                com.ermek.parentwellness.ui.components.MetricType.STEPS -> TYPE_STEPS
            }

            return HealthData(
                userId = userId,
                metricType = metricTypeString,
                primaryValue = entry.primaryValue.toDoubleOrNull() ?: 0.0,
                secondaryValue = entry.secondaryValue?.toDoubleOrNull(),
                timestamp = entry.timestamp,
                situation = entry.situation,
                notes = entry.notes,
                source = SOURCE_MANUAL,
                createdAt = System.currentTimeMillis()
            )
        }
    }
}