package com.ermek.parentwellness.notifications

import android.util.Log
import com.ermek.parentwellness.data.repository.AuthRepository
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val TAG = "NotificationRepository"
    private val firestore = Firebase.firestore
    private val authRepository = AuthRepository()

    // Update FCM token for the current user
    fun updateFCMToken(token: String) {
        val userId = authRepository.getCurrentUserId() ?: return

        firestore.collection("users").document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating FCM token", e)
            }
    }

    // Get all alerts for the current user
    suspend fun getAlerts(limit: Int = 50): Result<List<Alert>> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val alertsSnapshot = firestore.collection("users").document(userId)
                .collection("alerts")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val alerts = alertsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Alert::class.java)?.copy(id = doc.id)
            }

            Result.success(alerts)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching alerts", e)
            Result.failure(e)
        }
    }

    // Mark an alert as read
    suspend fun markAlertAsRead(alertId: String): Result<Boolean> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            firestore.collection("users").document(userId)
                .collection("alerts").document(alertId)
                .update("read", true)
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking alert as read", e)
            Result.failure(e)
        }
    }

    // Update notification preferences
    suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Boolean> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            firestore.collection("users").document(userId)
                .update("notificationPreferences", preferences)
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification preferences", e)
            Result.failure(e)
        }
    }

    // Get notification preferences
    suspend fun getNotificationPreferences(): Result<NotificationPreferences> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val userDoc = firestore.collection("users").document(userId)
                .get()
                .await()

            val preferences = userDoc.get("notificationPreferences") as? Map<*, *>
            if (preferences != null) {
                val notificationPreferences = NotificationPreferences(
                    heartRateAlerts = preferences["heartRateAlerts"] as? Boolean ?: true,
                    bloodPressureAlerts = preferences["bloodPressureAlerts"] as? Boolean ?: true,
                    bloodSugarAlerts = preferences["bloodSugarAlerts"] as? Boolean ?: true,
                    weeklyReports = preferences["weeklyReports"] as? Boolean ?: true,
                    caregiverAlerts = preferences["caregiverAlerts"] as? Boolean ?: true
                )
                Result.success(notificationPreferences)
            } else {
                // Return default preferences if none set
                Result.success(NotificationPreferences())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notification preferences", e)
            Result.failure(e)
        }
    }
}

// Data classes for notifications
data class Alert(
    val id: String = "",
    val userId: String = "",
    val type: String = "",
    val metricName: String = "",
    val value: String = "",
    val timestamp: Long = 0,
    val read: Boolean = false,
    val createdAt: Long = 0
)

data class NotificationPreferences(
    val heartRateAlerts: Boolean = true,
    val bloodPressureAlerts: Boolean = true,
    val bloodSugarAlerts: Boolean = true,
    val weeklyReports: Boolean = true,
    val caregiverAlerts: Boolean = true
)