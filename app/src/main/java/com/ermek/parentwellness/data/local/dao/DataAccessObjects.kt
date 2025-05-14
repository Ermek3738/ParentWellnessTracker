package com.ermek.parentwellness.data.local.dao

import androidx.room.*
import com.ermek.parentwellness.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HeartRateDao {
    @Query("SELECT * FROM heart_rate_readings WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllHeartRateReadings(userId: String): Flow<List<HeartRateEntity>>

    @Query("SELECT * FROM heart_rate_readings WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getHeartRateReadingsByTimeRange(userId: String, startTime: Long, endTime: Long): Flow<List<HeartRateEntity>>

    @Query("SELECT * FROM heart_rate_readings WHERE syncStatus = :status")
    suspend fun getHeartRateReadingsBySyncStatus(status: SyncStatus): List<HeartRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: HeartRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<HeartRateEntity>)

    @Update
    suspend fun update(reading: HeartRateEntity)

    @Query("UPDATE heart_rate_readings SET syncStatus = :syncStatus, lastSyncAttempt = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: SyncStatus, timestamp: Long)

    @Delete
    suspend fun delete(reading: HeartRateEntity)

    @Query("DELETE FROM heart_rate_readings WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface BloodPressureDao {
    @Query("SELECT * FROM blood_pressure_readings WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllBloodPressureReadings(userId: String): Flow<List<BloodPressureEntity>>

    @Query("SELECT * FROM blood_pressure_readings WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getBloodPressureReadingsByTimeRange(userId: String, startTime: Long, endTime: Long): Flow<List<BloodPressureEntity>>

    @Query("SELECT * FROM blood_pressure_readings WHERE syncStatus = :status")
    suspend fun getBloodPressureReadingsBySyncStatus(status: SyncStatus): List<BloodPressureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: BloodPressureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<BloodPressureEntity>)

    @Update
    suspend fun update(reading: BloodPressureEntity)

    @Query("UPDATE blood_pressure_readings SET syncStatus = :syncStatus, lastSyncAttempt = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: SyncStatus, timestamp: Long)

    @Delete
    suspend fun delete(reading: BloodPressureEntity)

    @Query("DELETE FROM blood_pressure_readings WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface BloodSugarDao {
    @Query("SELECT * FROM blood_sugar_readings WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllBloodSugarReadings(userId: String): Flow<List<BloodSugarEntity>>

    @Query("SELECT * FROM blood_sugar_readings WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getBloodSugarReadingsByTimeRange(userId: String, startTime: Long, endTime: Long): Flow<List<BloodSugarEntity>>

    @Query("SELECT * FROM blood_sugar_readings WHERE syncStatus = :status")
    suspend fun getBloodSugarReadingsBySyncStatus(status: SyncStatus): List<BloodSugarEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: BloodSugarEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<BloodSugarEntity>)

    @Update
    suspend fun update(reading: BloodSugarEntity)

    @Query("UPDATE blood_sugar_readings SET syncStatus = :syncStatus, lastSyncAttempt = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: SyncStatus, timestamp: Long)

    @Delete
    suspend fun delete(reading: BloodSugarEntity)

    @Query("DELETE FROM blood_sugar_readings WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface StepsDao {
    @Query("SELECT * FROM steps_readings WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllStepsReadings(userId: String): Flow<List<StepsEntity>>

    @Query("SELECT * FROM steps_readings WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getStepsReadingsByTimeRange(userId: String, startTime: Long, endTime: Long): Flow<List<StepsEntity>>

    @Query("SELECT * FROM steps_readings WHERE syncStatus = :status")
    suspend fun getStepsReadingsBySyncStatus(status: SyncStatus): List<StepsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: StepsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<StepsEntity>)

    @Update
    suspend fun update(reading: StepsEntity)

    @Query("UPDATE steps_readings SET syncStatus = :syncStatus, lastSyncAttempt = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: SyncStatus, timestamp: Long)

    @Delete
    suspend fun delete(reading: StepsEntity)

    @Query("DELETE FROM steps_readings WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}