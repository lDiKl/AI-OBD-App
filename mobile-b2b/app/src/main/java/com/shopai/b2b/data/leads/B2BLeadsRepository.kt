package com.shopai.b2b.data.leads

import com.shopai.b2b.data.network.B2BLeadDto
import com.shopai.b2b.data.network.QuoteRequest
import com.shopai.b2b.data.network.ShopApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class B2BLeadsRepository @Inject constructor(
    private val api: ShopApiService,
) {
    suspend fun getLeads(): Result<List<B2BLeadDto>> =
        runCatching { api.getLeads() }

    suspend fun getLead(leadId: String): Result<B2BLeadDto> =
        runCatching { api.getLead(leadId) }

    suspend fun sendQuote(leadId: String, costMin: Float, costMax: Float, days: Int, notes: String?): Result<B2BLeadDto> =
        runCatching { api.sendQuote(leadId, QuoteRequest(costMin, costMax, days, notes)) }

    suspend fun closeLead(leadId: String): Result<B2BLeadDto> =
        runCatching { api.closeLead(leadId) }
}
