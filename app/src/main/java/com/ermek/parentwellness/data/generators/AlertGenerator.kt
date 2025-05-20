package com.ermek.parentwellness.data.generators

import android.util.Log
import com.ermek.parentwellness.data.model.HealthData
import com.ermek.parentwellness.notifications.Alert
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Generates alerts based on health data anomalies
 */
class AlertGenerator {
    private val tag = "AlertGenerator"
    private val firestore = Firebase.firestore

    /**
     * Generate alerts for abnormal health readings
     */
    suspend fun generateAlertsForHealthData(userId: String, healthData: List<HealthData>): List<Alert> {
        val alerts = mutableListOf<Alert>()

        healthData.forEach { data ->
            val alert = when (data.metricType) {
                HealthData.TYPE_HEART_RATE -> checkHeartRateAlert(userId, data)
                HealthData.TYPE_BLOOD_PRESSURE -> checkBloodPressureAlert(userId, data)
                HealthData.TYPE_BLOOD_SUGAR -> checkBloodSugarAlert(userId, data)
                else -> null
            }

            alert?.let {
                alerts.add(it)
                saveAlert(userId, it)
            }
        }

        return alerts
    }

    private fun checkHeartRateAlert(userId: String, data: HealthData): Alert? {
        val heartRate = data.primaryValue.toInt()

        return when {
            heartRate > 100 -> {
                Alert(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = "high_heart_rate",
                    metricName = "Heart Rate",
                    value = "$heartRate BPM",
                    timestamp = data.timestamp,
                    read = false,
                    createdAt = System.currentTimeMillis()
                )
            }
            heartRate < 50 -> {
                Alert(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = "low_heart_rate",
                    metricName = "Heart Rate",
                    value = "$heartRate BPM",
                    timestamp = data.timestamp,
                    read = false,
                    createdAt = System.currentTimeMillis()
                )
            }
            else -> null
        }
    }

    private fun checkBloodPressureAlert(userId: String, data: HealthData): Alert? {
        val systolic = data.primaryValue.toInt()
        val diastolic = data.secondaryValue?.toInt() ?: return null

        return when {
            systolic >= 140 || diastolic >= 90 -> {
                Alert(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = "high_blood_pressure",
                    metricName = "Blood Pressure",
                    value = "$systolic/$diastolic mmHg",
                    timestamp = data.timestamp,
                    read = false,
                    createdAt = System.currentTimeMillis()
                )
            }
            systolic <= 90 || diastolic <= 60 -> {
                Alert(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = "low_blood_pressure",
                    metricName = "Blood Pressure",
                    value = "$systolic/$diastolic mmHg",
                    timestamp = data.timestamp,
                    read = false,
                    createdAt = System.currentTimeMillis()
                )
            }
            else -> null
        }
    }

    private fun checkBloodSugarAlert(userId: String, data: HealthData): Alert? {
        val bloodSugar = data.primaryValue.toInt()
        val isFasting = data.situation.contains("Fasting", ignoreCase = true) ||
                data.situation.contains("Before", ignoreCase = true)

        return when {
            isFasting && bloodSugar >= 126 -> {
                Alert(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = "high_blood_sugar",
                    metricName = "Blood Sugar (Fasting)",
                    value = "$bloodSugar mg/dL",
                    timestamp = data.timestamp,
                    read = false,
                    createdAt = System.currentTimeMillis()
                )
            }
            !isFasting && bloodSugar >= 200 -> {
                Alert(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = "high_blood_sugar",
                    metricName = "Blood Sugar (Post-meal)",
                    value = "$bloodSugar mg/dL",
                    timestamp = data.timestamp,
                    read = false,
                    createdAt = System.currentTimeMillis()
                )
            }
            bloodSugar <= 70 -> {
                Alert(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = "low_blood_sugar",
                    metricName = "Blood Sugar",
                    value = "$bloodSugar mg/dL",
                    timestamp = data.timestamp,
                    read = false,
                    createdAt = System.currentTimeMillis()
                )
            }
            else -> null
        }
    }

    private suspend fun saveAlert(userId: String, alert: Alert) {
        try {
            firestore.collection("users")
                .document(userId)
                .collection("alerts")
                .document(alert.id)
                .set(alert)
                .await()

            Log.d(tag, "Alert saved: ${alert.type} - ${alert.value}")
        } catch (e: Exception) {
            Log.e(tag, "Error saving alert", e)
        }
    }
}