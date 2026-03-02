package com.driverai.b2c.data.obd

/**
 * OBD-II service mode command strings sent to the ELM327 adapter.
 *
 * Format: "<mode> [PID] [frameNumber]\r"
 * Spaces are stripped by AT S0, but kept here for clarity before sending.
 */
object ObdCommands {
    // --- Mode 03: Read stored Diagnostic Trouble Codes ---
    const val GET_DTCS = "03\r"

    // --- Mode 04: Clear DTCs and freeze frame ---
    const val CLEAR_DTCS = "04\r"

    // --- Mode 02: Freeze frame data (PID, frame 01) ---
    // Response format: "42 <PID> <byte1> [byte2]"
    const val FREEZE_RPM    = "02 0C 01\r"  // Engine RPM       → (A*256+B)/4
    const val FREEZE_SPEED  = "02 0D 01\r"  // Vehicle speed    → A km/h
    const val FREEZE_TEMP   = "02 05 01\r"  // Coolant temp     → A-40 °C
    const val FREEZE_LOAD   = "02 04 01\r"  // Engine load      → A*100/255 %

    // --- Mode 01: Live data PIDs (for future live scanning feature) ---
    const val LIVE_RPM      = "01 0C\r"
    const val LIVE_SPEED    = "01 0D\r"
    const val LIVE_TEMP     = "01 05\r"
    const val LIVE_LOAD     = "01 04\r"

    /** All freeze frame commands to run after DTC scan */
    val FREEZE_FRAME_COMMANDS = mapOf(
        "RPM"   to FREEZE_RPM,
        "SPEED" to FREEZE_SPEED,
        "TEMP"  to FREEZE_TEMP,
        "LOAD"  to FREEZE_LOAD
    )
}
