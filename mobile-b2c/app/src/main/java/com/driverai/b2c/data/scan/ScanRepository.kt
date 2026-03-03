package com.driverai.b2c.data.scan

import com.driverai.b2c.data.network.FreezeFrameRequest
import com.driverai.b2c.data.network.ScanAnalyzeRequest
import com.driverai.b2c.data.network.ScanAnalyzeResponse
import com.driverai.b2c.data.network.ScanApiService
import com.driverai.b2c.data.network.ScannedCodeRequest
import com.driverai.b2c.data.obd.models.FreezeFrameData
import com.driverai.b2c.data.obd.models.ObdScanResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(
    private val api: ScanApiService,
) {
    suspend fun analyzeOBDResult(
        obdResult: ObdScanResult,
        vehicleId: String,
        mileage: Int,
    ): Result<ScanAnalyzeResponse> {
        return try {
            val request = ScanAnalyzeRequest(
                vehicleId = vehicleId,
                mileage = mileage,
                codes = obdResult.dtcCodes.map { dtc ->
                    ScannedCodeRequest(
                        code = dtc.raw,
                        freezeFrame = obdResult.freezeFrame?.toRequest() ?: FreezeFrameRequest(),
                    )
                },
            )
            val response = api.analyzeScan(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun FreezeFrameData.toRequest() = FreezeFrameRequest(
        rpm = engineRpm?.toFloat(),
        coolantTempC = coolantTemp?.toFloat(),
        engineLoadPct = engineLoad?.toFloat(),
        vehicleSpeedKmh = vehicleSpeed?.toFloat(),
    )
}
