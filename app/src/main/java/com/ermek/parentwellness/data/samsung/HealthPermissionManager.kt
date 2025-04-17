package com.ermek.parentwellness.data.samsung

import android.util.Log
import com.samsung.android.sdk.healthdata.HealthDataStore
import com.samsung.android.sdk.healthdata.HealthPermissionManager as SamsungHealthPermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.HashSet
import android.os.Handler
import android.os.Looper


class HealthPermissionManager(private val healthDataStore: HealthDataStore) {
    companion object {
        private const val TAG = "HealthPermissionManager"
    }


    private val permissionManager = SamsungHealthPermissionManager(healthDataStore)

    suspend fun checkPermissions(dataTypes: Set<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            val permissionKeys = HashSet<SamsungHealthPermissionManager.PermissionKey>()
            for (dataType in dataTypes) {
                permissionKeys.add(
                    SamsungHealthPermissionManager.PermissionKey(
                        dataType,
                        SamsungHealthPermissionManager.PermissionType.READ
                    )
                )
            }

            val permissionMap = permissionManager.isPermissionAcquired(permissionKeys)
            Log.d(TAG, "Permission check result: $permissionMap")

            // Check if all permissions are granted
            var allGranted = true
            for (key in permissionKeys) {
                val isGranted = permissionMap[key]
                Log.d(TAG, "Permission for ${key.dataType}: $isGranted")
                if (isGranted == false) {
                    allGranted = false
                    break
                }
            }

            Log.d(TAG, "All permissions granted: $allGranted")
            return@withContext allGranted
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions: ${e.message}", e)
            return@withContext false
        }
    }

    fun requestPermissions(dataTypes: Set<String>, callback: (Boolean) -> Unit) {
        // Always run permission requests on the main thread
        Handler(Looper.getMainLooper()).post {
            try {
                Log.d(TAG, "Requesting permissions for data types: $dataTypes")

                // Convert data types to permission keys
                val permissionKeys = HashSet<SamsungHealthPermissionManager.PermissionKey>()
                for (dataType in dataTypes) {
                    permissionKeys.add(
                        SamsungHealthPermissionManager.PermissionKey(
                            dataType,
                            SamsungHealthPermissionManager.PermissionType.READ
                        )
                    )
                }

                Log.d(TAG, "Created permission keys: $permissionKeys")

                // Request permissions with activity context (this is important!)
                val resultHolder = permissionManager.requestPermissions(permissionKeys, null)

                resultHolder.setResultListener { result ->
                    // Result is already on the main thread
                    try {
                        Log.d(TAG, "Permission result received")

                        val resultMap = result.resultMap
                        Log.d(TAG, "Permission result map: $resultMap")

                        val allGranted = resultMap.values.all { it == true }

                        if (allGranted) {
                            Log.d(TAG, "All permissions granted")
                            callback(true)
                        } else {
                            Log.d(TAG, "Some permissions denied")
                            callback(false)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing permission result: ${e.message}", e)
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting permissions: ${e.message}", e)
                callback(false)
            }
        }
    }

}