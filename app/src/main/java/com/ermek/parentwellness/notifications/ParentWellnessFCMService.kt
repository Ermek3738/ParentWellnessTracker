package com.ermek.parentwellness.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ermek.parentwellness.MainActivity
import com.ermek.parentwellness.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ParentWellnessFCMService : FirebaseMessagingService() {
    private val TAG = "FCMService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Title: ${notification.title}")
            Log.d(TAG, "Message Notification Body: ${notification.body}")

            // Determine notification channel based on data or default to general
            val channelId = when (remoteMessage.data["type"]) {
                "high_heart_rate", "low_heart_rate" -> CHANNEL_HEART_RATE
                "high_blood_pressure", "low_blood_pressure" -> CHANNEL_BLOOD_PRESSURE
                "high_blood_sugar", "low_blood_sugar" -> CHANNEL_BLOOD_SUGAR
                "emergency" -> CHANNEL_EMERGENCY
                else -> CHANNEL_GENERAL
            }

            // Display the notification
            sendNotification(
                notification.title ?: "Health Alert",
                notification.body ?: "You have a new health alert",
                channelId,
                remoteMessage.data
            )
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed FCM token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        // Save or update the FCM token in Firestore for the current user
        val repository = NotificationRepository()
        repository.updateFCMToken(token)
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        channelId: String,
        data: Map<String, String>
    ) {
        // Create an intent to open the main activity
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Pass any data that might be needed
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Use alert priority for emergency and health alerts
        val priority = if (channelId == CHANNEL_EMERGENCY)
            NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(priority)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channels for Android O and above
        // Since you're targeting SDK 33+, this check is unnecessary but kept for compatibility
        createNotificationChannels(notificationManager)

        // Use a unique ID for each notification
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannels(notificationManager: NotificationManager) {
        // Android O (API 26) and above always has notification channels
        // Since you're targeting SDK 33+, this check is unnecessary but kept for compatibility
        val channels = listOf(
            NotificationChannel(
                CHANNEL_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            NotificationChannel(
                CHANNEL_HEART_RATE,
                "Heart Rate Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ),
            NotificationChannel(
                CHANNEL_BLOOD_PRESSURE,
                "Blood Pressure Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ),
            NotificationChannel(
                CHANNEL_BLOOD_SUGAR,
                "Blood Sugar Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ),
            NotificationChannel(
                CHANNEL_EMERGENCY,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                enableLights(true)
                lightColor = android.graphics.Color.RED
            }
        )

        channels.forEach { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        // Notification channel IDs
        const val CHANNEL_GENERAL = "general_channel"
        const val CHANNEL_HEART_RATE = "heart_rate_channel"
        const val CHANNEL_BLOOD_PRESSURE = "blood_pressure_channel"
        const val CHANNEL_BLOOD_SUGAR = "blood_sugar_channel"
        const val CHANNEL_EMERGENCY = "emergency_channel"
    }
}