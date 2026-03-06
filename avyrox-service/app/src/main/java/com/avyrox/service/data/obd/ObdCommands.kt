package com.avyrox.service.data.obd

object ObdCommands {
    const val GET_DTCS  = "03\r"
    const val CLEAR_DTCS = "04\r"

    const val FREEZE_RPM   = "02 0C 01\r"
    const val FREEZE_SPEED = "02 0D 01\r"
    const val FREEZE_TEMP  = "02 05 01\r"
    const val FREEZE_LOAD  = "02 04 01\r"

    const val LIVE_RPM   = "01 0C\r"
    const val LIVE_SPEED = "01 0D\r"
    const val LIVE_TEMP  = "01 05\r"
    const val LIVE_LOAD  = "01 04\r"

    val FREEZE_FRAME_COMMANDS = mapOf(
        "RPM"   to FREEZE_RPM,
        "SPEED" to FREEZE_SPEED,
        "TEMP"  to FREEZE_TEMP,
        "LOAD"  to FREEZE_LOAD
    )
}
