package com.driverai.b2c.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.driverai.b2c.data.scan.ErrorOccurrenceEntity
import com.driverai.b2c.data.scan.ScanHistoryDao
import com.driverai.b2c.data.scan.ScanSessionEntity
import com.driverai.b2c.data.vehicle.VehicleDao
import com.driverai.b2c.data.vehicle.VehicleEntity

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
