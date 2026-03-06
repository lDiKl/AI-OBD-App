package com.avyrox.drive.data.leads

import com.avyrox.drive.data.network.B2CLeadDto
import com.avyrox.drive.data.network.LeadCreatedDto
import com.avyrox.drive.data.network.SendLeadRequest
import com.avyrox.drive.data.network.ShopDto
import com.avyrox.drive.data.network.ShopsLeadsApiService
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
