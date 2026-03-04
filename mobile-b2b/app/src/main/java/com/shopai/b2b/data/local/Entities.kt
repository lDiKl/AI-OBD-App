package com.shopai.b2b.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Local-only record of a diagnostic case.
 * pendingSync = true means it hasn't been sent to the server yet.
 */
@Entity(tableName = "diagnostic_cases")
data class DiagnosticCaseEntity(
    @PrimaryKey val localId: String = UUID.randomUUID().toString(),
    val serverId: String? = null,          // null until synced
    val vehicleMake: String = "",
    val vehicleModel: String = "",
    val vehicleYear: String = "",
    val vehiclePlate: String = "",
    val inputCodes: String = "",           // JSON: List<String>
    val symptomsText: String = "",
    val aiResult: String = "",             // JSON: DiagnosticAnalyzeResponse
    val status: String = "open",           // open | in_progress | closed
    val createdAt: Long = System.currentTimeMillis(),
    val pendingSync: Boolean = true,
)

/**
 * Cached shop profile fetched from the API.
 * There is always at most one row (id = "current").
 */
@Entity(tableName = "shop_profile")
data class ShopProfileEntity(
    @PrimaryKey val id: String = "current",
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val subscriptionTier: String = "basic",
    val verified: Boolean = false,
)
