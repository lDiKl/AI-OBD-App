package com.driverai.b2c.data.network

import retrofit2.http.Body
import retrofit2.http.POST

// ---- Request ----

data class FreezeFrameRequest(
    val rpm: Float? = null,
    val coolantTempC: Float? = null,
    val engineLoadPct: Float? = null,
    val vehicleSpeedKmh: Float? = null,
)

data class ScannedCodeRequest(
    val code: String,
    val freezeFrame: FreezeFrameRequest = FreezeFrameRequest(),
)

data class ScanAnalyzeRequest(
    val vehicleId: String,
    val mileage: Int,
    val codes: List<ScannedCodeRequest>,
)

// ---- Response ----

data class CodeResultFreeDto(
    val description: String,
    val category: String,
    val severity: String,    // low | medium | high | critical
    val canDrive: String,    // yes | yes_with_caution | yes_within_2_weeks | limited | no
)

data class CodeResultPremiumDto(
    val simpleExplanation: String,
    val mainCauses: List<String>,
    val causesProbability: List<Int>,
    val whatHappensIfIgnored: String,
    val recommendedAction: String,
)

data class CodeResultDto(
    val code: String,
    val free: CodeResultFreeDto,
    val premium: CodeResultPremiumDto?,
)

data class ScanAnalyzeResponse(
    val sessionId: String,
    val overallRisk: String,   // low | medium | high | critical
    val safeToDrive: Boolean,
    val codes: List<CodeResultDto>,
)

// ---- Retrofit interface ----

interface ScanApiService {

    @POST("api/v1/b2c/scan/analyze")
    suspend fun analyzeScan(@Body request: ScanAnalyzeRequest): ScanAnalyzeResponse
}
