package com.example.telemetrylab.di

import android.content.Context
import androidx.room.Room
import com.example.telemetrylab.data.source.local.TelemetryDatabase
import com.example.telemetrylab.data.source.local.TelemetryLocalDataSource
import com.example.telemetrylab.data.source.local.TelemetryLocalDataSourceImpl
import com.example.telemetrylab.domain.repository.TelemetryRepository
import com.example.telemetrylab.data.repository.TelemetryRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTelemetryRepository(
        impl: TelemetryRepositoryImpl
    ): TelemetryRepository

    @Binds
    @Singleton
    abstract fun bindTelemetryLocalDataSource(
        impl: TelemetryLocalDataSourceImpl
    ): TelemetryLocalDataSource

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(
            @ApplicationContext context: Context
        ): TelemetryDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                TelemetryDatabase::class.java,
                "telemetry.db"
            ).fallbackToDestructiveMigration()
             .build()
        }

        @Provides
        @Singleton
        fun provideTelemetryDao(database: TelemetryDatabase) = database.telemetryDao()
    }
}
