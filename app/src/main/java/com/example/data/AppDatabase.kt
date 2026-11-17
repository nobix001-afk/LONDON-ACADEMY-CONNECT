package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.AppDao
import com.example.data.model.*

@Database(
    entities = [
        UserEntity::class,
        EmployeeEntity::class,
        StudentEntity::class,
        EmployeeAttendanceEntity::class,
        StudentAttendanceEntity::class,
        FeePaymentEntity::class,
        FeeInstallmentEntity::class,
        AppNotificationEntity::class,
        HolidayEntity::class,
        OfficeSettingsEntity::class,
        EmployeeCategoryEntity::class,
        AuditLogEntity::class,
        CourseEntity::class,
        BranchEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "london_academy_connect.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
