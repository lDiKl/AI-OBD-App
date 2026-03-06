package com.avyrox.drive.data.scan

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class ScanSessionWithOccurrences(
    @Embedded val session: ScanSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val occurrences: List<ErrorOccurrenceEntity>,
)

@Dao
interface ScanHistoryDao {

    @Transaction
    @Query("SELECT * FROM scan_sessions ORDER BY scannedAt DESC")
    fun observeAllSessions(): Flow<List<ScanSessionWithOccurrences>>

    @Transaction
    @Query("SELECT * FROM scan_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): ScanSessionWithOccurrences?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScanSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrences(occurrences: List<ErrorOccurrenceEntity>)

    @Transaction
    suspend fun insertSessionWithOccurrences(
        session: ScanSessionEntity,
        occurrences: List<ErrorOccurrenceEntity>,
    ) {
        insertSession(session)
        insertOccurrences(occurrences)
    }

    @Query("DELETE FROM scan_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)
}
