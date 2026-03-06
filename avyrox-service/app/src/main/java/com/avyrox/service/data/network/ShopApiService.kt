package com.avyrox.service.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// ---- Shop ----

data class ShopSetupRequest(
    val shopName: String,
    val address: String = "",
    val phone: String = "",
    val email: String = "",
)

data class ShopDto(
    val id: String,
    val name: String,
    val address: String,
    val phone: String,
    val email: String,
    val subscriptionTier: String,
    val verified: Boolean,
)

// ---- Diagnostic case ----

data class CaseCreateRequest(
    val vehicleInfo: Map<String, String>,
    val inputCodes: List<String>,
    val symptomsText: String = "",
)

data class CaseDto(
    val id: String,
    val shopId: String,
    val vehicleInfo: Map<String, String>,
    val inputCodes: List<String>,
    val symptomsText: String,
    val aiResult: Map<String, Any> = emptyMap(),
    val clientReportText: String = "",
    val estimate: Map<String, Any> = emptyMap(),
    val status: String,
    val createdAt: String,
)

// ---- Diagnostic analyze ----

data class FreezeFrameRequest(
    val rpm: Float? = null,
    val coolantTempC: Float? = null,
    val engineLoadPct: Float? = null,
    val vehicleSpeedKmh: Float? = null,
)

data class DiagnosticAnalyzeRequest(
    val codes: List<String>,
    val vehicleInfo: Map<String, String>,
    val symptoms: String = "",
    val freezeFrame: FreezeFrameRequest = FreezeFrameRequest(),
    val saveAsCase: Boolean = true,
)

data class ProbableCause(
    val cause: String,
    val probability: Int,
    val explanation: String,
)

data class DiagnosticAnalyzeResponse(
    val caseId: String?,
    val probableCauses: List<ProbableCause>,
    val diagnosticSequence: List<String>,
    val estimatedLaborHours: Float,
    val partsLikelyNeeded: List<String>,
    val tsbReferences: List<String>,
    val urgency: String,
    val additionalNotes: String,
)

// ---- Leads ----

data class B2BQuoteDto(
    val costMin: Float,
    val costMax: Float,
    val estimatedDays: Int,
    val notes: String?,
)

data class B2BLeadDto(
    val leadId: String,
    val userEmail: String,
    val status: String,   // pending | quoted | closed
    val dtcCodes: List<String>,
    val vehicleInfo: Map<String, String>,
    val freezeFrame: Map<String, Any>?,
    val createdAt: String,
    val quote: B2BQuoteDto?,
)

data class QuoteRequest(
    val costMin: Float,
    val costMax: Float,
    val estimatedDays: Int,
    val notes: String? = null,
)

// ---- Subscription ----

data class CheckoutRequest(
    val tier: String,
    val successUrl: String = "shopai://payment/success",
    val cancelUrl: String = "shopai://payment/cancel",
)
data class CheckoutResponse(val checkoutUrl: String)
data class SubscriptionStatusResponse(val subscriptionTier: String)

// ---- Retrofit interface ----

interface ShopApiService {

    @POST("api/v1/b2b/shop/setup")
    suspend fun setupShop(@Body request: ShopSetupRequest): ShopDto

    @GET("api/v1/b2b/shop/profile")
    suspend fun getShopProfile(): ShopDto

    @PUT("api/v1/b2b/shop/profile")
    suspend fun updateShopProfile(@Body request: ShopSetupRequest): ShopDto

    @GET("api/v1/b2b/cases/")
    suspend fun listCases(): List<CaseDto>

    @GET("api/v1/b2b/cases/{caseId}")
    suspend fun getCase(@Path("caseId") caseId: String): CaseDto

    @POST("api/v1/b2b/cases/")
    suspend fun createCase(@Body request: CaseCreateRequest): CaseDto

    @POST("api/v1/b2b/diagnostic/analyze")
    suspend fun analyzeAndCreateCase(@Body request: DiagnosticAnalyzeRequest): DiagnosticAnalyzeResponse

    @GET("api/v1/b2b/leads")
    suspend fun getLeads(): List<B2BLeadDto>

    @GET("api/v1/b2b/leads/{leadId}")
    suspend fun getLead(@Path("leadId") leadId: String): B2BLeadDto

    @PUT("api/v1/b2b/leads/{leadId}/quote")
    suspend fun sendQuote(@Path("leadId") leadId: String, @Body request: QuoteRequest): B2BLeadDto

    @PUT("api/v1/b2b/leads/{leadId}/close")
    suspend fun closeLead(@Path("leadId") leadId: String): B2BLeadDto

    @POST("api/v1/b2b/subscription/checkout")
    suspend fun getCheckoutUrl(@Body request: CheckoutRequest): CheckoutResponse

    @GET("api/v1/b2b/subscription/status")
    suspend fun getSubscriptionStatus(): SubscriptionStatusResponse
}
