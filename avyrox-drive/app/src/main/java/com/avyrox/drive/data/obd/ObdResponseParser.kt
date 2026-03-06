package com.avyrox.drive.data.obd

import com.avyrox.drive.data.obd.models.DtcCode
import com.avyrox.drive.data.obd.models.DtcType
import com.avyrox.drive.data.obd.models.FreezeFrameData

/**
 * Parses raw ELM327 hex responses into domain models.
 *
 * All spaces are already stripped (AT S0 is set during init).
 * Headers are hidden (AT H0).
 */
object ObdResponseParser {

    /**
     * Parse Mode 03 response into a list of DTC codes.
     *
     * Example raw response (spaces stripped): "43030000000000"
     *   43 = mode 03 response marker
     *   03 00 = DTC bytes → "P0300"
     *   00 00, 00 00, 00 00 = padding (no more codes)
     *
     * Note: ELM327 does NOT include a count byte after 43.
     * DTC pairs start immediately after the "43" prefix.
     */
    fun parseDtcCodes(raw: String): List<DtcCode> {
        val cleaned = raw.trim().uppercase().replace(" ", "")

        // Sanity check — must start with mode 03 response byte "43"
        if (!cleaned.startsWith("43") || cleaned.length < 4) return emptyList()

        // DTC pairs start immediately after the "43" header — no count byte
        val payload = cleaned.removePrefix("43")

        // Each DTC is 2 bytes = 4 hex chars
        val dtcHex = payload

        val codes = mutableListOf<DtcCode>()
        var i = 0
        while (i + 3 < dtcHex.length) {
            val b1 = dtcHex.substring(i, i + 2).toIntOrNull(16) ?: break
            val b2 = dtcHex.substring(i + 2, i + 4).toIntOrNull(16) ?: break
            i += 4

            // Skip padding bytes (0x00 0x00 means no code)
            if (b1 == 0 && b2 == 0) continue

            val codeStr = rawCodeToString(b1, b2)
            val type = detectType(codeStr[0])
            codes.add(DtcCode(raw = codeStr, type = type, description = lookupDescription(codeStr)))
        }
        return codes
    }

    /**
     * Parse a map of freeze frame responses into FreezeFrameData.
     *
     * Keys: "RPM", "SPEED", "TEMP", "LOAD"
     * Values: raw ELM327 response string (spaces stripped)
     *
     * Example response for RPM (PID 0C): "420C0FA0"
     *   42 = mode 02 response marker
     *   0C = PID
     *   0F A0 = data bytes → (0x0F * 256 + 0xA0) / 4 = 1000 RPM
     */
    fun parseFreezeFrame(responses: Map<String, String>): FreezeFrameData {
        return FreezeFrameData(
            engineRpm   = parseFreezePid(responses["RPM"],   pidOffset = 4, byteCount = 2) { bytes ->
                ((bytes[0] * 256) + bytes[1]) / 4
            },
            vehicleSpeed = parseFreezePid(responses["SPEED"], pidOffset = 4, byteCount = 1) { bytes ->
                bytes[0]
            },
            coolantTemp = parseFreezePid(responses["TEMP"],   pidOffset = 4, byteCount = 1) { bytes ->
                bytes[0] - 40
            },
            engineLoad  = parseFreezePid(responses["LOAD"],   pidOffset = 4, byteCount = 1) { bytes ->
                bytes[0] * 100 / 255
            }
        )
    }

    // --- Private helpers ---

    /**
     * Generic freeze frame PID parser.
     * @param raw        raw response string (spaces stripped)
     * @param pidOffset  number of hex chars to skip before data (mode byte + PID byte = 4 chars)
     * @param byteCount  how many data bytes to read
     * @param calculate  lambda: converts byte list to Int result
     */
    private fun parseFreezePid(
        raw: String?,
        pidOffset: Int,
        byteCount: Int,
        calculate: (List<Int>) -> Int
    ): Int? {
        if (raw == null) return null
        val cleaned = raw.trim().uppercase().replace(" ", "")
        if (cleaned == ElmProtocol.NO_DATA || ElmProtocol.ERROR_RESPONSES.any { cleaned.contains(it) }) return null

        val dataStart = pidOffset  // skip "42" (mode) + "XX" (PID) = 4 hex chars
        val dataEnd = dataStart + byteCount * 2
        if (cleaned.length < dataEnd) return null

        val bytes = (0 until byteCount).map { idx ->
            val start = dataStart + idx * 2
            cleaned.substring(start, start + 2).toIntOrNull(16) ?: return null
        }
        return calculate(bytes)
    }

