package com.driverai.b2c.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.driverai.b2c.data.vehicle.VehicleDao
import com.driverai.b2c.data.vehicle.VehicleEntity

@Database(
    entities = [VehicleEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
}
