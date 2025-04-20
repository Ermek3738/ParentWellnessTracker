package com.ermek.parentwellness.data.samsung.models

/**
 * Data class representing step count measurements
 */
data class StepsData(
    val stepCount: Int,               // Number of steps
    val timestamp: Long,              // Timestamp when the reading was taken
    val dailyGoal: Int = 5000,        // Default daily step goal (adjustable per user)
    val caloriesBurned: Float = 0f,   // Estimated calories burned (if available)
    val distanceInMeters: Float = 0f  // Estimated distance covered (if available)
) {
    companion object {
        // Suggested daily step goals for elderly users based on activity level
        const val LOW_ACTIVITY_GOAL = 2500
        const val MEDIUM_ACTIVITY_GOAL = 5000
        const val HIGH_ACTIVITY_GOAL = 7500

        // Thresholds for activity monitoring (steps per day)
        const val VERY_LOW_ACTIVITY_THRESHOLD = 1000 // Trigger alert if below this threshold
    }

    /**
     * Calculate progress towards daily goal as percentage
     */
    fun getProgressPercentage(): Int {
        return if (dailyGoal > 0) {
            (stepCount * 100 / dailyGoal).coerceIn(0, 100)
        } else {
            0
        }
    }
}