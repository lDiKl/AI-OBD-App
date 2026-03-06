package com.avyrox.service.data.obd.models

enum class DtcType(val prefix: Char) {
    POWERTRAIN('P'),
    BODY('B'),
    CHASSIS('C'),
    NETWORK('U')
}

data class DtcCode(
    val raw: String,          // e.g. "P0420"
    val type: DtcType,
    val description: String   // from local lookup or "Unknown code"
)
