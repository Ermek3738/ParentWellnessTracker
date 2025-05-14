package com.ermek.parentwellness.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heart_rate_readings")
data class HeartRateEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val heartRate: Int,
    val timestamp: Long,
    val isResting: Boolean,
    val accuracy: Int,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncAttempt: Long? = null
)

@Entity(tableName = "blood_pressure_readings")
data class BloodPressureEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int?,
    val timestamp: Long,
    val situation: String?,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncAttempt: Long? = null
)

@Entity(tableName = "blood_sugar_readings")
data class BloodSugarEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val value: Int,
    val timestamp: Long,
    val situation: String?,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncAttempt: Long? = null
)

@Entity(tableName = "steps_readings")
data class StepsEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val steps: Int,
    val timestamp: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncAttempt: Long? = null
)

enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}