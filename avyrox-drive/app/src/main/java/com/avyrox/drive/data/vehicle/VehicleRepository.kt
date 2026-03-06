package com.avyrox.drive.data.vehicle

import com.avyrox.drive.data.network.VehicleApiService
import com.avyrox.drive.data.network.VehicleCreateRequest
import com.avyrox.drive.data.network.VehicleDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepository @Inject constructor(
    private val dao: VehicleDao,
    private val api: VehicleApiService,
) {
    /** Room as single source of truth — UI always observes local DB. */
    val vehicles: Flow<List<VehicleEntity>> = dao.observeAll()

    /** Fetch from backend and sync into Room.
     *  Also pushes any vehicles that were created offline (isSynced=false). */
    suspend fun sync(): Result<Unit> {
        return try {
            // 1. Push locally-created vehicles to backend
            val unsynced = dao.getUnsynced()
            for (local in unsynced) {
                runCatching {
                    val dto = api.createVehicle(
                        VehicleCreateRequest(local.make, local.model, local.year, local.engineType, local.vin, local.mileage)
                    )
                    dao.deleteById(local.id)
                    dao.upsert(dto.toEntity())
                }
            }
            // 2. Pull authoritative list from backend
            val dtos = api.getVehicles()
            dao.deleteSynced()
            dao.upsertAll(dtos.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addVehicle(
        make: String,
        model: String,
        year: Int,
        engineType: String?,
        vin: String?,
        mileage: Int?,
    ): Result<VehicleEntity> {
        return try {
            val dto = api.createVehicle(
                VehicleCreateRequest(make, model, year, engineType, vin, mileage)
            )
            val entity = dto.toEntity()
            dao.upsert(entity)
            Result.success(entity)
        } catch (e: Exception) {
            // Backend unavailable — save locally with a generated UUID.
            // isSynced=false ensures sync() will push it to backend when connectivity returns.
            val entity = VehicleEntity(
                id = java.util.UUID.randomUUID().toString(),
                make = make,
                model = model,
                year = year,
                engineType = engineType,
                vin = vin,
                mileage = mileage,
                isSynced = false,
            )
            dao.upsert(entity)
            Result.success(entity)
        }
    }

    suspend fun deleteVehicle(id: String): Result<Unit> {
        return try {
            api.deleteVehicle(id)
            dao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            // If backend fails, still remove locally to keep UI responsive
            dao.deleteById(id)
            Result.failure(e)
        }
    }

    private fun VehicleDto.toEntity() = VehicleEntity(
        id = id,
        make = make,
        model = model,
        year = year,
        engineType = engineType,
        vin = vin,
        mileage = mileage,
    )
}
