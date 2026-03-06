package com.avyrox.drive.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.avyrox.drive.data.scan.ErrorOccurrenceEntity
import com.avyrox.drive.data.scan.ScanHistoryDao
import com.avyrox.drive.data.scan.ScanSessionEntity
import com.avyrox.drive.data.vehicle.VehicleDao
import com.avyrox.drive.data.vehicle.VehicleEntity

@Database(
    entities = [
        VehicleEntity::class,
        ScanSessionEntity::class,
        ErrorOccurrenceEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun scanHistoryDao(): ScanHistoryDao
}
