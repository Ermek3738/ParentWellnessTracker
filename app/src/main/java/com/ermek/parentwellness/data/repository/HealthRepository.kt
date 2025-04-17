package com.ermek.parentwellness.data.repository

import com.ermek.parentwellness.data.model.HealthData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class HealthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val healthCollection = firestore.collection("health_data")

    suspend fun addHealthData(healthData: HealthData): Result<String> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val documentData = when (healthData) {
                is HealthData.Steps -> createStepsDocument(healthData, currentUserId)
                is HealthData.HeartRate -> createHeartRateDocument(healthData, currentUserId)
                is HealthData.BloodPressure -> createBloodPressureDocument(healthData, currentUserId)
                is HealthData.BloodGlucose -> createBloodGlucoseDocument(healthData, currentUserId)
                else -> return Result.failure(Exception("Unsupported health data type"))
            }

            val documentRef = healthCollection.add(documentData).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createStepsDocument(
        steps: HealthData.Steps,
        userId: String
    ): Map<String, Any> = mapOf(
        "userId" to userId,
        "dataType" to "Steps",
        "count" to steps.count,
        "startTime" to steps.startTime,
        "endTime" to steps.endTime,
        "timestamp" to System.currentTimeMillis()
    )

    private fun createHeartRateDocument(
        heartRate: HealthData.HeartRate,
        userId: String
    ): Map<String, Any> = mapOf(
        "userId" to userId,
        "dataType" to "HeartRate",
        "average" to heartRate.average,
        "dataTimestamp" to heartRate.dataTimestamp,
        "readings" to heartRate.readings.map { reading ->
            mapOf(
                "value" to reading.value,
                "timestamp" to reading.timestamp
            )
        },
        "timestamp" to System.currentTimeMillis()
    )

    private fun createBloodPressureDocument(
        bloodPressure: HealthData.BloodPressure,
        userId: String
    ): Map<String, Any> = mapOf(
        "userId" to userId,
        "dataType" to "BloodPressure",
        "systolic" to bloodPressure.systolic,
        "diastolic" to bloodPressure.diastolic,
        "dataTimestamp" to bloodPressure.dataTimestamp,
        "readings" to bloodPressure.readings.map { reading ->
            mapOf(
                "systolic" to reading.systolic,
                "diastolic" to reading.diastolic,
                "timestamp" to reading.timestamp
            )
        },
        "timestamp" to System.currentTimeMillis()
    )

    private fun createBloodGlucoseDocument(
        bloodGlucose: HealthData.BloodGlucose,
        userId: String
    ): Map<String, Any> = mapOf(
        "userId" to userId,
        "dataType" to "BloodGlucose",
        "average" to bloodGlucose.average,
        "dataTimestamp" to bloodGlucose.dataTimestamp,
        "readings" to bloodGlucose.readings.map { reading ->
            mapOf(
                "glucose" to reading.glucose,
                "timestamp" to reading.timestamp,
                "mealTime" to reading.mealTime
            )
        },
        "timestamp" to System.currentTimeMillis()
    )

    suspend fun getLatestHealthData(): Result<List<HealthData>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val querySnapshot = healthCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()

            val healthDataList = querySnapshot.documents.mapNotNull { document ->
                val data = document.data
                val dataType = data?.get("dataType") as? String

                when (dataType) {
                    "Steps" -> parseStepsData(data)
                    "HeartRate" -> parseHeartRateData(data)
                    "BloodPressure" -> parseBloodPressureData(data)
                    "BloodGlucose" -> parseBloodGlucoseData(data)
                    else -> null
                }
            }

            Result.success(healthDataList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseStepsData(data: Map<String?, Any?>?): HealthData.Steps {
        return HealthData.Steps(
            count = data?.let { (it["count"] as? Number)?.toInt() } ?: 0,
            startTime = data?.let { (it["startTime"] as? Number)?.toLong() } ?: 0L,
            endTime = data?.let { (it["endTime"] as? Number)?.toLong() } ?: 0L
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseHeartRateData(data: Map<String, Any>): HealthData.HeartRate {
        return HealthData.HeartRate(
            average = (data["average"] as? Number)?.toInt() ?: 0,
            dataTimestamp = (data["dataTimestamp"] as? Number)?.toLong() ?: 0L,
            readings = (data["readings"] as? List<Map<String, Any>>)?.map { reading ->
                HealthData.HeartRateReading(
                    value = (reading["value"] as? Number)?.toInt() ?: 0,
                    timestamp = (reading["timestamp"] as? Number)?.toLong() ?: 0L
                )
            } ?: emptyList()
        )
    }
    @Suppress("UNCHECKED_CAST")
    private fun parseBloodPressureData(data: Map<String, Any>): HealthData.BloodPressure {
        return HealthData.BloodPressure(
            systolic = (data["systolic"] as? Number)?.toFloat() ?: 0f,
            diastolic = (data["diastolic"] as? Number)?.toFloat() ?: 0f,
            dataTimestamp = (data["dataTimestamp"] as? Number)?.toLong() ?: 0L,
            readings = (data["readings"] as? List<Map<String, Any>>)?.map { reading ->
                HealthData.BloodPressureReading(
                    systolic = (reading["systolic"] as? Number)?.toFloat() ?: 0f,
                    diastolic = (reading["diastolic"] as? Number)?.toFloat() ?: 0f,
                    timestamp = (reading["timestamp"] as? Number)?.toLong() ?: 0L
                )
            } ?: emptyList()
        )
    }
    @Suppress("UNCHECKED_CAST")
    private fun parseBloodGlucoseData(data: Map<String, Any>): HealthData.BloodGlucose {
        return HealthData.BloodGlucose(
            average = (data["average"] as? Number)?.toFloat() ?: 0f,
            dataTimestamp = (data["dataTimestamp"] as? Number)?.toLong() ?: 0L,
            readings = (data["readings"] as? List<Map<String, Any>>)?.map { reading ->
                HealthData.BloodGlucoseReading(
                    glucose = (reading["glucose"] as? Number)?.toFloat() ?: 0f,
                    timestamp = (reading["timestamp"] as? Number)?.toLong() ?: 0L,
                    mealTime = reading["mealTime"] as? String ?: ""
                )
            } ?: emptyList()
        )
    }

    suspend fun getLatestStepsData(): Result<HealthData.Steps?> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val querySnapshot = healthCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("dataType", "Steps")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val stepsData = querySnapshot.documents.firstOrNull()?.let { document ->
                parseStepsData(document.data)
            }

            Result.success(stepsData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}