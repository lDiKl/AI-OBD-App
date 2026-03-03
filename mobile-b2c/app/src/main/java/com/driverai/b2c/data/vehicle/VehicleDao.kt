package com.driverai.b2c.data.vehicle

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {

    @Query("SELECT * FROM vehicles ORDER BY make, model")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(vehicles: List<VehicleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vehicle: VehicleEntity)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vehicles")
    suspend fun deleteAll()

    @Query("SELECT * FROM vehicles WHERE is_synced = 0")
    suspend fun getUnsynced(): List<VehicleEntity>

    @Query("DELETE FROM vehicles WHERE is_synced = 1")
    suspend fun deleteSynced()
}
