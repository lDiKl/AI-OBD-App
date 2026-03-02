package com.driverai.b2c.data.vehicle

import com.driverai.b2c.data.network.VehicleApiService
import com.driverai.b2c.data.network.VehicleCreateRequest
import com.driverai.b2c.data.network.VehicleDto
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

    /** Fetch from backend and sync into Room. */
    suspend fun sync(): Result<Unit> {
        return try {
            val dtos = api.getVehicles()
            dao.deleteAll()
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
            Result.failure(e)
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
