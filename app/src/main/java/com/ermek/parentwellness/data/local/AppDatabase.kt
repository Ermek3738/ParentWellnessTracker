package com.ermek.parentwellness.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ermek.parentwellness.data.local.dao.*
import com.ermek.parentwellness.data.local.entities.*

@Database(
    entities = [
        HeartRateEntity::class,
        BloodPressureEntity::class,
        BloodSugarEntity::class,
        StepsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun heartRateDao(): HeartRateDao
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun bloodSugarDao(): BloodSugarDao
    abstract fun stepsDao(): StepsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "parent_wellness_database"
                )
                    .fallbackToDestructiveMigration() // Updated with parameter
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

// Implement Converters class for Room type conversions
class Converters {
    @androidx.room.TypeConverter
    fun fromTimestamp(value: Long?): java.util.Date? {
        return value?.let { java.util.Date(it) }
    }

    @androidx.room.TypeConverter
    fun dateToTimestamp(date: java.util.Date?): Long? {
        return date?.time
    }

    @androidx.room.TypeConverter
    fun fromString(value: String?): List<String> {
        return value?.split(",") ?: emptyList()
    }

    @androidx.room.TypeConverter
    fun fromList(list: List<String>): String {
        return list.joinToString(",")
    }

    @androidx.room.TypeConverter
    fun syncStatusToString(status: SyncStatus): String {
        return status.name
    }

    @androidx.room.TypeConverter
    fun stringToSyncStatus(statusName: String): SyncStatus {
        return try {
            SyncStatus.valueOf(statusName)
        } catch (e: Exception) {
            SyncStatus.PENDING
        }
    }
}