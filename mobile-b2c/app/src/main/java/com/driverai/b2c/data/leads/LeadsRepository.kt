package com.driverai.b2c.data.leads

import com.driverai.b2c.data.network.B2CLeadDto
import com.driverai.b2c.data.network.LeadCreatedDto
import com.driverai.b2c.data.network.SendLeadRequest
import com.driverai.b2c.data.network.ShopDto
import com.driverai.b2c.data.network.ShopsLeadsApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeadsRepository @Inject constructor(
    private val api: ShopsLeadsApiService,
) {
    suspend fun getNearbyShops(lat: Double, lng: Double): Result<List<ShopDto>> =
        runCatching { api.getNearbyShops(lat, lng) }

    suspend fun sendLead(
        shopId: String,
        sessionId: String?,
        dtcCodes: List<String>,
        vehicleInfo: Map<String, String>,
    ): Result<LeadCreatedDto> = runCatching {
        api.sendLead(SendLeadRequest(shopId, sessionId, dtcCodes, vehicleInfo))
    }

    suspend fun getMyLeads(): Result<List<B2CLeadDto>> =
        runCatching { api.getMyLeads() }
}
