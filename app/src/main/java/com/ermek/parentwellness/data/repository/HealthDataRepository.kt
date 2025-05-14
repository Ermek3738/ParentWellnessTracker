package com.ermek.parentwellness.data.repository

import android.content.Context
import com.ermek.parentwellness.data.local.AppDatabase
import com.ermek.parentwellness.data.local.entities.*
import com.ermek.parentwellness.data.sync.DataSyncWorker
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.*

class HealthDataRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val firestore = Firebase.firestore
    private val authRepository = AuthRepository()

    // Heart Rate Methods
    suspend fun addHeartRateReading(heartRate: Int, isResting: Boolean = false, accuracy: Int = 2) {
        val userId = authRepository.getCurrentUserId() ?: return

        val reading = HeartRateEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            heartRate = heartRate,
            timestamp = System.currentTimeMillis(),
            isResting = isResting,
            accuracy = accuracy,
            syncStatus = SyncStatus.PENDING
        )

        // Save to local database
        database.heartRateDao().insert(reading)

        // Request sync
        DataSyncWorker.requestImmediateSync(context)
    }

    fun getHeartRateReadings(): Flow<List<HeartRateData>> {
        val userId = authRepository.getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())

        return database.heartRateDao().getAllHeartRateReadings(userId).map { readings ->
            readings.map { entity ->
                HeartRateData(
                    id = entity.id,
                    heartRate = entity.heartRate,
                    timestamp = entity.timestamp,
                    isResting = entity.isResting,
                    isSynced = entity.syncStatus == SyncStatus.SYNCED
                )
            }
        }
    }

    fun getHeartRateReadingsByTimeRange(startTime: Long, endTime: Long): Flow<List<HeartRateData>> {
        val userId = authRepository.getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())

        return database.heartRateDao().getHeartRateReadingsByTimeRange(userId, startTime, endTime).map { readings ->
            readings.map { entity ->
                HeartRateData(
                    id = entity.id,
                    heartRate = entity.heartRate,
                    timestamp = entity.timestamp,
                    isResting = entity.isResting,
                    isSynced = entity.syncStatus == SyncStatus.SYNCED
                )
            }
        }
    }

    // Blood Pressure Methods
    suspend fun addBloodPressureReading(
        systolic: Int,
        diastolic: Int,
        pulse: Int? = null,
        situation: String? = null
    ) {
        val userId = authRepository.getCurrentUserId() ?: return

        val reading = BloodPressureEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            systolic = systolic,
            diastolic = diastolic,
            pulse = pulse,
            timestamp = System.currentTimeMillis(),
            situation = situation,
            syncStatus = SyncStatus.PENDING
        )

        // Save to local database
        database.bloodPressureDao().insert(reading)

        // Request sync
        DataSyncWorker.requestImmediateSync(context)
    }

    fun getBloodPressureReadings(): Flow<List<BloodPressureData>> {
        val userId = authRepository.getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())

        return database.bloodPressureDao().getAllBloodPressureReadings(userId).map { readings ->
            readings.map { entity ->
                BloodPressureData(
                    id = entity.id,
                    systolic = entity.systolic,
                    diastolic = entity.diastolic,
                    pulse = entity.pulse,
                    timestamp = entity.timestamp,
                    situation = entity.situation,
                    isSynced = entity.syncStatus == SyncStatus.SYNCED
                )
            }
        }
    }

    fun getBloodPressureReadingsByTimeRange(startTime: Long, endTime: Long): Flow<List<BloodPressureData>> {
        val userId = authRepository.getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())

        return database.bloodPressureDao().getBloodPressureReadingsByTimeRange(userId, startTime, endTime).map { readings ->
            readings.map { entity ->
                BloodPressureData(
                    id = entity.id,
                    systolic = entity.systolic,
                    diastolic = entity.diastolic,
                    pulse = entity.pulse,
                    timestamp = entity.timestamp,
                    situation = entity.situation,
                    isSynced = entity.syncStatus == SyncStatus.SYNCED
                )
            }
        }
    }

    // Blood Sugar Methods
    suspend fun addBloodSugarReading(value: Int, situation: String? = null) {
        val userId = authRepository.getCurrentUserId() ?: return

        val reading = BloodSugarEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            value = value,
            timestamp = System.currentTimeMillis(),
            situation = situation,
            syncStatus = SyncStatus.PENDING
        )

        // Save to local database
        database.bloodSugarDao().insert(reading)

        // Request sync
        DataSyncWorker.requestImmediateSync(context)
    }

    fun getBloodSugarReadings(): Flow<List<BloodSugarData>> {
        val userId = authRepository.getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())

        return database.bloodSugarDao().getAllBloodSugarReadings(userId).map { readings ->
            readings.map { entity ->
                BloodSugarData(
                    id = entity.id,
                    value = entity.value,
                    timestamp = entity.timestamp,
                    situation = entity.situation,
                    isSynced = entity.syncStatus == SyncStatus.SYNCED
                )
            }
        }
    }

    fun getBloodSugarReadingsByTimeRange(startTime: Long, endTime: Long): Flow<List<BloodSugarData>> {
        val userId = authRepository.getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())

        return database.bloodSugarDao().getBloodSugarReadingsByTimeRange(userId, startTime, endTime).map { readings ->
            readings.map { entity ->
                BloodSugarData(
                    id = entity.id,
                    value = entity.value,
                    timestamp = entity.timestamp,
                    situation = entity.situation,
                    isSynced = entity.syncStatus == SyncStatus.SYNCED
                )
            }
        }
    }

    // Steps Methods
    suspend fun addStepsReading(steps: Int) {
        val userId = authRepository.getCurrentUserId() ?: return

        val reading = StepsEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            steps = steps,
            timestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )

        // Save to local database
        database.stepsDao().insert(reading)

        // Request sync
        DataSyncWorker.requestImmediateSync(context)
    }

    fun getStepsReadings(): Flow<List<StepsData>> {
        val userId = authRepository.getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())

        return database.stepsDao().getAllStepsReadings(userId).map { readings ->
            readings.map { entity ->
                StepsData(
                    id = entity.id,
                    steps = entity.steps,
                    timestamp = entity.timestamp,
                    isSynced = entity.syncStatus == SyncStatus.SYNCED
                )
            }
        }
    }

    fun getStepsReadingsByTimeRange(startTime: Long, endTime: Long): Flow<List<StepsData>> {
        val userId = authRepository.getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())

        return database.stepsDao().getStepsReadingsByTimeRange(userId, startTime, endTime).map { readings ->
            readings.map { entity ->
                StepsData(
                    id = entity.id,
                    steps = entity.steps,
                    timestamp = entity.timestamp,
                    isSynced = entity.syncStatus == SyncStatus.SYNCED
                )
            }
        }
    }

    // Fetch and store remote data
    suspend fun fetchAndSyncRemoteData() {
        val userId = authRepository.getCurrentUserId() ?: return

        try {
            // Fetch heart rate data
            val heartRateSnapshot = firestore.collection("users").document(userId)
                .collection("healthData")
                .whereEqualTo("type", "heartRate")
                .get()
                .await()

            val heartRateEntities = heartRateSnapshot.documents.mapNotNull { doc ->
                try {
                    HeartRateEntity(
                        id = doc.id,
                        userId = doc.getString("userId") ?: userId,
                        heartRate = doc.getLong("heartRate")?.toInt() ?: 0,
                        timestamp = doc.getLong("timestamp") ?: 0,
                        isResting = doc.getBoolean("isResting") == true,
                        accuracy = doc.getLong("accuracy")?.toInt() ?: 2,
                        syncStatus = SyncStatus.SYNCED
                    )
                } catch (_: Exception) {
                    null
                }
            }

            database.heartRateDao().insertAll(heartRateEntities)

            // Fetch blood pressure data
            val bloodPressureSnapshot = firestore.collection("users").document(userId)
                .collection("healthData")
                .whereEqualTo("type", "bloodPressure")
                .get()
                .await()

            val bloodPressureEntities = bloodPressureSnapshot.documents.mapNotNull { doc ->
                try {
                    BloodPressureEntity(
                        id = doc.id,
                        userId = doc.getString("userId") ?: userId,
                        systolic = doc.getLong("systolic")?.toInt() ?: 0,
                        diastolic = doc.getLong("diastolic")?.toInt() ?: 0,
                        pulse = doc.getLong("pulse")?.toInt(),
                        timestamp = doc.getLong("timestamp") ?: 0,
                        situation = doc.getString("situation"),
                        syncStatus = SyncStatus.SYNCED
                    )
                } catch (_: Exception) {
                    null
                }
            }

            database.bloodPressureDao().insertAll(bloodPressureEntities)

            // Fetch blood sugar data
            val bloodSugarSnapshot = firestore.collection("users").document(userId)
                .collection("healthData")
                .whereEqualTo("type", "bloodSugar")
                .get()
                .await()

            val bloodSugarEntities = bloodSugarSnapshot.documents.mapNotNull { doc ->
                try {
                    BloodSugarEntity(
                        id = doc.id,
                        userId = doc.getString("userId") ?: userId,
                        value = doc.getLong("value")?.toInt() ?: 0,
                        timestamp = doc.getLong("timestamp") ?: 0,
                        situation = doc.getString("situation"),
                        syncStatus = SyncStatus.SYNCED
                    )
                } catch (_: Exception) {
                    null
                }
            }

            database.bloodSugarDao().insertAll(bloodSugarEntities)

            // Fetch steps data
            val stepsSnapshot = firestore.collection("users").document(userId)
                .collection("healthData")
                .whereEqualTo("type", "steps")
                .get()
                .await()

            val stepsEntities = stepsSnapshot.documents.mapNotNull { doc ->
                try {
                    StepsEntity(
                        id = doc.id,
                        userId = doc.getString("userId") ?: userId,
                        steps = doc.getLong("steps")?.toInt() ?: 0,
                        timestamp = doc.getLong("timestamp") ?: 0,
                        syncStatus = SyncStatus.SYNCED
                    )
                } catch (_: Exception) {
                    null
                }
            }

            database.stepsDao().insertAll(stepsEntities)
        } catch (_: Exception) {
            // Handle any errors during fetch and sync
        }
    }

    // Initialize sync worker
    fun initializeSync() {
        DataSyncWorker.setupPeriodicSync(context)
    }
}

// Data classes for the UI
data class HeartRateData(
    val id: String,
    val heartRate: Int,
    val timestamp: Long,
    val isResting: Boolean = false,
    val isSynced: Boolean = false
)

data class BloodPressureData(
    val id: String,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int? = null,
    val timestamp: Long,
    val situation: String? = null,
    val isSynced: Boolean = false
)

data class BloodSugarData(
    val id: String,
    val value: Int,
    val timestamp: Long,
    val situation: String? = null,
    val isSynced: Boolean = false
)

data class StepsData(
    val id: String,
    val steps: Int,
    val timestamp: Long,
    val isSynced: Boolean = false
)