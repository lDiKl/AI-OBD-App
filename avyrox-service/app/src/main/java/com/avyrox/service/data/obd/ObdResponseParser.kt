package com.avyrox.service.data.obd

import com.avyrox.service.data.obd.models.DtcCode
import com.avyrox.service.data.obd.models.DtcType
import com.avyrox.service.data.obd.models.FreezeFrameData

object ObdResponseParser {

    fun parseDtcCodes(raw: String): List<DtcCode> {
        val cleaned = raw.trim().uppercase().replace(" ", "")
        if (!cleaned.startsWith("43") || cleaned.length < 4) return emptyList()

        val payload = cleaned.removePrefix("43")
        val codes = mutableListOf<DtcCode>()
        var i = 0
        while (i + 3 < payload.length) {
            val b1 = payload.substring(i, i + 2).toIntOrNull(16) ?: break
            val b2 = payload.substring(i + 2, i + 4).toIntOrNull(16) ?: break
            i += 4
            if (b1 == 0 && b2 == 0) continue
            val codeStr = rawCodeToString(b1, b2)
            val type = detectType(codeStr[0])
            codes.add(DtcCode(raw = codeStr, type = type, description = lookupDescription(codeStr)))
        }
        return codes
    }

    fun parseFreezeFrame(responses: Map<String, String>): FreezeFrameData {
        return FreezeFrameData(
            engineRpm    = parseFreezePid(responses["RPM"],   4, 2) { b -> (b[0] * 256 + b[1]) / 4 },
            vehicleSpeed = parseFreezePid(responses["SPEED"], 4, 1) { b -> b[0] },
            coolantTemp  = parseFreezePid(responses["TEMP"],  4, 1) { b -> b[0] - 40 },
            engineLoad   = parseFreezePid(responses["LOAD"],  4, 1) { b -> b[0] * 100 / 255 }
        )
    }

    private fun parseFreezePid(raw: String?, pidOffset: Int, byteCount: Int, calculate: (List<Int>) -> Int): Int? {
        if (raw == null) return null
        val cleaned = raw.trim().uppercase().replace(" ", "")
        if (cleaned == ElmProtocol.NO_DATA || ElmProtocol.ERROR_RESPONSES.any { cleaned.contains(it) }) return null
        val dataEnd = pidOffset + byteCount * 2
        if (cleaned.length < dataEnd) return null
        val bytes = (0 until byteCount).map { idx ->
            cleaned.substring(pidOffset + idx * 2, pidOffset + idx * 2 + 2).toIntOrNull(16) ?: return null
        }
        return calculate(bytes)
    }

    private fun rawCodeToString(b1: Int, b2: Int): String {
        val typeBits = (b1 and 0xC0) shr 6
        val prefix = when (typeBits) { 0 -> 'P'; 1 -> 'C'; 2 -> 'B'; 3 -> 'U'; else -> 'P' }
        val d1 = (b1 and 0x30) shr 4
        val d2 = b1 and 0x0F
        val d3 = (b2 and 0xF0) shr 4
        val d4 = b2 and 0x0F
        return "$prefix$d1$d2$d3$d4"
    }

    private fun detectType(prefix: Char): DtcType = when (prefix) {
        'P' -> DtcType.POWERTRAIN; 'B' -> DtcType.BODY
        'C' -> DtcType.CHASSIS;    'U' -> DtcType.NETWORK
        else -> DtcType.POWERTRAIN
    }

    private fun lookupDescription(code: String): String = COMMON_CODES[code] ?: "Unknown code"

    private val COMMON_CODES = mapOf(
        "P0420" to "Catalyst System Efficiency Below Threshold (Bank 1)",
        "P0430" to "Catalyst System Efficiency Below Threshold (Bank 2)",
        "P0300" to "Random/Multiple Cylinder Misfire Detected",
        "P0301" to "Cylinder 1 Misfire Detected",
        "P0302" to "Cylinder 2 Misfire Detected",
        "P0303" to "Cylinder 3 Misfire Detected",
        "P0304" to "Cylinder 4 Misfire Detected",
        "P0171" to "System Too Lean (Bank 1)",
        "P0174" to "System Too Lean (Bank 2)",
        "P0172" to "System Too Rich (Bank 1)",
        "P0175" to "System Too Rich (Bank 2)",
        "P0128" to "Coolant Thermostat Below Thermostat Regulating Temperature",
        "P0401" to "Exhaust Gas Recirculation Flow Insufficient",
        "P0442" to "Evaporative Emission System Leak Detected (Small)",
        "P0455" to "Evaporative Emission System Leak Detected (Large)",
        "P0505" to "Idle Control System Malfunction",
        "P0340" to "Camshaft Position Sensor A Circuit",
        "P0335" to "Crankshaft Position Sensor A Circuit",
        "P0011" to "Camshaft Position Timing Over-Advanced (Bank 1)",
        "P0101" to "Mass Air Flow Circuit Range/Performance",
        "P0700" to "Transmission Control System Malfunction"
    )
}
