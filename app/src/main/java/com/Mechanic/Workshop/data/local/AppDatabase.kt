package com.Mechanic.Workshop.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.Mechanic.Workshop.data.local.dao.CoefficientDao
import com.Mechanic.Workshop.data.local.dao.OutboxDao
import com.Mechanic.Workshop.data.local.dao.ReportDao
import com.Mechanic.Workshop.data.local.dao.TaskDao
import com.Mechanic.Workshop.data.local.dao.UserDao
import com.Mechanic.Workshop.data.local.entity.CoefficientEntity
import com.Mechanic.Workshop.data.local.entity.OutboxEntity
import com.Mechanic.Workshop.data.local.entity.ReportEntity
import com.Mechanic.Workshop.data.local.entity.TaskEntity
import com.Mechanic.Workshop.data.local.entity.UserEntity

@Database(
    entities = [
        TaskEntity::class,
        ReportEntity::class,
        UserEntity::class,
        OutboxEntity::class,
        CoefficientEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun reportDao(): ReportDao
    abstract fun userDao(): UserDao
    abstract fun outboxDao(): OutboxDao
    abstract fun coefficientDao(): CoefficientDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mechanic_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}