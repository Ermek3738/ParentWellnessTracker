package com.ermek.parentwellness.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.ermek.parentwellness.data.local.AppDatabase
import com.ermek.parentwellness.data.local.entities.SyncStatus
import com.ermek.parentwellness.util.NetworkUtil
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class DataSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    private val TAG = "DataSyncWorker"
    private val database = AppDatabase.getDatabase(context)
    private val firestore = Firebase.firestore

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting data sync")

        try {
            // Check if network is available
            if (!NetworkUtil.isNetworkAvailable(applicationContext)) {
                Log.d(TAG, "Network not available, skipping sync")
                return Result.retry()
            }

            // Sync heart rate readings
            syncHeartRateReadings()

            // Sync blood pressure readings
            syncBloodPressureReadings()

            // Sync blood sugar readings
            syncBloodSugarReadings()

            // Sync steps data
            syncStepsReadings()

            Log.d(TAG, "Data sync completed successfully")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during data sync: ${e.message}")
            return Result.retry()
        }
    }

    private suspend fun syncHeartRateReadings() {
        // Get heart rate readings that need syncing
        val pendingReadings = database.heartRateDao()
            .getHeartRateReadingsBySyncStatus(SyncStatus.PENDING)

        if (pendingReadings.isEmpty()) {
            Log.d(TAG, "No heart rate readings to sync")
            return
        }

        Log.d(TAG, "Syncing ${pendingReadings.size} heart rate readings")

        for (reading in pendingReadings) {
            try {
                // Mark as syncing
                database.heartRateDao().updateSyncStatus(
                    reading.id,
                    SyncStatus.SYNCING,
                    System.currentTimeMillis()
                )

                // Upload to Firestore
                val data = hashMapOf(
                    "userId" to reading.userId,
                    "heartRate" to reading.heartRate,
                    "timestamp" to reading.timestamp,
                    "isResting" to reading.isResting,
                    "accuracy" to reading.accuracy,
                    "type" to "heartRate"
                )

                firestore
                    .collection("users")
                    .document(reading.userId)
                    .collection("healthData")
                    .document(reading.id)
                    .set(data)
                    .await()

                // Mark as synced
                database.heartRateDao().updateSyncStatus(
                    reading.id,
                    SyncStatus.SYNCED,
                    System.currentTimeMillis()
                )

                Log.d(TAG, "Heart rate reading ${reading.id} synced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing heart rate reading ${reading.id}: ${e.message}")

                // Mark as failed
                database.heartRateDao().updateSyncStatus(
                    reading.id,
                    SyncStatus.FAILED,
                    System.currentTimeMillis()
                )
            }
        }
    }

    private suspend fun syncBloodPressureReadings() {
        // Get blood pressure readings that need syncing
        val pendingReadings = database.bloodPressureDao()
            .getBloodPressureReadingsBySyncStatus(SyncStatus.PENDING)

        if (pendingReadings.isEmpty()) {
            Log.d(TAG, "No blood pressure readings to sync")
            return
        }

        Log.d(TAG, "Syncing ${pendingReadings.size} blood pressure readings")

        for (reading in pendingReadings) {
            try {
                // Mark as syncing
                database.bloodPressureDao().updateSyncStatus(
                    reading.id,
                    SyncStatus.SYNCING,
                    System.currentTimeMillis()
                )

                // Upload to Firestore
                val data = hashMapOf(
                    "userId" to reading.userId,
                    "systolic" to reading.systolic,
                    "diastolic" to reading.diastolic,
                    "pulse" to reading.pulse,
                    "timestamp" to reading.timestamp,
                    "situation" to reading.situation,
                    "type" to "bloodPressure"
                )

                firestore
                    .collection("users")
                    .document(reading.userId)
                    .collection("healthData")
                    .document(reading.id)
                    .set(data)
                    .await()

                // Mark as synced
                database.bloodPressureDao().updateSyncStatus(
                    reading.id,
                    SyncStatus.SYNCED,
                    System.currentTimeMillis()
                )

                Log.d(TAG, "Blood pressure reading ${reading.id} synced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing blood pressure reading ${reading.id}: ${e.message}")

                // Mark as failed
                database.bloodPressureDao().updateSyncStatus(
                    reading.id,
                    SyncStatus.FAILED,
                    System.currentTimeMillis()
                )
            }
        }
    }

    private suspend fun syncBloodSugarReadings() {
        // Get blood sugar readings that need syncing
        val pendingReadings = database.bloodSugarDao()
            .getBloodSugarReadingsBySyncStatus(SyncStatus.PENDING)

        if (pendingReadings.isEmpty()) {
            Log.d(TAG, "No blood sugar readings to sync")
            return
        }

        Log.d(TAG, "Syncing ${pendingReadings.size} blood sugar readings")

        for (reading in pendingReadings) {
            try {
                // Mark as syncing
                database.bloodSugarDao().updateSyncStatus(
                    reading.id,
                    SyncStatus.SYNCING,
                    System.currentTimeMillis()
                )

                // Upload to Firestore
                val data = hashMapOf(
                    "userId" to reading.userId,
                    "value" to reading.value,
                    "timestamp" to reading.timestamp,
                    "situation" to reading.situation,
                    "type" to "bloodSugar"
                )

                firestore
                    .collection("users")
                    .document(reading.userId)
                    .collection("healthData")
                    .document(reading.id)
                    .set(data)
                    .await()

                // Mark as synced
                database.bloodSugarDao().updateSyncStatus(
                    reading.id,
                    SyncStatus.SYNCED,
                    System.currentTimeMillis()
                )

                Log.d(TAG, "Blood sugar reading ${reading.id} synced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing blood sugar reading ${reading.id}: ${e.message}")

                // Mark as failed
                database.bloodSugarDao().updateSyncStatus(
                    reading.id,
                    SyncStatus.FAILED,
                    System.currentTimeMillis()
                )
            }
        }
    }

    private suspend fun syncStepsReadings() {
        // Get steps readings that need syncing
        val pendingReadings = database.stepsDao()
            .getStepsReadingsBySyncStatus(SyncStatus.PENDING)

        if (pendingReadings.isEmpty()) {
            Log.d(TAG, "No steps readings to sync")
            return
        }

        Log.d(TAG, "Syncing ${pendingReadings.size} steps readings")

        for (reading in pendingReadings) {
            try {
                // Mark as syncing
                database.stepsDao().updateSyncStatus(
                    reading.id,
                    SyncStatus.SYNCING,
                    System.currentTimeMillis()
                )

                // Upload to Firestore
                val data = hashMapOf(
                    "userId" to reading.userId,
                    "steps" to reading.steps,
                    "timestamp" to reading.timestamp,
                    "type" to "steps"
                )

                firestore
                    .collection("users")
                    .document(reading.userId)
                    .collection("healthData")
                    .document(reading.id)
                    .set(data)
                    .await()

                // Mark as synced
                database.stepsDao().updateSyncStatus(
                    reading.id,
                    SyncStatus.SYNCED,
                    System.currentTimeMillis()
                )

                Log.d(TAG, "Steps reading ${reading.id} synced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing steps reading ${reading.id}: ${e.message}")

                // Mark as failed
                database.stepsDao().updateSyncStatus(
                    reading.id,
                    SyncStatus.FAILED,
                    System.currentTimeMillis()
                )
            }
        }
    }

    companion object {
        // Setup periodic work
        fun setupPeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
                15, TimeUnit.MINUTES,  // Run every 15 minutes
                5, TimeUnit.MINUTES    // Flex period
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "health_data_sync",
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )

            Log.d("DataSyncWorker", "Periodic sync scheduled")
        }

        // Schedule immediate sync
        fun requestImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<DataSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "immediate_health_data_sync",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )

            Log.d("DataSyncWorker", "Immediate sync requested")
        }
    }
}