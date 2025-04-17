package com.ermek.parentwellness.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ermek.parentwellness.data.repository.WatchRepository
import com.ermek.parentwellness.data.samsung.SamsungHealthConnectionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for syncing health data in the background
 */
class HealthSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "HealthSyncWorker"
        private val SYNC_TIMEOUT = TimeUnit.MINUTES.toMillis(3) // 3 minutes timeout
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Health data sync started")

        try {
            // Initialize repository
            val watchRepository = WatchRepository(applicationContext)
            print("Initializing watch repository")
            watchRepository.initialize()

            // Wait for connection to be established
            withTimeoutOrNull(SYNC_TIMEOUT) {
                var status = watchRepository.connectionStatus.first()
                while (status != SamsungHealthConnectionStatus.CONNECTED &&
                    status != SamsungHealthConnectionStatus.CONNECTION_FAILED &&
                    status != SamsungHealthConnectionStatus.PERMISSIONS_REQUIRED) {
                    // Short delay to avoid busy waiting
                    kotlinx.coroutines.delay(100)
                    status = watchRepository.connectionStatus.first()
                }

                // Check final connection status
                when (status) {
                    SamsungHealthConnectionStatus.CONNECTED -> {
                        // Connected, continue
                        true
                    }
                    SamsungHealthConnectionStatus.PERMISSIONS_REQUIRED -> {
                        // Need permissions, can't continue in background
                        Log.d(TAG, "Samsung Health permissions required, sync canceled")
                        false
                    }
                    else -> {
                        // Connection failed or other error
                        Log.d(TAG, "Samsung Health connection failed, sync canceled")
                        false
                    }
                }
            } ?: run {
                // Timeout occurred
                Log.d(TAG, "Timed out waiting for Samsung Health connection")
                return@withContext Result.retry()
            }

            // Refresh data
            val success = watchRepository.refreshWatchData(forceRefresh = true)

            // Cleanup resources
            watchRepository.cleanup()

            if (success) {
                Log.d(TAG, "Health data sync completed successfully")
                Result.success()
            } else {
                Log.d(TAG, "Health data sync failed, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during health data sync: ${e.message}")
            Result.failure()
        }
    }
}