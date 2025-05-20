package com.ermek.parentwellness.data.generators

import com.ermek.parentwellness.data.model.HealthData
import java.util.*
import kotlin.math.abs

class SimulatedDataGenerator {
    companion object {
        // User profile types for different simulation scenarios
        enum class UserProfile {
            HEALTHY,
            PRE_HYPERTENSIVE,
            HYPERTENSIVE,
            PRE_DIABETIC,
            DIABETIC,
            SEDENTARY,
            ACTIVE
        }

        // Time periods for data generation
        enum class TimePeriod {
            DAY,
            WEEK,
            MONTH,
            THREE_MONTHS,
            SIX_MONTHS,
            YEAR
        }

        /**
         * Generate health data for a specific user, profile, and time period
         */
        fun generateHealthData(
            userId: String,
            metric: String, // HealthData.TYPE_*
            profile: UserProfile = UserProfile.HEALTHY,
            period: TimePeriod = TimePeriod.MONTH,
            includeAnomalies: Boolean = true
        ): List<HealthData> {
            return when(metric) {
                HealthData.TYPE_HEART_RATE -> generateHeartRateData(userId, profile, period, includeAnomalies)
                HealthData.TYPE_BLOOD_PRESSURE -> generateBloodPressureData(userId, profile, period, includeAnomalies)
                HealthData.TYPE_BLOOD_SUGAR -> generateBloodSugarData(userId, profile, period, includeAnomalies)
                HealthData.TYPE_STEPS -> generateStepsData(userId, profile, period, includeAnomalies)
                else -> emptyList()
            }
        }

        // Implementations of specific generators below...
        /**
         * Generate heart rate data with realistic patterns
         */
        private fun generateHeartRateData(
            userId: String,
            profile: UserProfile,
            period: TimePeriod,
            includeAnomalies: Boolean
        ): List<HealthData> {
            val heartRateData = mutableListOf<HealthData>()
            val calendar = Calendar.getInstance()

            // Set timestamp to current time
            val endTime = calendar.timeInMillis

            // Determine start time based on period
            calendar.apply {
                when (period) {
                    TimePeriod.DAY -> add(Calendar.DAY_OF_YEAR, -1)
                    TimePeriod.WEEK -> add(Calendar.DAY_OF_YEAR, -7)
                    TimePeriod.MONTH -> add(Calendar.MONTH, -1)
                    TimePeriod.THREE_MONTHS -> add(Calendar.MONTH, -3)
                    TimePeriod.SIX_MONTHS -> add(Calendar.MONTH, -6)
                    TimePeriod.YEAR -> add(Calendar.YEAR, -1)
                }
            }
            val startTime = calendar.timeInMillis

            // Calculate interval between readings based on period
            val intervalMillis = when(period) {
                TimePeriod.DAY -> 30 * 60 * 1000L // 30 minutes
                TimePeriod.WEEK -> 2 * 60 * 60 * 1000L // 2 hours
                TimePeriod.MONTH -> 6 * 60 * 60 * 1000L // 6 hours
                TimePeriod.THREE_MONTHS, TimePeriod.SIX_MONTHS -> 12 * 60 * 60 * 1000L // 12 hours
                TimePeriod.YEAR -> 24 * 60 * 60 * 1000L // 24 hours
            }

            // Base heart rate values depending on profile
            val baseHeartRate = when(profile) {
                UserProfile.HEALTHY -> 65.0
                UserProfile.ACTIVE -> 60.0
                UserProfile.SEDENTARY -> 72.0
                UserProfile.PRE_HYPERTENSIVE, UserProfile.HYPERTENSIVE -> 75.0
                UserProfile.PRE_DIABETIC, UserProfile.DIABETIC -> 78.0
            }

            // Determine activity level for daily pattern (resting vs active)
            val activityFactor = when(profile) {
                UserProfile.ACTIVE -> 1.3
                UserProfile.HEALTHY -> 1.2
                UserProfile.SEDENTARY, UserProfile.PRE_DIABETIC -> 1.1
                else -> 1.15
            }

            // Generate data points from start to end time
            var timestamp = startTime
            val random = Random()
            val anomalyProbability = if (includeAnomalies) 0.05 else 0.0 // 5% chance of anomaly

            while (timestamp <= endTime) {
                calendar.timeInMillis = timestamp

                // Get hour of day (0-23) for daily pattern
                val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

                // Calculate daily pattern: higher during day, lower at night
                val dailyPattern = if (hourOfDay in 8..20) {
                    // Daytime (8am-8pm): higher heart rate
                    // Peak around 12-2pm (hourOfDay 12-14)
                    val peakFactor = 1.0 - abs(hourOfDay - 13) / 10.0
                    1.0 + (peakFactor * (activityFactor - 1.0))
                } else {
                    // Nighttime: lower heart rate
                    0.85 + (random.nextDouble() * 0.1) // 0.85-0.95 of base
                }

                // Weekly pattern: slightly more activity on weekdays vs weekend
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val weekdayFactor = if (dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY) 1.05 else 0.95

                // Calculate heart rate with random variation
                var heartRate = baseHeartRate * dailyPattern * weekdayFactor

                // Add some random variation (±5 bpm)
                heartRate += (random.nextDouble() * 10.0) - 5.0

                // Occasionally add anomalies (very high or low)
                if (random.nextDouble() < anomalyProbability) {
                    // Either high or low anomaly
                    heartRate = if (random.nextBoolean()) {
                        // High heart rate (110-130)
                        110.0 + (random.nextDouble() * 20.0)
                    } else {
                        // Low heart rate (40-50)
                        40.0 + (random.nextDouble() * 10.0)
                    }
                }

                // Determine if this is a resting heart rate reading
                val isResting = hourOfDay !in 8..20 || random.nextDouble() < 0.3

                // Create situation description
                val situation = when {
                    hourOfDay < 6 -> "Sleeping"
                    hourOfDay < 10 -> "Morning Routine"
                    hourOfDay < 14 -> "Daily Activity"
                    hourOfDay < 18 -> "Afternoon"
                    hourOfDay < 22 -> "Evening"
                    else -> "Bedtime"
                }

                // Create health data object
                val data = HealthData(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    metricType = HealthData.TYPE_HEART_RATE,
                    primaryValue = heartRate.toInt().toDouble(),
                    timestamp = timestamp,
                    situation = if (isResting) "Resting" else situation,
                    notes = if (isResting) "Resting heart rate measurement" else "",
                    source = HealthData.SOURCE_SIMULATOR,
                    createdAt = System.currentTimeMillis()
                )

                heartRateData.add(data)
                timestamp += intervalMillis
            }

            return heartRateData
        }

        /**
         * Generate blood pressure data with realistic patterns
         */
        private fun generateBloodPressureData(
            userId: String,
            profile: UserProfile,
            period: TimePeriod,
            includeAnomalies: Boolean
        ): List<HealthData> {
            val bpData = mutableListOf<HealthData>()
            val calendar = Calendar.getInstance()

            // Set timestamp to current time
            val endTime = calendar.timeInMillis

            // Determine start time based on period
            calendar.apply {
                when (period) {
                    TimePeriod.DAY -> add(Calendar.DAY_OF_YEAR, -1)
                    TimePeriod.WEEK -> add(Calendar.DAY_OF_YEAR, -7)
                    TimePeriod.MONTH -> add(Calendar.MONTH, -1)
                    TimePeriod.THREE_MONTHS -> add(Calendar.MONTH, -3)
                    TimePeriod.SIX_MONTHS -> add(Calendar.MONTH, -6)
                    TimePeriod.YEAR -> add(Calendar.YEAR, -1)
                }
            }
            val startTime = calendar.timeInMillis

            // Blood pressure readings are typically taken less frequently
            val intervalMillis = when(period) {
                TimePeriod.DAY -> 6 * 60 * 60 * 1000L // 6 hours
                TimePeriod.WEEK -> 12 * 60 * 60 * 1000L // 12 hours
                TimePeriod.MONTH -> 24 * 60 * 60 * 1000L // 24 hours
                TimePeriod.THREE_MONTHS -> 3 * 24 * 60 * 60 * 1000L // 3 days
                TimePeriod.SIX_MONTHS -> 5 * 24 * 60 * 60 * 1000L // 5 days
                TimePeriod.YEAR -> 7 * 24 * 60 * 60 * 1000L // 7 days
            }

            // Base blood pressure values depending on profile
            val baseSystolic = when(profile) {
                UserProfile.HEALTHY, UserProfile.ACTIVE -> 115.0
                UserProfile.SEDENTARY -> 122.0
                UserProfile.PRE_HYPERTENSIVE -> 130.0
                UserProfile.HYPERTENSIVE -> 140.0
                UserProfile.PRE_DIABETIC -> 125.0
                UserProfile.DIABETIC -> 135.0
            }

            val baseDiastolic = when(profile) {
                UserProfile.HEALTHY, UserProfile.ACTIVE -> 75.0
                UserProfile.SEDENTARY -> 78.0
                UserProfile.PRE_HYPERTENSIVE -> 85.0
                UserProfile.HYPERTENSIVE -> 90.0
                UserProfile.PRE_DIABETIC -> 80.0
                UserProfile.DIABETIC -> 85.0
            }

            // Generate data points from start to end time
            var timestamp = startTime
            val random = Random()
            val anomalyProbability = if (includeAnomalies) 0.05 else 0.0 // 5% chance of anomaly

            while (timestamp <= endTime) {
                calendar.timeInMillis = timestamp

                // Get hour of day (0-23) for daily pattern
                val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

                // Calculate daily pattern: higher during morning, lower at night
                val dailyPattern = when (hourOfDay) {
                    in 6..10 -> 1.05 // Morning BP surge
                    in 11..18 -> 1.0 // Midday normal
                    in 19..22 -> 0.98 // Evening slight dip
                    else -> 0.92 // Night dip
                }

                // Calculate bp with random variation
                var systolic = baseSystolic * dailyPattern
                var diastolic = baseDiastolic * dailyPattern

                // Add some random variation
                systolic += (random.nextDouble() * 10.0) - 5.0 // ±5 mmHg
                diastolic += (random.nextDouble() * 8.0) - 4.0 // ±4 mmHg

                // Ensure diastolic is always less than systolic by at least 30
                if (systolic - diastolic < 30) {
                    diastolic = systolic - 30.0
                }

                // Occasionally add anomalies
                if (random.nextDouble() < anomalyProbability) {
                    if (random.nextBoolean()) {
                        // High BP
                        systolic = 160.0 + (random.nextDouble() * 20.0)
                        diastolic = 95.0 + (random.nextDouble() * 15.0)
                    } else {
                        // Low BP
                        systolic = 90.0 - (random.nextDouble() * 10.0)
                        diastolic = 60.0 - (random.nextDouble() * 10.0)
                    }
                }

                // Calculate pulse (60-100 bpm)
                val pulse = 70 + (random.nextInt(30))

                // Create situation description
                val situation = when {
                    hourOfDay < 10 -> "Morning"
                    hourOfDay < 12 -> "Before Lunch"
                    hourOfDay < 15 -> "After Lunch"
                    hourOfDay < 18 -> "Afternoon"
                    hourOfDay < 22 -> "Evening"
                    else -> "Before Sleep"
                }

                // Create health data object for blood pressure
                val data = HealthData(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    metricType = HealthData.TYPE_BLOOD_PRESSURE,
                    primaryValue = systolic,
                    secondaryValue = diastolic,
                    timestamp = timestamp,
                    situation = situation,
                    notes = "Pulse: $pulse BPM",
                    source = HealthData.SOURCE_SIMULATOR,
                    createdAt = System.currentTimeMillis()
                )

                bpData.add(data)
                timestamp += intervalMillis
            }

            return bpData
        }

        /**
         * Generate blood sugar data with realistic patterns
         */
        private fun generateBloodSugarData(
            userId: String,
            profile: UserProfile,
            period: TimePeriod,
            includeAnomalies: Boolean
        ): List<HealthData> {
            val bloodSugarData = mutableListOf<HealthData>()
            val calendar = Calendar.getInstance()

            // Set timestamp to current time
            val endTime = calendar.timeInMillis

            // Determine start time based on period
            calendar.apply {
                when (period) {
                    TimePeriod.DAY -> add(Calendar.DAY_OF_YEAR, -1)
                    TimePeriod.WEEK -> add(Calendar.DAY_OF_YEAR, -7)
                    TimePeriod.MONTH -> add(Calendar.MONTH, -1)
                    TimePeriod.THREE_MONTHS -> add(Calendar.MONTH, -3)
                    TimePeriod.SIX_MONTHS -> add(Calendar.MONTH, -6)
                    TimePeriod.YEAR -> add(Calendar.YEAR, -1)
                }
            }
            val startTime = calendar.timeInMillis

            // Blood sugar readings typically taken before/after meals
            val intervalMillis = when(period) {
                TimePeriod.DAY -> 3 * 60 * 60 * 1000L // 3 hours (for meals)
                TimePeriod.WEEK -> 6 * 60 * 60 * 1000L // 6 hours
                TimePeriod.MONTH -> 12 * 60 * 60 * 1000L // 12 hours
                TimePeriod.THREE_MONTHS, TimePeriod.SIX_MONTHS -> 24 * 60 * 60 * 1000L // 24 hours
                TimePeriod.YEAR -> 2 * 24 * 60 * 60 * 1000L // 2 days
            }

            // Base blood sugar values depending on profile (mg/dL)
            val baseFastingValue = when(profile) {
                UserProfile.HEALTHY, UserProfile.ACTIVE -> 85.0
                UserProfile.SEDENTARY -> 90.0
                UserProfile.PRE_HYPERTENSIVE, UserProfile.HYPERTENSIVE -> 95.0
                UserProfile.PRE_DIABETIC -> 110.0
                UserProfile.DIABETIC -> 130.0
            }

            val postMealIncrease = when(profile) {
                UserProfile.HEALTHY, UserProfile.ACTIVE -> 40.0
                UserProfile.SEDENTARY -> 45.0
                UserProfile.PRE_HYPERTENSIVE, UserProfile.HYPERTENSIVE -> 50.0
                UserProfile.PRE_DIABETIC -> 60.0
                UserProfile.DIABETIC -> 90.0
            }

            // Generate data points from start to end time
            var timestamp = startTime
            val random = Random()
            val anomalyProbability = if (includeAnomalies) 0.05 else 0.0 // 5% chance of anomaly

            while (timestamp <= endTime) {
                calendar.timeInMillis = timestamp

                // Get hour of day (0-23) for meal patterns
                val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

                // Determine if this is a before or after meal reading
                val isBeforeMeal = hourOfDay in setOf(7, 11, 17) // Before breakfast, lunch, dinner
                val isAfterMeal = hourOfDay in setOf(9, 13, 19) // After breakfast, lunch, dinner

                var bloodSugar = when {
                    hourOfDay < 7 -> {
                        // Early morning/fasting
                        baseFastingValue + (random.nextDouble() * 5) - 2.5 // ±2.5
                    }
                    isBeforeMeal -> {
                        // Before meal - slightly higher than fasting
                        baseFastingValue + 5 + (random.nextDouble() * 10) - 5 // +5 ±5
                    }
                    isAfterMeal -> {
                        // After meal - peak blood sugar
                        baseFastingValue + postMealIncrease + (random.nextDouble() * 20) - 10 // +increase ±10
                    }
                    else -> {
                        // Between meals - returning to baseline
                        val hoursSinceLastMeal = when (hourOfDay) {
                            in 10..11 -> hourOfDay - 9
                            // Since breakfast
                            in 14..16 -> hourOfDay - 13
                            // Since lunch
                            in 20..23 -> hourOfDay - 19
                            // Since dinner
                            else -> 3
                            // Default 3 hours
                        }

                        // Gradually decrease back to baseline
                        val decreaseFactor = (3.0 - hoursSinceLastMeal.coerceAtMost(3)) / 3.0
                        baseFastingValue + (postMealIncrease * decreaseFactor) + (random.nextDouble() * 15) - 7.5 // ±7.5
                    }
                }

                // Occasionally add anomalies
                if (random.nextDouble() < anomalyProbability) {
                    bloodSugar = if (profile == UserProfile.DIABETIC || profile == UserProfile.PRE_DIABETIC) {
                        // High blood sugar for diabetics (200-300 mg/dL)
                        200.0 + (random.nextDouble() * 100.0)
                    } else if (random.nextBoolean()) {
                        // High blood sugar (160-200 mg/dL)
                        160.0 + (random.nextDouble() * 40.0)
                    } else {
                        // Low blood sugar (40-60 mg/dL)
                        40.0 + (random.nextDouble() * 20.0)
                    }
                }

                // Create situation description
                val situation = when {
                    hourOfDay < 7 -> "Fasting"
                    hourOfDay == 7 -> "Before Breakfast"
                    hourOfDay == 9 -> "After Breakfast"
                    hourOfDay == 11 -> "Before Lunch"
                    hourOfDay == 13 -> "After Lunch"
                    hourOfDay == 17 -> "Before Dinner"
                    hourOfDay == 19 -> "After Dinner"
                    hourOfDay >= 21 -> "Before Sleep"
                    else -> "Between Meals"
                }

                // Create health data object for blood sugar
                val data = HealthData(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    metricType = HealthData.TYPE_BLOOD_SUGAR,
                    primaryValue = bloodSugar.toInt().toDouble(),
                    timestamp = timestamp,
                    situation = situation,
                    source = HealthData.SOURCE_SIMULATOR,
                    createdAt = System.currentTimeMillis()
                )

                bloodSugarData.add(data)
                timestamp += intervalMillis
            }

            return bloodSugarData
        }

        /**
         * Generate steps data with realistic patterns
         */
        private fun generateStepsData(
            userId: String,
            profile: UserProfile,
            period: TimePeriod,
            includeAnomalies: Boolean
        ): List<HealthData> {
            val stepsData = mutableListOf<HealthData>()
            val calendar = Calendar.getInstance()

            // Steps are usually accumulated daily, so we'll generate one entry per day
            // and add hourly breakdowns for shorter periods

            // Set timestamp to end of current day
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endTime = calendar.timeInMillis

            // Determine start time based on period
            calendar.apply {
                when (period) {
                    TimePeriod.DAY -> {
                        // For a single day, we'll do hourly entries instead
                        add(Calendar.DAY_OF_YEAR, -1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    TimePeriod.WEEK -> add(Calendar.DAY_OF_YEAR, -7)
                    TimePeriod.MONTH -> add(Calendar.MONTH, -1)
                    TimePeriod.THREE_MONTHS -> add(Calendar.MONTH, -3)
                    TimePeriod.SIX_MONTHS -> add(Calendar.MONTH, -6)
                    TimePeriod.YEAR -> add(Calendar.YEAR, -1)
                }
            }
            val startTime = calendar.timeInMillis

            // Base daily steps targets depending on profile
            val baseStepsTarget = when(profile) {
                UserProfile.ACTIVE -> 12000
                UserProfile.HEALTHY -> 10000
                UserProfile.SEDENTARY -> 6000
                UserProfile.PRE_HYPERTENSIVE, UserProfile.PRE_DIABETIC -> 8000
                UserProfile.HYPERTENSIVE, UserProfile.DIABETIC -> 7000
            }

            // Consistency factor - how consistently the user reaches their target
            val consistencyFactor = when(profile) {
                UserProfile.ACTIVE -> 0.9 // Reaches 90% of target on average
                UserProfile.HEALTHY -> 0.8
                UserProfile.PRE_HYPERTENSIVE, UserProfile.PRE_DIABETIC -> 0.7
                UserProfile.SEDENTARY -> 0.6
                UserProfile.HYPERTENSIVE, UserProfile.DIABETIC -> 0.5
            }

            // Generate data points
            val random = Random()
            val anomalyProbability = if (includeAnomalies) 0.1 else 0.0 // 10% chance of anomaly for steps

            if (period == TimePeriod.DAY) {
                // For a single day, generate hourly steps
                var timestamp = startTime
                var cumulativeSteps = 0

                while (timestamp <= endTime) {
                    calendar.timeInMillis = timestamp
                    val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

                    // Hourly step distribution - different patterns during the day
                    val hourlyFactor = when(hourOfDay) {
                        in 0..5 -> 0.01 // Almost no steps during sleep
                        6 -> 0.03 // Waking up
                        7 -> 0.08 // Morning routine
                        8 -> 0.06 // Commute
                        9, 10, 11 -> 0.04 // Morning work
                        12 -> 0.07 // Lunch break
                        13, 14, 15, 16 -> 0.04 // Afternoon work
                        17 -> 0.08 // Commute home
                        18 -> 0.10 // Evening activity
                        19 -> 0.08 // Dinner time
                        20 -> 0.06 // Post dinner
                        21 -> 0.04 // Evening wind down
                        22 -> 0.02 // Getting ready for bed
                        23 -> 0.01 // Sleep
                        else -> 0.01
                    }

                    // Calculate hour's steps
                    val hourlyTarget = (baseStepsTarget * hourlyFactor).toInt()
                    val hourlySteps = (hourlyTarget * (0.7 + (random.nextDouble() * 0.6))).toInt() // 70-130% of target

                    cumulativeSteps += hourlySteps

                    // Create data entry
                    val data = HealthData(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        metricType = HealthData.TYPE_STEPS,
                        primaryValue = cumulativeSteps.toDouble(),
                        timestamp = timestamp,
                        situation = "Daily",
                        notes = "Steps by ${hourOfDay}:00 - Added $hourlySteps steps",
                        source = HealthData.SOURCE_SIMULATOR,
                        createdAt = System.currentTimeMillis()
                    )

                    stepsData.add(data)

                    // Move to next hour
                    calendar.add(Calendar.HOUR_OF_DAY, 1)
                    timestamp = calendar.timeInMillis
                }
            } else {
                // For longer periods, generate daily steps
                var timestamp = startTime

                while (timestamp <= endTime) {
                    calendar.timeInMillis = timestamp

                    // Day of week factor - weekdays vs weekends
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val dayOfWeekFactor = when (dayOfWeek) {
                        Calendar.SATURDAY, Calendar.SUNDAY -> 0.8 // Less steps on weekends
                        Calendar.MONDAY, Calendar.FRIDAY -> 0.95 // Slightly less on Monday/Friday
                        else -> 1.0 // Normal on Tue/Wed/Thu
                    }

                    // Calculate daily steps with randomness
                    var dailySteps = (baseStepsTarget * consistencyFactor * dayOfWeekFactor *
                            (0.8 + (random.nextDouble() * 0.4))).toInt() // 80-120% of adjusted target

                    // Add occasional anomalies
                    if (random.nextDouble() < anomalyProbability) {
                        dailySteps = if (random.nextBoolean()) {
                            // High step count day (150-200% of target)
                            (baseStepsTarget * (1.5 + (random.nextDouble() * 0.5))).toInt()
                        } else {
                            // Very low step count day (10-30% of target)
                            (baseStepsTarget * (0.1 + (random.nextDouble() * 0.2))).toInt()
                        }
                    }

                    // Create data entry
                    val data = HealthData(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        metricType = HealthData.TYPE_STEPS,
                        primaryValue = dailySteps.toDouble(),
                        timestamp = timestamp,
                        situation = "Daily",
                        source = HealthData.SOURCE_SIMULATOR,
                        createdAt = System.currentTimeMillis()
                    )

                    stepsData.add(data)

                    // Move to next day
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    timestamp = calendar.timeInMillis
                }
            }

            return stepsData
        }
    }
}