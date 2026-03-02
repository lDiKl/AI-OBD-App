package com.driverai.b2c.data.vehicle

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,          // UUID from backend
    val make: String,
    val model: String,
    val year: Int,
    val engineType: String?,
    val vin: String?,
    val mileage: Int?,
    val syncedAt: Long = System.currentTimeMillis(),
)
