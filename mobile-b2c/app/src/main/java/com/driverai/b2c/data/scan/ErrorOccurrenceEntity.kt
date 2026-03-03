package com.driverai.b2c.data.scan

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "error_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = ScanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sessionId")],
)
data class ErrorOccurrenceEntity(
    @PrimaryKey val id: String,   // "{sessionId}_{code}"
    val sessionId: String,
    val code: String,
    val category: String,
    val severity: String,
    val canDrive: String,
    val description: String,
    // Premium AI fields (null for free tier)
    val simpleExplanation: String?,
    val mainCausesJson: String?,        // JSON array: ["cause1","cause2"]
    val causesProbabilityJson: String?, // JSON array: [40,35,25]
    val recommendedAction: String?,
)
