package com.ermek.parentwellness.data.samsung.models

/**
 * Data class representing heart rate measurements
 */
data class HeartRateData(
    val heartRate: Int,               // Heart rate in BPM
    val timestamp: Long,              // Timestamp when the reading was taken
    val accuracy: Int = 0,            // Accuracy level of the reading (0: unreliable, 3: most accurate)
    val isResting: Boolean = false,   // Whether the heart rate is measured during rest
    val isAbnormal: Boolean = false   // Flag for potentially concerning rates based on user profile
) {
    companion object {
        const val ACCURACY_UNRELIABLE = 0
        const val ACCURACY_LOW = 1
        const val ACCURACY_MEDIUM = 2
        const val ACCURACY_HIGH = 3

        // Heart rate thresholds for elderly users (adjust based on medical guidance)
        const val RESTING_RATE_LOW_THRESHOLD = 50
        const val RESTING_RATE_HIGH_THRESHOLD = 90
        const val ACTIVE_RATE_HIGH_THRESHOLD = 120
    }
}