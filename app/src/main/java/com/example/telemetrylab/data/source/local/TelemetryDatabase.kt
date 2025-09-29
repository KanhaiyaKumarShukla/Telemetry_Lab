package com.example.telemetrylab.data.source.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.telemetrylab.data.source.local.dao.TelemetryDao
import com.example.telemetrylab.data.source.local.entity.TelemetryDataEntity
import com.example.telemetrylab.data.source.local.entity.TelemetryStateEntity


@Database(
    entities = [
        TelemetryDataEntity::class,
        TelemetryStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TelemetryDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao

    companion object {
        @Volatile
        private var INSTANCE: TelemetryDatabase? = null


        fun getInstance(context: Context): TelemetryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TelemetryDatabase::class.java,
                    "telemetry_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
