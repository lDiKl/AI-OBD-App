package com.avyrox.drive.di

import android.content.Context
import androidx.room.Room
import com.avyrox.drive.data.db.AppDatabase
import com.avyrox.drive.data.scan.ScanHistoryDao
import com.avyrox.drive.data.vehicle.VehicleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "driverai.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideVehicleDao(db: AppDatabase): VehicleDao = db.vehicleDao()

    @Provides
    fun provideScanHistoryDao(db: AppDatabase): ScanHistoryDao = db.scanHistoryDao()
}
