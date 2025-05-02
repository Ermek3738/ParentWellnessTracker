package com.ermek.parentwellness.data.repository

import android.util.Log
import com.ermek.parentwellness.data.model.HealthData
import com.ermek.parentwellness.ui.components.HealthDataEntry
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository class for handling health data operations
 */
class HealthRepository {
    private val tag = "HealthRepository"  // Fixed: Changed TAG to lowercase 'tag'

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val healthDataCollection = firestore.collection("health_data")

    // Removed unused Flow import

    /**
     * Save health data entry to Firestore
     */
    suspend fun saveHealthData(entry: HealthDataEntry): Result<HealthData> {
        try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            Log.d(tag, "Saving health data for user: $userId, metric: ${entry.metricType}")

            // Convert entry to HealthData model
            val healthData = HealthData.fromHealthDataEntry(userId, entry)

            // Generate a document ID
            val docId = UUID.randomUUID().toString()
            healthData.id = docId

            // Save to Firestore
            healthDataCollection.document(docId).set(healthData).await()

            Log.d(tag, "Health data saved successfully: $docId")
            return Result.success(healthData)
        } catch (e: Exception) {
            Log.e(tag, "Error saving health data", e)
            return Result.failure(e)
        }
    }

    /**
     * Get health data for a specific metric type and user
     */
    suspend fun getHealthDataByType(metricType: String, limit: Int = 50): Result<List<HealthData>> {
        try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            Log.d(tag, "Fetching $metricType data for user: $userId")

            val snapshot = healthDataCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("metricType", metricType)  // Change to "type" if field name mismatch in Firestore
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val healthDataList = snapshot.documents.mapNotNull { doc ->
                val data = doc.toObject(HealthData::class.java)
                data?.id = doc.id
                data
            }

            Log.d(tag, "Fetched ${healthDataList.size} $metricType records")
            return Result.success(healthDataList)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching $metricType data", e)
            return Result.failure(e)
        }
    }

    /**
     * Get health data for a specific date range
     */
    suspend fun getHealthDataByDateRange(
        metricType: String,
        startTime: Long,
        endTime: Long
    ): Result<List<HealthData>> {
        try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            Log.d(tag, "Fetching $metricType data for date range: $startTime - $endTime")

            val snapshot = healthDataCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("metricType", metricType)  // Change to "type" if field name mismatch in Firestore
                .whereGreaterThanOrEqualTo("timestamp", startTime)
                .whereLessThanOrEqualTo("timestamp", endTime)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val healthDataList = snapshot.documents.mapNotNull { doc ->
                val data = doc.toObject(HealthData::class.java)
                data?.id = doc.id
                data
            }

            Log.d(tag, "Fetched ${healthDataList.size} $metricType records for date range")
            return Result.success(healthDataList)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching $metricType data for date range", e)
            return Result.failure(e)
        }
    }

    /**
     * Delete a health data entry
     */
    suspend fun deleteHealthData(id: String): Result<Boolean> {
        try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            Log.d(tag, "Deleting health data: $id for user: $userId")

            // Get the document first to check ownership
            val doc = healthDataCollection.document(id).get().await()
            val healthData = doc.toObject(HealthData::class.java)

            if (healthData == null || healthData.userId != userId) {
                return Result.failure(Exception("Health data not found or access denied"))
            }

            // Delete the document
            healthDataCollection.document(id).delete().await()

            Log.d(tag, "Health data deleted successfully: $id")
            return Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting health data", e)
            return Result.failure(e)
        }
    }

    /**
     * Generate simulated health data (for testing)
     */
    suspend fun generateSimulatedData(metricType: String, count: Int = 10): Result<List<HealthData>> {
        try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            Log.d(tag, "Generating $count simulated $metricType records")

            val simulatedData = mutableListOf<HealthData>()
            val currentTime = System.currentTimeMillis()
            val dayInMillis = 24 * 60 * 60 * 1000L

            for (i in 0 until count) {
                val timestamp = currentTime - (i * dayInMillis / count)
                val healthData = when (metricType) {
                    HealthData.TYPE_HEART_RATE -> createSimulatedHeartRate(userId, timestamp)
                    HealthData.TYPE_BLOOD_PRESSURE -> createSimulatedBloodPressure(userId, timestamp)
                    HealthData.TYPE_BLOOD_SUGAR -> createSimulatedBloodSugar(userId, timestamp)
                    HealthData.TYPE_STEPS -> createSimulatedSteps(userId, timestamp)
                    else -> continue
                }

                // Generate a document ID
                val docId = UUID.randomUUID().toString()
                healthData.id = docId

                // Save to Firestore
                healthDataCollection.document(docId).set(healthData).await()
                simulatedData.add(healthData)
            }

            // Refresh data
            getHealthDataByType(metricType)

            Log.d(tag, "Generated ${simulatedData.size} simulated $metricType records")
            return Result.success(simulatedData)
        } catch (e: Exception) {
            Log.e(tag, "Error generating simulated data", e)
            return Result.failure(e)
        }
    }

    private fun createSimulatedHeartRate(userId: String, timestamp: Long): HealthData {
        val baseHeartRate = 72.0
        val variation = (Math.random() * 20) - 10 // -10 to +10
        val situations = listOf("Resting", "After Exercise", "Sitting", "Walking")

        return HealthData(
            userId = userId,
            metricType = HealthData.TYPE_HEART_RATE,
            primaryValue = baseHeartRate + variation,
            timestamp = timestamp,
            situation = situations.random(),
            notes = "Simulated data",
            source = HealthData.SOURCE_SIMULATOR,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createSimulatedBloodPressure(userId: String, timestamp: Long): HealthData {
        val baseSystolic = 120.0
        val baseDiastolic = 80.0
        val systolicVariation = (Math.random() * 30) - 15 // -15 to +15
        val diastolicVariation = (Math.random() * 20) - 10 // -10 to +10
        val situations = listOf("Resting", "After Exercise", "Sitting", "At Work")

        return HealthData(
            userId = userId,
            metricType = HealthData.TYPE_BLOOD_PRESSURE,
            primaryValue = baseSystolic + systolicVariation,
            secondaryValue = baseDiastolic + diastolicVariation,
            timestamp = timestamp,
            situation = situations.random(),
            notes = "Simulated data",
            source = HealthData.SOURCE_SIMULATOR,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createSimulatedBloodSugar(userId: String, timestamp: Long): HealthData {
        val baseBloodSugar = 95.0
        val variation = (Math.random() * 40) - 20 // -20 to +20
        val situations = listOf("Fasting", "Before Meal", "After Meal", "Before Sleep")

        return HealthData(
            userId = userId,
            metricType = HealthData.TYPE_BLOOD_SUGAR,
            primaryValue = baseBloodSugar + variation,
            timestamp = timestamp,
            situation = situations.random(),
            notes = "Simulated data",
            source = HealthData.SOURCE_SIMULATOR,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createSimulatedSteps(userId: String, timestamp: Long): HealthData {
        val baseSteps = 7500.0
        val variation = (Math.random() * 5000) - 2500 // -2500 to +2500

        return HealthData(
            userId = userId,
            metricType = HealthData.TYPE_STEPS,
            primaryValue = baseSteps + variation,
            timestamp = timestamp,
            situation = "Daily",
            notes = "Simulated data",
            source = HealthData.SOURCE_SIMULATOR,
            createdAt = System.currentTimeMillis()
        )
    }
}