    /**
     * Convert two raw OBD bytes to a DTC string like "P0420".
     *
     * First nibble of b1 encodes system:
     *   0 = P0 (Powertrain, generic)
     *   1 = P1 (Powertrain, manufacturer)
     *   2 = P2 (Powertrain, generic)
     *   3 = P3 (Powertrain, manufacturer)
     *   4 = C0 (Chassis, generic)   — note: bit 6 is "C" indicator
     *   8 = B0 (Body, generic)
     *   C = U0 (Network, generic)
     *
     * Encoding:
     *   bits 7-6 of b1 → system type
     *   bits 5-4 of b1 → first digit
     *   bits 3-0 of b1 → second digit
     *   bits 7-4 of b2 → third digit
     *   bits 3-0 of b2 → fourth digit
     */
    private fun rawCodeToString(b1: Int, b2: Int): String {
        val typeBits = (b1 and 0xC0) shr 6
        val prefix = when (typeBits) {
            0 -> 'P'
            1 -> 'C'
            2 -> 'B'
            3 -> 'U'
            else -> 'P'
        }
        val d1 = (b1 and 0x30) shr 4
        val d2 = b1 and 0x0F
        val d3 = (b2 and 0xF0) shr 4
        val d4 = b2 and 0x0F
        return "$prefix$d1$d2$d3$d4"
    }

    private fun detectType(prefix: Char): DtcType = when (prefix) {
        'P' -> DtcType.POWERTRAIN
        'B' -> DtcType.BODY
        'C' -> DtcType.CHASSIS
        'U' -> DtcType.NETWORK
        else -> DtcType.POWERTRAIN
    }

    /**
     * Very small local description map for the most common codes.
     * Full lookup will later be done via the backend AI analysis.
     */
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
        "P0128" to "Coolant Thermostat (Coolant Temperature Below Thermostat Regulating Temperature)",
        "P0401" to "Exhaust Gas Recirculation Flow Insufficient Detected",
        "P0404" to "Exhaust Gas Recirculation Circuit Range/Performance",
        "P0442" to "Evaporative Emission Control System Leak Detected (Small Leak)",
        "P0455" to "Evaporative Emission Control System Leak Detected (Large Leak)",
        "P0505" to "Idle Control System Malfunction",
        "P0340" to "Camshaft Position Sensor A Circuit (Bank 1 or Single Sensor)",
        "P0335" to "Crankshaft Position Sensor A Circuit",
        "P0017" to "Crankshaft Position - Camshaft Position Correlation (Bank 1, Sensor B)",
        "P0011" to "Camshaft Position A - Timing Over-Advanced or System Performance (Bank 1)",
        "P0013" to "Exhaust Camshaft Position Actuator Circuit/Open (Bank 1)",
        "P0101" to "Mass Air Flow Circuit Range/Performance",
        "P0113" to "Intake Air Temperature Circuit High Input",
        "P0117" to "Engine Coolant Temperature Circuit Low Input",
        "P0118" to "Engine Coolant Temperature Circuit High Input",
        "P0131" to "O2 Sensor Circuit Low Voltage (Bank 1, Sensor 1)",
        "P0141" to "O2 Sensor Heater Circuit (Bank 1, Sensor 2)",
        "P0217" to "Engine Overtemperature Condition",
        "P0562" to "System Voltage Low",
        "P0563" to "System Voltage High",
        "P0600" to "Serial Communication Link",
        "P0700" to "Transmission Control System Malfunction",
        "P0730" to "Incorrect Gear Ratio"
    )
}
