package com.driverai.b2c.data.scan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val vehicleMake: String,
    val vehicleModel: String,
    val vehicleYear: Int,
    val overallRisk: String,      // low | medium | high | critical
    val safeToDrive: Boolean,
    val mileage: Int,
    val codeCount: Int,
    val scannedAt: Long = System.currentTimeMillis(),
)
