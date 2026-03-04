package com.shopai.b2b.data.cases

import com.google.gson.Gson
import com.shopai.b2b.data.local.DiagnosticCaseDao
import com.shopai.b2b.data.local.DiagnosticCaseEntity
import com.shopai.b2b.data.network.DiagnosticAnalyzeRequest
import com.shopai.b2b.data.network.DiagnosticAnalyzeResponse
import com.shopai.b2b.data.network.FreezeFrameRequest
import com.shopai.b2b.data.network.ShopApiService
import com.shopai.b2b.data.obd.models.FreezeFrameData
import com.shopai.b2b.data.obd.models.ObdScanResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticCaseRepository @Inject constructor(
    private val api: ShopApiService,
    private val dao: DiagnosticCaseDao,
    private val gson: Gson,
) {

    fun observeCases(): Flow<List<DiagnosticCaseEntity>> = dao.observeAll()

    suspend fun getCase(localId: String): DiagnosticCaseEntity? = dao.getById(localId)

    /**
     * Offline-first: saves the case locally first (pendingSync=true),
     * then calls the API. On success marks as synced.
     */
    suspend fun analyzeAndCreateCase(
        scanResult: ObdScanResult,
        vehicleMake: String,
        vehicleModel: String,
        vehicleYear: String,
        vehiclePlate: String,
        symptoms: String,
    ): DiagnosticCaseEntity {
        val codes = scanResult.dtcCodes.map { it.raw }
        val vehicleInfo = mapOf(
            "make" to vehicleMake,
            "model" to vehicleModel,
            "year" to vehicleYear,
            "plate" to vehiclePlate,
        )

        // Save locally immediately
        val entity = DiagnosticCaseEntity(
            vehicleMake = vehicleMake,
            vehicleModel = vehicleModel,
            vehicleYear = vehicleYear,
            vehiclePlate = vehiclePlate,
            inputCodes = gson.toJson(codes),
            symptomsText = symptoms,
            pendingSync = true,
        )
        dao.insert(entity)

        // Try to sync with the API
        return try {
            val ff = scanResult.freezeFrame
            val response = api.analyzeAndCreateCase(
                DiagnosticAnalyzeRequest(
                    codes = codes,
                    vehicleInfo = vehicleInfo,
                    symptoms = symptoms,
                    freezeFrame = ff?.toRequest() ?: FreezeFrameRequest(),
                    saveAsCase = true,
                )
            )
            val synced = entity.copy(
                serverId = response.caseId,
                aiResult = gson.toJson(response),
                pendingSync = false,
            )
            dao.update(synced)
            synced
        } catch (e: Exception) {
            entity  // return local entity even if network fails
        }
    }

    /** Push locally-created cases that haven't reached the server yet. */
    suspend fun syncPending() {
        val pending = dao.getPending()
        for (entity in pending) {
            try {
                val codes: List<String> = gson.fromJson(entity.inputCodes, Array<String>::class.java).toList()
                val vehicleInfo = mapOf(
                    "make" to entity.vehicleMake,
                    "model" to entity.vehicleModel,
                    "year" to entity.vehicleYear,
                    "plate" to entity.vehiclePlate,
                )
                val response = api.analyzeAndCreateCase(
                    DiagnosticAnalyzeRequest(
                        codes = codes,
                        vehicleInfo = vehicleInfo,
                        symptoms = entity.symptomsText,
                        saveAsCase = true,
                    )
                )
                if (response.caseId != null) {
                    dao.markSynced(entity.localId, response.caseId)
                }
            } catch (_: Exception) {
                // leave pendingSync = true, will retry later
            }
        }
    }
}

private fun FreezeFrameData.toRequest() = FreezeFrameRequest(
    rpm = engineRpm?.toFloat(),
    coolantTempC = coolantTemp?.toFloat(),
    engineLoadPct = engineLoad?.toFloat(),
    vehicleSpeedKmh = vehicleSpeed?.toFloat(),
)
