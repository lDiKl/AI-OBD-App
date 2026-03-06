package com.avyrox.service.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticCaseDao {

    @Query("SELECT * FROM diagnostic_cases ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DiagnosticCaseEntity>>

    @Query("SELECT * FROM diagnostic_cases WHERE localId = :id")
    suspend fun getById(id: String): DiagnosticCaseEntity?

    @Query("SELECT * FROM diagnostic_cases WHERE pendingSync = 1")
    suspend fun getPending(): List<DiagnosticCaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(case: DiagnosticCaseEntity)

    @Update
    suspend fun update(case: DiagnosticCaseEntity)

    @Query("UPDATE diagnostic_cases SET serverId = :serverId, pendingSync = 0 WHERE localId = :localId")
    suspend fun markSynced(localId: String, serverId: String)

    @Query("UPDATE diagnostic_cases SET status = :status WHERE localId = :localId")
    suspend fun updateStatus(localId: String, status: String)
}
