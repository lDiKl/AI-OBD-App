package com.avyrox.drive.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// ---- DTOs ----

data class ShopDto(
    val id: String,
    val name: String,
    val address: String,
    val phone: String,
    val rating: Float,
    val distanceKm: Float,
)

data class SendLeadRequest(
    val shopId: String,
    val scanSessionId: String?,
    val dtcCodes: List<String>,
    val vehicleInfo: Map<String, String>,
)

data class LeadCreatedDto(
    val leadId: String,
    val status: String,
)

data class B2CQuoteDto(
    val costMin: Float,
    val costMax: Float,
    val estimatedDays: Int,
    val notes: String?,
)

data class B2CLeadDto(
    val leadId: String,
    val shopName: String,
    val status: String,   // pending | quoted | closed
    val dtcCodes: List<String>,
    val createdAt: String,
    val quote: B2CQuoteDto?,
)

// ---- Retrofit interface ----

interface ShopsLeadsApiService {

    @GET("api/v1/b2c/services/nearby")
    suspend fun getNearbyShops(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius_km") radiusKm: Int = 50,
    ): List<ShopDto>

    @POST("api/v1/b2c/leads")
    suspend fun sendLead(@Body request: SendLeadRequest): LeadCreatedDto

    @GET("api/v1/b2c/leads")
    suspend fun getMyLeads(): List<B2CLeadDto>
}
