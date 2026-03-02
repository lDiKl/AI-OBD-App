package com.driverai.b2c.data.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class VehicleDto(
    val id: String,
    val make: String,
    val model: String,
    val year: Int,
    val engineType: String?,
    val vin: String?,
    val mileage: Int?,
)

data class VehicleCreateRequest(
    val make: String,
    val model: String,
    val year: Int,
    val engineType: String?,
    val vin: String?,
    val mileage: Int?,
)

interface VehicleApiService {

    @GET("api/v1/b2c/vehicles")
    suspend fun getVehicles(): List<VehicleDto>

    @POST("api/v1/b2c/vehicles")
    suspend fun createVehicle(@Body request: VehicleCreateRequest): VehicleDto

    @DELETE("api/v1/b2c/vehicles/{id}")
    suspend fun deleteVehicle(@Path("id") id: String)
}
