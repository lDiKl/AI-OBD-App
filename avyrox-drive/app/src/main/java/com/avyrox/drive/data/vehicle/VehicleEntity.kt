package com.avyrox.drive.data.vehicle

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,          // UUID from backend (or local UUID if not yet synced)
    val make: String,
    val model: String,
    val year: Int,
    val engineType: String?,
    val vin: String?,
    val mileage: Int?,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = true,  // false = created offline, not yet on backend
    val syncedAt: Long = System.currentTimeMillis(),
)
