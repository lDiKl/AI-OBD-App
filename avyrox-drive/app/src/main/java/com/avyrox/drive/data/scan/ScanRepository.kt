package com.avyrox.drive.data.scan

import com.avyrox.drive.data.network.FreezeFrameRequest
import com.avyrox.drive.data.network.ScanAnalyzeRequest
import com.avyrox.drive.data.network.ScanAnalyzeResponse
import com.avyrox.drive.data.network.ScanApiService
import com.avyrox.drive.data.network.ScannedCodeRequest
import com.avyrox.drive.data.obd.models.FreezeFrameData
import com.avyrox.drive.data.obd.models.ObdScanResult
import com.avyrox.drive.data.vehicle.VehicleEntity
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(
    private val api: ScanApiService,
    private val dao: ScanHistoryDao,
) {
    private val gson = Gson()

    val sessions: Flow<List<ScanSessionWithOccurrences>> = dao.observeAllSessions()

    suspend fun analyzeOBDResult(
        obdResult: ObdScanResult,
        vehicle: VehicleEntity,
    ): Result<ScanAnalyzeResponse> {
        return try {
            val request = ScanAnalyzeRequest(
                vehicleId = vehicle.id,
                mileage = vehicle.mileage ?: 0,
                codes = obdResult.dtcCodes.map { dtc ->
                    ScannedCodeRequest(
                        code = dtc.raw,
                        freezeFrame = obdResult.freezeFrame?.toRequest() ?: FreezeFrameRequest(),
                    )
                },
            )
            val response = api.analyzeScan(request)
            saveToRoom(response, vehicle)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSession(sessionId: String) = dao.deleteSession(sessionId)

    private suspend fun saveToRoom(response: ScanAnalyzeResponse, vehicle: VehicleEntity) {
        val session = ScanSessionEntity(
            id = response.sessionId,
            vehicleId = vehicle.id,
            vehicleMake = vehicle.make,
            vehicleModel = vehicle.model,
            vehicleYear = vehicle.year,
            overallRisk = response.overallRisk,
            safeToDrive = response.safeToDrive,
            mileage = vehicle.mileage ?: 0,
            codeCount = response.codes.size,
        )
        val occurrences = response.codes.mapIndexed { i, code ->
            ErrorOccurrenceEntity(
                id = "${response.sessionId}_${code.code}",
                sessionId = response.sessionId,
                code = code.code,
                category = code.free.category,
                severity = code.free.severity,
                canDrive = code.free.canDrive,
                description = code.free.description,
                simpleExplanation = code.premium?.simpleExplanation,
                mainCausesJson = code.premium?.mainCauses?.let { gson.toJson(it) },
                causesProbabilityJson = code.premium?.causesProbability?.let { gson.toJson(it) },
                recommendedAction = code.premium?.recommendedAction,
            )
        }
        dao.insertSessionWithOccurrences(session, occurrences)
    }

    private fun FreezeFrameData.toRequest() = FreezeFrameRequest(
        rpm = engineRpm?.toFloat(),
        coolantTempC = coolantTemp?.toFloat(),
        engineLoadPct = engineLoad?.toFloat(),
        vehicleSpeedKmh = vehicleSpeed?.toFloat(),
    )
}
